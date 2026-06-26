import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 테스트 조건 설정 (VU 100명, 1분 동안 실행)
export const options = {
    vus: 500,
    duration: '60s',
};

export default function () {
    const BASE_URL = 'http://localhost:8080/concerts';
    const SIZE = 10; // 한 페이지당 조회할 개수

    // -----------------------------------------------------------------------------
    // [1페이지 조회] 최초 요청이므로 lastConcertId 파라미터 없음
    // -----------------------------------------------------------------------------
    let res1 = http.get(`${BASE_URL}?size=${SIZE}`);

    // 정상 응답(200)인지 체크
    check(res1, { '1페이지 조회 성공': (r) => r.status === 200 });

    // 응답 바디에서 다음 커서 ID 추출
    let body1 = JSON.parse(res1.body);
    let cursor1 = body1?.nextCursor;
    let hasNext1 = body1?.hasNext;

    // 실제 유저의 행동 패턴을 시뮬레이션하기 위해 잠시 대기 (0.5초 ~ 1초 사이 랜덤)
    sleep(Math.random() * 0.5 + 0.5);


    // -----------------------------------------------------------------------------
    // [2페이지 조회] 1페이지에서 받은 cursor1이 존재하고, 다음 페이지가 있을 때만 진입
    // -----------------------------------------------------------------------------
    if (hasNext1 && cursor1 !== null) {
        let res2 = http.get(`${BASE_URL}?lastConcertId=${cursor1}&size=${SIZE}`);

        check(res2, { '2페이지 조회 성공': (r) => r.status === 200 });

        let body2 = JSON.parse(res2.body);
        let cursor2 = body2?.nextCursor;
        let hasNext2 = body2?.hasNext;

        sleep(Math.random() * 0.5 + 0.5);


        // -----------------------------------------------------------------------------
        // [3페이지 조회] 2페이지에서 받은 cursor2가 존재하고, 다음 페이지가 있을 때만 진입
        // -----------------------------------------------------------------------------
        if (hasNext2 && cursor2 !== null) {
            let res3 = http.get(`${BASE_URL}?lastConcertId=${cursor2}&size=${SIZE}`);

            check(res3, { '3페이지 조회 성공': (r) => r.status === 200 });

            // 3페이지 조회 후 작업 종료 -> 다음 루프에서 다시 1페이지부터 시작
            sleep(Math.random() * 0.5 + 0.5);
        }
    }
}