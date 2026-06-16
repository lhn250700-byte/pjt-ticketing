import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 100,
  duration: '1m',
};

export default function () {
  const res = http.get('http://localhost:8080/concerts');

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}