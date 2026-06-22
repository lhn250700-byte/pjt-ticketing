import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
    stages: [
            { duration: '30s', target: 1000 }, // 1,000명 도달
            { duration: '1m',  target: 2000 }, // 2,000명으로 증가
            { duration: '1m',  target: 3000 }, // 3,000명 피크 타임!
            { duration: '30s', target: 0 },    // 부하 감소
        ],
    thresholds: {
        // 단일 좌석 경합이므로 대다수가 실패하는 것이 정상 비즈니스 로직입니다.
        // 따라서 실패율(http_req_failed) 임계값은 해제하거나 제외합니다.
        http_req_duration: ['p(95)<500'],
    },
};

const BASE_URL = 'http://localhost:8080';
const SCHEDULE_ID = 1;
const TARGET_SEAT_ID = 11; // 🎯 [핵심] 모든 유저가 저격할 타겟 좌석 번호 고정!
const TARGET_PRICE = 150000; // 1번 좌석의 VIP 가격 고정

export default function () {
    const vuId = __VU;
    const iterId = __ITER;

    // 실제 존재하는 1~1000 사이의 userId 분산
    const MAX_USER_ID = 1000;
    const userId = ((vuId * 31 + iterId) % MAX_USER_ID) + 1;

    // 🎯 [1단계] 대기열 진입
    const joinUrl = `${BASE_URL}/queue/join?scheduleId=${SCHEDULE_ID}&userId=${userId}`;
    const joinRes = http.post(joinUrl);

    const isJoinSuccess = check(joinRes, {
        '1단계: 대기열 진입 성공 (200)': (r) => r.status === 200,
    });

    if (!isJoinSuccess) {
        sleep(1);
        return;
    }

    const realQueueToken = joinRes.json('token');

    // 스케줄러가 Active 방으로 옮겨주도록 대기
    sleep(7);

    // 🎯 [2단계] 좌석 가선점 요청 (단일 좌석 집중 포화 💣)
    const resUrl = `${BASE_URL}/reservations?scheduleId=${SCHEDULE_ID}`;
    const resHeaders = {
        'Content-Type': 'application/json',
        'Queue-Token': realQueueToken,
        'User-Id': String(userId),
    };
    const resBody = JSON.stringify({
        userId: userId,
        scheduleId: SCHEDULE_ID,
        seatId: TARGET_SEAT_ID, // 👈 분산 수식 제거, 무조건 1번 좌석으로 고정!
        queueToken: realQueueToken,
    });

    const resHold = http.post(resUrl, resBody, { headers: resHeaders });

    // 💡 최초 승자 1명만 202 성공을 받고, 나머지는 실패해야 완벽한 동시성 방어입니다.
    const isHoldSuccess = check(resHold, {
        '2단계: 좌석 가선점 요청 완료': (r) => r.status === 202 || r.status === 400 || r.status === 409, // 실패 시 백엔드가 던지는 예외 상태코드에 맞게 적어주세요.
    });

    // 가선점 성공 여부 체크 (진짜 성공한 1명만 통과)
    const realSuccess = (resHold.status === 202);

    if (!realSuccess) {
        // 경쟁에서 밀린 가상 유저들은 3단계 결제로 가지 못하고 여기서 시나리오 아웃
        sleep(1);
        return;
    }

    // 결제 진입 전 대기
    sleep(randomIntBetween(3, 5));

    // 🎯 [3단계] 최종 결제 승인 (오직 선점 성공자 1명만 들어옴)
    const payUrl = `${BASE_URL}/payments`;
    const payHeaders = {
        'Content-Type': 'application/json',
    };
    const payBody = JSON.stringify({
        userId: userId,
        scheduleId: SCHEDULE_ID,
        seatId: TARGET_SEAT_ID,
        amount: TARGET_PRICE,
        method: 'CARD',
    });

    const payRes = http.post(payUrl, payBody, { headers: payHeaders });

    check(payRes, {
        '3단계: 최종 결제 승인 완료 (201)': (r) => r.status === 201,
    });

    sleep(1);
}