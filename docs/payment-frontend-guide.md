# 결제(토스페이먼츠) 프론트 연동 가이드 — React Native

> 대상: 프론트(RN) 파트
> 백엔드: 토스페이먼츠 일회성 결제(프리미엄 이용권) 연동 완료
> 핵심: **사용자가 "결제하기"를 누르면 우리 페이지가 아니라 토스 결제창이 바로 떠야 함**

---

## 0. 한 줄 요약

```
결제하기 탭
  → POST /orders 로 주문 생성
  → 그 값으로 checkout.html(UI 없음)을 WebView로 오픈 → 토스창 즉시 등장
  → 결제 끝나면 successUrl 을 WebView가 가로채서 POST /confirm 호출
  → GET /me/premium 으로 이용권 상태 갱신
```

백엔드 API는 **SDK 무관**입니다. 프론트가 웹이든 RN이든 동일하게 사용합니다.

---

## 1. API 명세 (3개)

모든 요청에 헤더 필요: `Authorization: Bearer <accessToken>`

### 1) 주문 생성 — `POST /api/v1/payments/orders`

토스 결제창 호출 **직전에** 호출. 가격은 백엔드가 결정(프론트가 금액 못 정함 → 위변조 차단).

**Request**
```json
{ "productType": "PREMIUM_1MONTH" }
```

`productType` — 1개월 / 3개월 / 12개월 중 택 1:

| 값 | 상품 | 금액 |
|---|---|---|
| `PREMIUM_1MONTH` | 프리미엄 1개월 | 4,900원 |
| `PREMIUM_1YEAR` | 프리미엄 1년(12개월) | 49,000원 |

**Response**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "주문이 생성되었습니다.",
  "data": {
    "orderId": "ec1d2666-1c90-48a1-ba73-ab03d08b3af7",
    "userId": 1,
    "amount": 4900,
    "orderName": "프리미엄 1개월"
  }
}
```

`data.orderId`, `data.amount`, `data.orderName` 을 토스 결제창에 그대로 넘깁니다. (`userId`는 누가 주문했는지 식별용으로 함께 반환)

### 2) 결제 승인 — `POST /api/v1/payments/confirm`

토스 결제창에서 결제가 끝난 뒤 호출해야 **결제가 확정**됩니다. (호출 안 하면 매입 취소)

**Request** — 토스 성공 리다이렉트 URL의 쿼리값 그대로
```json
{
  "paymentKey": "토스가 준 paymentKey",
  "orderId": "주문 생성 때 받은 orderId",
  "amount": 4900
}
```

**Response**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "결제가 완료되었습니다.",
  "data": {
    "orderId": "ec1d2666-1c90-48a1-ba73-ab03d08b3af7",
    "status": "COMPLETED",
    "amount": 4900,
    "method": "카드",
    "approvedAt": "2026-06-01T12:34:56+09:00"
  }
}
```

### 3) 내 프리미엄 상태 — `GET /api/v1/payments/me/premium`

현재 이용권 상태 조회. 결제 화면 진입/결제 완료 후 갱신용.

**Response**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {
    "premium": true,
    "expiresAt": "2026-07-01T12:34:56",
    "daysLeft": 30
  }
}
```

- `premium` : 현재 이용 중 여부
- `expiresAt` : 만료 시각 (결제 이력 없으면 `null`, 만료된 경우 과거 시각일 수 있음)
- `daysLeft` : 남은 일수 (프리미엄 아니면 `null`)
- 여러 번 구매 시 기간 **누적**됨 (남은 이용권 끝에 이어붙음)

---

## 2. RN 결제 흐름 (결제하기 → 토스창 바로)

토스 v1 결제창 SDK는 브라우저 JS라 RN 네이티브에서 직접 못 돎 → **`react-native-webview` 안에서** 띄웁니다.

```
[RN] 결제하기 탭 (어떤 상품인지 결정: PREMIUM_1MONTH 등)
  │
  ├─ 1. POST /api/v1/payments/orders  (Bearer 토큰)
  │       → { orderId, userId, amount, orderName }
  │
  ├─ 2. WebView 오픈 → checkout.html 로드 (UI 없음, onload에서 즉시 토스창 호출)
  │       → 사용자 눈엔 "토스창이 바로 뜸"
  │
  └─ 3. 결제 완료 → successUrl 이동 시도
          → WebView onShouldStartLoadWithRequest 가 가로챔
          → paymentKey/orderId/amount 추출, WebView 닫기
          → POST /api/v1/payments/confirm (Bearer 토큰)
          → 완료 UI + GET /me/premium 으로 상태 갱신
