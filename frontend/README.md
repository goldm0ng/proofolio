# proofolio frontend

Next.js App Router · TypeScript · Tailwind v4. 백엔드(`../backend`, 기본 `http://localhost:8080`)에 프록시로 붙는다.

```bash
cp .env.local.example .env.local   # BACKEND_URL, APP_API_TOKEN
npm install
npm run dev        # http://localhost:3000
npm run build      # 타입 체크 + 빌드
```

브라우저는 백엔드 토큰을 모른다. `src/app/api/backend/[...path]/route.ts`가 서버에서 `Authorization: Bearer ${APP_API_TOKEN}`을 붙여 `${BACKEND_URL}/api/v1/*`로 전달한다.

```
src/lib/types.ts     docs/api.md의 타입
src/lib/api.ts       타입이 붙은 API 클라이언트 (/api/backend/* 호출)
src/lib/labels.ts    enum 한글 라벨
src/components/ui.tsx 공용 컴포넌트 (Button, Input, Select, Chip, Card, Toast, ConfirmDialog ...)
src/app/*            페이지
```
