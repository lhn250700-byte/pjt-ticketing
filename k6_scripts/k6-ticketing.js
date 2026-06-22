import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
    stages: [
        { duration: '20s', target: 50 },  // 50명까지 유입
        { duration: '40s', target: 200 }, // 최대 200명 부하
        { duration: '20s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<500'],
    },
};

const BASE_URL = 'http://localhost:8080';
const SCHEDULE_ID = 1;

export default function () {
    const vuId = __VU;
    const iterId = __ITER;

    // 실제 존재하는 1~1000 사이의 userId만 동적으로 매핑
    const MAX_USER_ID = 1000;
    const userId = ((vuId * 31 + iterId) % MAX_USER_ID) + 1;

    // 🎯 [1단계] 대기열 진입 (POST)
    const joinUrl = `${BASE_URL}/queue/join?scheduleId=${SCHEDULE_ID}&userId=${userId}`;
    const joinRes = http.post(joinUrl);

    const isJoinSuccess = check(joinRes, {
        '1단계: 대기열 진입 성공 (200)': (r) => r.status === 200,
    });

    // 1단계 실패 시 다음 단계 진행 안 함
    if (!isJoinSuccess) {
        sleep(1);
        return;
    }

    // 📝 [핵심 추가] 서버가 응답 바디로 내려준 진짜 UUID 토큰을 동적으로 추출합니다.
    // 💡 만약 백엔드 DTO의 토큰 필드명이 'queueToken'이면 'token' 대신 'queueToken'으로 적어주세요.
    const realQueueToken = joinRes.json('token');

    // [중요] 스케줄러가 대기열 유저를 Active 방으로 확실히 옮겨줄 시간을 줍니다.
    sleep(7);

    // 🎯 [2단계] 좌석 가선점 요청 (POST)
    const totalSeats = 20000;
    const seatId = (userId % totalSeats) + 1;

    const resUrl = `${BASE_URL}/reservations?scheduleId=${SCHEDULE_ID}`;
    const resHeaders = {
        'Content-Type': 'application/json',
        'Queue-Token': realQueueToken, // 👈 가짜 문자열 대신 서버가 발급한 '진짜 토큰' 주입
        'User-Id': String(userId),
    };
    const resBody = JSON.stringify({
        userId: userId,
        scheduleId: SCHEDULE_ID,
        seatId: seatId,
        queueToken: realQueueToken,    // 👈 바디에도 진짜 토큰 주입
    });

    const resHold = http.post(resUrl, resBody, { headers: resHeaders });

    const isHoldSuccess = check(resHold, {
        '2단계: 좌석 가선점 성공 (202)': (r) => r.status === 202,
    });

    // 선점 경쟁에서 밀렸거나 인터셉터에서 튕겼다면 종료
    if (!isHoldSuccess) {
        sleep(1);
        return;
    }

    sleep(randomIntBetween(3, 5));

    // 🎯 [3단계] 결제 및 최종 예약 확정 (POST)
    let amount = 100000;
    if (seatId <= 3000) {
        amount = 150000;
    } else if (seatId >= 3001 && seatId <= 15000) {
        amount = 120000;
    }

    const payUrl = `${BASE_URL}/payments`;
    const payHeaders = {
        'Content-Type': 'application/json',
    };
    const payBody = JSON.stringify({
        userId: userId,
        scheduleId: SCHEDULE_ID,
        seatId: seatId,
        amount: amount,
        method: 'CARD',
    });

    const payRes = http.post(payUrl, payBody, { headers: payHeaders });

    check(payRes, {
        '3단계: 최종 결제 승인 완료 (200)': (r) => r.status === 200,
    });

    sleep(1);
}