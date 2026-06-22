import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        join_queue: {
            executor: 'per-vu-iterations',
            vus: 10000,
            iterations: 1,
            maxDuration: '30s',
        },
    },
};

export default function () {
    const userId = __VU;
    const scheduleId = 2;

    const url =
        `http://localhost:8080/queue/join?scheduleId=${scheduleId}&userId=${userId}`;

    const res = http.post(url);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}