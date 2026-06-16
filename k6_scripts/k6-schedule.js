import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1000,
  duration: '1m',
};

export default function () {
  const rand = Math.random();

  let scheduleId;

  if (rand < 0.7) {
    // 70%
    scheduleId = 1;
  } else  {
    // 20%
    scheduleId = 2;
  }

  const url = `http://localhost:8080/schedules/${scheduleId}`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}