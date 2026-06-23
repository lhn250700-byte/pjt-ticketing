import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
stages: [
        { duration: '30s', target: 500 },  // 30초 동안 500명까지 점진적 증가
        { duration: '30s', target: 1000 }, // 다음 30초 동안 1,000명까지 증가
        { duration: '1m',  target: 1500 }, // 1분 동안 목표치인 1,500명 유지 (피크 타임 검증)
        { duration: '30s', target: 0 },    // 30초 동안 부하 감축 및 정리
    ],
    thresholds: {
        // 지연 시간 목표치를 p(95) < 1000ms(1초)로 설정하여 모니터링합니다.
        http_req_duration: ['p(95)<1000'],
    },
};

const BASE_URL = 'http://localhost:8080';
const SCHEDULE_ID = 1;
const TARGET_SEAT_ID = 14;
const TARGET_PRICE = 150000;

// ==================== 상태코드 집계 ====================

const status200 = new Counter('status_200');
const status201 = new Counter('status_201');
const status202 = new Counter('status_202');
const status400 = new Counter('status_400');
const status401 = new Counter('status_401');
const status403 = new Counter('status_403');
const status404 = new Counter('status_404');
const status409 = new Counter('status_409');
const status429 = new Counter('status_429');
const status500 = new Counter('status_500');
const status503 = new Counter('status_503');

const seatConflict = new Counter('seat_conflict');
const serverError = new Counter('server_error');

function recordStatus(res) {
    switch (res.status) {
        case 200:
            status200.add(1);
            break;
        case 201:
            status201.add(1);
            break;
        case 202:
            status202.add(1);
            break;
        case 400:
            status400.add(1);
            break;
        case 401:
            status401.add(1);
            break;
        case 403:
            status403.add(1);
            break;
        case 404:
            status404.add(1);
            break;
        case 409:
            status409.add(1);
            seatConflict.add(1);
            break;
        case 429:
            status429.add(1);
            break;
        case 500:
            status500.add(1);
            serverError.add(1);
            break;
        case 503:
            status503.add(1);
            serverError.add(1);
            break;
    }

    if (res.status >= 500) {
        serverError.add(1);
    }
}

// ======================================================

export default function () {
    const vuId = __VU;
    const iterId = __ITER;

    const MAX_USER_ID = 1000;
    const userId = ((vuId * 31 + iterId) % MAX_USER_ID) + 1;

    // 1단계: 대기열 진입
    const joinUrl = `${BASE_URL}/queue/join?scheduleId=${SCHEDULE_ID}&userId=${userId}`;
    const joinRes = http.post(joinUrl);

    recordStatus(joinRes);

    const isJoinSuccess = check(joinRes, {
        '1단계: 대기열 진입 성공 (200)': (r) => r.status === 200,
    });

    if (!isJoinSuccess) {
        sleep(1);
        return;
    }

    const realQueueToken = joinRes.json('token');

    sleep(7);

    // 2단계: 좌석 가선점
    const resUrl = `${BASE_URL}/reservations?scheduleId=${SCHEDULE_ID}`;
    const resHeaders = {
        'Content-Type': 'application/json',
        'Queue-Token': realQueueToken,
        'User-Id': String(userId),
    };

    const resBody = JSON.stringify({
        userId: userId,
        scheduleId: SCHEDULE_ID,
        seatId: TARGET_SEAT_ID,
        queueToken: realQueueToken,
    });

    const resHold = http.post(resUrl, resBody, { headers: resHeaders });

    recordStatus(resHold);

    const isHoldSuccess = check(resHold, {
        '2단계: 좌석 가선점 요청 완료':
            (r) => r.status === 202 || r.status === 400 || r.status === 409,
    });

    const realSuccess = (resHold.status === 202);

    if (!realSuccess) {
        sleep(1);
        return;
    }

    sleep(randomIntBetween(3, 5));

    // 3단계: 최종 결제
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

    recordStatus(payRes);

    check(payRes, {
        '3단계: 최종 결제 승인 완료 (201)': (r) => r.status === 201,
    });

    sleep(1);
}