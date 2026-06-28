import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
    scenarios: {
        test: {
            executor: "shared-iterations",
            vus: 100,
            iterations: 100,
        }
    }
};

const BASE_URL = 'http://localhost:8080';
const SCHEDULE_ID = 1;
const TARGET_SEAT_ID = 18;
const TARGET_PRICE = 150000; // 🚨 최종 결제 검증에 사용될 원천 금액 데이터

// ==================== 상태코드 집계 ====================
const status200 = new Counter('status_200');
const status201 = new Counter('status_201');
const status202 = new Counter('status_202'); // 🚨 비동기 수락 처리 상태 코드 집계 중심
const status400 = new Counter('status_400');
const status401 = new Counter('status_401');
const status403 = new Counter('status_403');
const status404 = new Counter('status_404');
const status409 = new Counter('status_409');
const status500 = new Counter('status_500');
const status503 = new Counter('status_503');

const seatConflict = new Counter('seat_conflict');
const serverError = new Counter('server_error');

function recordStatus(res) {
    switch (res.status) {
        case 200: status200.add(1); break;
        case 201: status201.add(1); break;
        case 202: status202.add(1); break; // 202 수량 카운팅
        case 400: status400.add(1); break;
        case 401: status401.add(1); break;
        case 403: status403.add(1); break;
        case 404: status404.add(1); break;
        case 409:
            status409.add(1);
            seatConflict.add(1);
            break;
        case 500:
        case 503:
            status503.add(1);
            serverError.add(1);
            break;
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

    // 대기열 진입 후 진입 허가 인터벌 모사
    sleep(7);

    // 2단계: 좌석 예매 요청 및 결제 정보 일괄 송신 (기존 2~3단계 통합)
    const resUrl = `${BASE_URL}/reservations?scheduleId=${SCHEDULE_ID}`;

    const resHeaders = {
        'Content-Type': 'application/json',
        'Queue-Token': realQueueToken,
        'User-Id': String(userId),
    };

    // 🚨 중요: 최초 요청 시 결제 대상 금액(amount)을 실어서 발송
    const resBody = JSON.stringify({
        userId: userId,
        scheduleId: SCHEDULE_ID,
        seatId: TARGET_SEAT_ID,
        amount: TARGET_PRICE,
        method: 'CARD',
        queueToken: realQueueToken,
    });

    const resHold = http.post(resUrl, resBody, { headers: resHeaders });

    recordStatus(resHold);

    // 🚨 202 Accepted 응답 무결성 및 경합 실패(409) 방어 분기 검증
    check(resHold, {
        '2단계: 예매 비동기 접수 완료 (202)': (r) => r.status === 202,
        '2단계 방어: 이미 선점된 좌석 혹은 중복 요청 (409)': (r) => r.status === 409,
    });

    sleep(1);
}