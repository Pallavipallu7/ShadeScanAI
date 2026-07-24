import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 100,
  duration: '1m',
  thresholds: {
    http_req_failed: ['rate<0.05'],     // Under 5% failure rate
    http_req_duration: ['p(95)<1500'],   // 95th percentile latency < 1.5s
  },
};

export default function () {
  const baseUrl = __ENV.BACKEND_URL || 'http://127.0.0.1:5000';
  const targetUrl = `${baseUrl.replace(/\/+$/, '')}/api/v1/dashboard/stats`;

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'User-Agent': 'k6-load-tester/1.0',
    },
    timeout: '10s'
  };

  const res = http.get(targetUrl, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 1500ms': (r) => r.timings.duration < 1500,
    'payload non-empty': (r) => r.body && r.body.length > 0
  });

  sleep(0.05);
}
