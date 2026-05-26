import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// > 커스텀 메트릭: API별 응답시간을 따로 추적해 캐싱 전후 비교 시 항목별 수치 확인
const errorRate      = new Rate('errors');
const feedTrend      = new Trend('feed_duration',      true);
const rankingTrend   = new Trend('ranking_duration',   true);
const statsTrend     = new Trend('stats_duration',     true);
const districtsTrend = new Trend('districts_duration', true);
const lightsTrend    = new Trend('lights_duration',    true);

const BASE_URL = 'https://dev.lightrip.cloud';

// > DB 실제 user_id 범위: 1~305 (305명)
// > VU 50개가 각자 다른 userId 사용 → 실제 트래픽 패턴에 근접
const USER_IDS = Array.from({ length: 305 }, (_, i) => i + 1);

// > 서울 기준 Bounding Box — DB 내 서울 범위 여권 5,907개 커버
const BBOX = {
    minLat: 37.4,
    maxLat: 37.7,
    minLng: 126.8,
    maxLng: 127.2,
};

// > 부하 테스트 옵션
// > stages: 30초 램프업 → 1분 유지 → 20초 램프다운, 총 VU 50명
// > thresholds: 캐싱 Before에서는 초과해도 무방 — After에서 통과가 목표
export const options = {
    scenarios: {
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },
                { duration: '1m',  target: 50 },
                { duration: '20s', target: 0  },
            ],
        },
    },
    thresholds: {
        errors:             ['rate<0.05'],
        http_req_duration:  ['p(95)<3000'],
        ranking_duration:   ['p(95)<500'],
        stats_duration:     ['p(95)<1000'],
        feed_duration:      ['p(95)<3000'],
        districts_duration: ['p(95)<1000'],
        lights_duration:    ['p(95)<1500'],
    },
};

// > setup(): 테스트 시작 전 1회 실행. USER_IDS 전체 accessToken 발급 후 공유
// > VU마다 다른 userId를 써야 DB 조회 패턴이 실제 트래픽에 가까워짐
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

// > 메인 시나리오: __ITER % 5로 API를 순환 호출해 고른 분산 유지 (각 20%)
// > sleep(1): 실제 앱 사용 패턴 모사
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

    sleep(1);
}

// > 전체 주간 랭킹 — 캐싱 효과 가장 큰 API. 모든 VU가 동일 결과 조회
// > 현재 최근 7일 likes 2,121개 → 랭킹 집계 쿼리 실제 동작
function testRanking(headers) {
    const res = http.get(`${BASE_URL}/api/v1/rankings/total`, { headers });
    rankingTrend.add(res.timings.duration);
    check(res, { 'ranking 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
}

// > 내 여권 통계 — COUNT 쿼리 3개 (여권/좋아요/스크랩 수)
// > 10,020개 여권 + 18,000개 likes + 9,000개 scrap 기준 집계
function testStats(headers) {
    const res = http.get(`${BASE_URL}/api/v1/passports/stats/me`, { headers });
    statsTrend.add(res.timings.duration);
    check(res, { 'stats 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
}

// > 피드 조회 — PostGIS 공간쿼리 + 인기순 정렬, 단일 요청에 DB 쿼리 5개
// > 서울 시청 기준 반경 5km, 서울 범위 여권 5,907개 대상 쿼리
function testFeed(headers) {
    const res = http.get(
        `${BASE_URL}/api/v1/passports/feed?latitude=37.5665&longitude=126.9780&radius=5&size=10`,
        { headers }
    );
    feedTrend.add(res.timings.duration);
    check(res, { 'feed 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
}

// > 내 기록 지역 목록 — 지역별 COUNT 집계 + 대표 이미지 조회
function testDistricts(headers) {
    const res = http.get(`${BASE_URL}/api/v1/passports/districts/me`, { headers });
    districtsTrend.add(res.timings.duration);
    check(res, { 'districts 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
}

// > 내 불빛 조회 — 서울 전체 Bounding Box 기준 PostGIS 공간 필터
// > 서울 범위(37.4~37.7, 126.8~127.2) 여권 5,907개 대상
function testLights(headers) {
    const url = `${BASE_URL}/api/v1/passports/lights/me`
        + `?minLat=${BBOX.minLat}&maxLat=${BBOX.maxLat}`
        + `&minLng=${BBOX.minLng}&maxLng=${BBOX.maxLng}`;
    const res = http.get(url, { headers });
    lightsTrend.add(res.timings.duration);
    check(res, { 'lights 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
}
