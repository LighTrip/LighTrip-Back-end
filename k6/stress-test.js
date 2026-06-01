import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// > 스트레스 테스트: sleep 없이 VU를 단계적으로 올려 시스템 한계점(임계선) 탐색
// > t3.small + RDS Free Tier 기준 HikariCP 기본 pool(10) 포화 → DB 병목 지점 확인
const errorRate      = new Rate('errors');
const feedTrend      = new Trend('feed_duration',      true);
const rankingTrend   = new Trend('ranking_duration',   true);
const statsTrend     = new Trend('stats_duration',     true);
const districtsTrend = new Trend('districts_duration', true);
const lightsTrend    = new Trend('lights_duration',    true);

const BASE_URL = 'https://dev.lightrip.cloud';

// > user_id 범위 1~305 — VU 수가 305를 초과해도 mod 연산으로 순환
const USER_IDS = Array.from({ length: 305 }, (_, i) => i + 1);

const BBOX = {
    minLat: 37.4,
    maxLat: 37.7,
    minLng: 126.8,
    maxLng: 127.2,
};

// > 스트레스 테스트 stages:
// >   25 → 50 → 75 → 100 → 0 으로 단계 상승 (Grafana Cloud k6 무료 플랜 최대 100 VU)
// >   sleep 없이 VU당 RPS 극대화 — 100 VU × 최대 RPS ≈ 부하 테스트 500 VU 수준 효과
// >   각 단계 1분 유지 — 어느 구간에서 에러율/응답시간이 급등하는지 확인
// > thresholds: abortOnFail=true 로 에러율 30% 초과 시 조기 종료 (서버 보호)
export const options = {
    scenarios: {
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 25  },   // 워밍업
                { duration: '1m',  target: 25  },   // 기준 확인 (부하 테스트 절반)
                { duration: '30s', target: 50  },
                { duration: '1m',  target: 50  },   // 부하 테스트 동일 VU, sleep 없음
                { duration: '30s', target: 75  },
                { duration: '1m',  target: 75  },
                { duration: '30s', target: 100 },
                { duration: '1m',  target: 100 },   // 최대 — 여기서 한계점 노출 예상
                { duration: '30s', target: 0   },   // 램프다운
            ],
            gracefulRampDown: '10s',
        },
    },
    thresholds: {
        // > 에러율 30% 초과 시 즉시 중단 — 서버가 이미 포화 상태, 더 올릴 필요 없음
        errors: [{ threshold: 'rate<0.30', abortOnFail: true }],
        // > 응답시간 threshold는 느슨하게 설정 — 한계점 도달 전에 중단되지 않도록
        http_req_duration:  ['p(95)<10000'],
        ranking_duration:   ['p(95)<5000'],
        stats_duration:     ['p(95)<5000'],
        feed_duration:      ['p(95)<10000'],
        districts_duration: ['p(95)<5000'],
        lights_duration:    ['p(95)<5000'],
    },
};

// > setup(): 부하 테스트와 동일하게 305명 전체 토큰 발급
export function setup() {
    const tokens = {};

    for (const userId of USER_IDS) {
        const res = http.get(`${BASE_URL}/dev/token/${userId}`);
        if (res.status !== 200) {
            console.error(`userId ${userId} 토큰 발급 실패 (status: ${res.status})`);
            continue;
        }
        tokens[userId] = res.json('accessToken');
    }

    const issued = Object.keys(tokens).length;
    console.log(`토큰 발급 완료: ${issued}/${USER_IDS.length}`);

    if (issued === 0) {
        throw new Error('토큰 발급 전체 실패 — dev 서버 상태 확인 필요');
    }

    return { tokens };
}

// > sleep 없음: VU가 응답 받는 즉시 다음 요청 — RPS 극대화해 DB 커넥션 풀 포화 유도
// > __ITER % 5 로 API 고른 분산 유지
export default function (data) {
    const userId = USER_IDS[(__VU - 1) % USER_IDS.length];
    const token  = data.tokens[userId];

    if (!token) {
        console.warn(`userId ${userId} 토큰 없음, 스킵`);
        return;
    }

    const headers = {
        Authorization:  `Bearer ${token}`,
        'Content-Type': 'application/json',
    };

    switch (__ITER % 5) {
        case 0: testRanking(headers);   break;
        case 1: testStats(headers);     break;
        case 2: testFeed(headers);      break;
        case 3: testDistricts(headers); break;
        case 4: testLights(headers);    break;
    }

    // > sleep 없음 — 의도적으로 제거. 부하 테스트(load-test.js)와의 핵심 차이점
}

function testRanking(headers) {
    const res = http.get(`${BASE_URL}/api/v1/rankings/total`, { headers });
    rankingTrend.add(res.timings.duration);
    check(res, { 'ranking 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
}

function testStats(headers) {
    const res = http.get(`${BASE_URL}/api/v1/passports/stats/me`, { headers });
    statsTrend.add(res.timings.duration);
    check(res, { 'stats 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
}

function testFeed(headers) {
    const res = http.get(
        `${BASE_URL}/api/v1/passports/feed?latitude=37.5665&longitude=126.9780&radius=5&size=10`,
        { headers }
    );
    feedTrend.add(res.timings.duration);
    check(res, { 'feed 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
}

function testDistricts(headers) {
    const res = http.get(`${BASE_URL}/api/v1/passports/districts/me`, { headers });
    districtsTrend.add(res.timings.duration);
    check(res, { 'districts 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
}

function testLights(headers) {
    const url = `${BASE_URL}/api/v1/lights/me`
        + `?minLat=${BBOX.minLat}&maxLat=${BBOX.maxLat}`
        + `&minLng=${BBOX.minLng}&maxLng=${BBOX.maxLng}`;
    const res = http.get(url, { headers });
    lightsTrend.add(res.timings.duration);
    check(res, { 'lights 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
}
