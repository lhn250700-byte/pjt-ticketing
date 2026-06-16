import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1000,
  duration: '1m',
};

export default function () {
  const rand = Math.random();

  let concertId;

  if (rand < 0.7) {
    // 70%
    concertId = 1;
  } else if (rand < 0.9) {
    // 20%
    concertId = 2;
  } else {
    // 10%
    concertId = Math.floor(Math.random() * 98) + 3; // 3 ~ 100
  }

  const url = `http://localhost:8080/concerts/${concertId}`;

  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}