```

> 핵심: checkout.html 은 **화면이 없습니다**. onload에서 곧장 `requestPayment`만 호출하므로 사용자는 우리 페이지를 한순간도 못 보고 토스창만 봅니다.

---

## 3. WebView가 로드할 checkout.html

UI 없이 쿼리값으로 즉시 토스창을 호출합니다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <script src="https://js.tosspayments.com/v1/payment"></script>
</head>
<body>
<script>
  const p = new URLSearchParams(location.search);
  TossPayments(p.get('ck')).requestPayment('카드', {
    amount:       Number(p.get('amount')),
    orderId:      p.get('orderId'),
    orderName:    p.get('orderName'),
    customerName: p.get('customerName') || '사용자',
    successUrl:   p.get('successUrl'),
    failUrl:      p.get('failUrl'),
  });
</script>
</body>
</html>
```

**호스팅 위치**: 백엔드 static(`https://<백엔드>/checkout.html`) 또는 프론트 CDN 중 택1. (백엔드 호스팅 여부는 백엔드 파트와 협의)

RN에서 WebView 여는 예시 (**한글·공백 파라미터는 반드시 `encodeURIComponent`** — `URLSearchParams`가 처리):

```js
const qs = new URLSearchParams({
  ck: TOSS_CLIENT_KEY,            // 앱 설정값(test_ck_ / live_ck_) — 공개 키
  orderId: order.orderId,
  amount: String(order.amount),
  orderName: order.orderName,
  successUrl: 'https://lightrip.app/payment/success',
  failUrl:    'https://lightrip.app/payment/fail',
}).toString();

<WebView source={{ uri: `https://<백엔드 또는 CDN>/checkout.html?${qs}` }} ... />
```

---

## 4. 결과 가로채기 + confirm (RN 네이티브)

```js
onShouldStartLoadWithRequest={(req) => {
  const url = req.url;

  // (A) 결제 성공
  if (url.startsWith('https://lightrip.app/payment/success')) {
    const p = new URL(url).searchParams;
    confirmPayment(p.get('paymentKey'), p.get('orderId'), Number(p.get('amount')));
    closeWebView();
    return false; // 실제 로드 막음
  }
  // (B) 결제 실패/취소
  if (url.startsWith('https://lightrip.app/payment/fail')) {
    closeWebView();
    return false;
  }
  // (C) ⚠️ 외부 앱 호출(카카오페이/앱카드/토스앱): http(s) 아닌 스킴이면 Linking으로
  if (!url.startsWith('http')) {
    Linking.openURL(url).catch(() => {});
    return false;
  }
  return true;
}}
```

```js
async function confirmPayment(paymentKey, orderId, amount) {
  const res = await fetch(`${BASE_URL}/api/v1/payments/confirm`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ paymentKey, orderId, amount }),
  });
  const json = await res.json();
  if (!res.ok || !json.success) throw new Error(json.message);
  // 성공 → 완료 UI, GET /me/premium 재호출로 상태 갱신
}
```

---

## 5. ⚠️ 반드시 처리해야 할 것 — 앱 간 이동(app-to-app)

카카오페이·앱카드·계좌이체를 누르면 WebView가 카카오톡/카드사 앱/토스앱을
`intent://`, `kakaotalk://`, `supertoss://`, `itms-apps://` 같은 **비-http 스킴**으로 열려고 합니다.
WebView는 기본적으로 이걸 못 열어 **결제가 여기서 멈춥니다.** (RN+토스 1순위 함정)

→ 위 4번 `(C)` 처럼 `http(s)`가 아닌 스킴이면 `Linking.openURL`로 외부 앱을 열어주세요.
안드로이드 `intent://`는 별도 파싱(fallback URL 처리)이 필요할 수 있습니다.
(토스 공식 문서: "React Native에서 결제 연동하기" 참고)

---

## 6. 보안 체크리스트

- **`ck`(클라이언트 키)는 앱/HTML에 박아도 됨** — 공개 키. **`sk`(시크릿 키)는 절대 앱·HTML·WebView에 넣지 말 것** (백엔드 전용).
- **accessToken은 checkout.html(WebView)에 넘기지 마세요.** checkout.html은 공개 `ck` + 주문정보만 필요. 토큰이 필요한 `confirm`은 **네이티브에서** 호출 → 토큰이 WebView에 노출 안 됨.
- successUrl/failUrl은 토스가 형식 검증하므로 **반드시 `https://`**. RN에선 "도착 페이지"가 아니라 "가로챌 마커"라 실제 페이지가 없어도 됨.
- 금액(amount)이 URL로 노출돼도 안전 — 백엔드 confirm이 DB의 상품 금액과 대조 검증함.

---

## 7. 운영 배포 시 (라이브 전환)

- 프론트: 라이브 클라이언트 키 `live_ck_` 사용 / 백엔드: 라이브 시크릿 키 `live_sk_` (같은 상점)
- successUrl/failUrl, checkout.html 모두 `https://`
- 토스 개발자센터에 서비스 도메인 등록 필요
- 프론트↔백엔드 도메인이 다르면 백엔드 CORS 허용 origin 확인
