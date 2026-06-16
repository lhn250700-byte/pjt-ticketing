import http from 'k6/http';
import { check, sleep } from 'k6';

// 💡 부하 테스트 기본 환경 설정
export const options = {
    stages: [
        { duration: '10s', target: 100 },  // 1️⃣ 처음 10초 동안 가상 유저(VU)를 100명까지 끌어올림
        { duration: '30s', target: 1000 }, // 2️⃣ 다음 30초 동안 폭발적으로 1,000명까지 늘림 (피크 트래픽)
        { duration: '10s', target: 0 },    // 3️⃣ 마지막 10초 동안 서서히 부하를 줄이며 마무리
    ],
};

export default function () {
    let randomUserId = Math.floor(Math.random() * 1000) + 1;

    const scheduleId = 1; // 테스트할 공연 일정 ID

    // 🎯 대기열 진입(Join) API 주소
    const url = `http://localhost:8080/queue/join?scheduleId=${scheduleId}&userId=${randomUserId}`;

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 🚀 대포 발사! (POST 요청)
    const res = http.post(url, {}, params);

    // ✅ 응답 결과 검증 (200 OK가 잘 떨어지는지 확인)
    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    // 유저들이 요청을 보내는 간격 제어 (실제 사람처럼 0.1초~0.5초 사이 랜덤 대기)
//    sleep(Math.random() * 0.4 + 0.1);
}