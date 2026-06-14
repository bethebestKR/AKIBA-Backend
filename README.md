# AKIBA Backend

> **오타쿠 굿즈 마켓플레이스 & 커뮤니티 플랫폼**의 백엔드 서버
> 중고거래 · 특전/한정판 · 경매 · 구해요 · 커뮤니티 · 실시간 채팅을 하나의 서버에서 제공합니다.

<p align="left">
  <img src="https://img.shields.io/badge/Java-21-007396?logo=java&logoColor=white">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white">
  <img src="https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white">
  <img src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white">
  <img src="https://img.shields.io/badge/AWS-EC2%20%7C%20S3-FF9900?logo=amazonaws&logoColor=white">
  <img src="https://img.shields.io/badge/GitHub%20Actions-CI%2FCD-2088FF?logo=githubactions&logoColor=white">
</p>

- **Dev API** : https://api.dev.akiba-shop.com
- **Swagger UI** : https://api.dev.akiba-shop.com/swagger-ui/index.html

---

## 📌 프로젝트 개요

AKIBA는 오타쿠 굿즈를 사고팔고, 정보를 나누는 마켓플레이스 + 커뮤니티 서비스입니다.
일반적인 중고거래에 더해 **특전/한정판 거래**, **실시간 경매**, **구해요(역경매)**, **정품 감정 투표** 등 오타쿠 굿즈 도메인에 특화된 기능을 제공합니다.

본 저장소는 그 백엔드 API 서버이며, **단일 서비스로 14개 도메인 / 11개 REST 컨트롤러 / 54개 JPA 엔티티 / 180여 개 클래스**로 구성되어 있습니다.

### 담당 역할

소프트웨어학과 졸업 후 **백엔드 개발 전담**으로 참여했으며, 인프라 담당자가 이탈한 이후 **서버 애플리케이션 개발과 AWS 인프라/배포 운영을 모두 담당**하고 있습니다. 도메인 설계부터 인증, 동시성 제어, 실시간 통신, CI/CD 파이프라인 구축, 운영 트러블슈팅까지 백엔드 전 영역을 직접 구현하고 운영했습니다.

---

## 🛠 기술 스택

| 분류 | 기술 |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5 |
| **Build** | Gradle 8.14 |
| **Database** | MySQL 8 |
| **ORM** | Spring Data JPA / Hibernate |
| **Auth** | OAuth2 Client (Naver, Google) + JWT (jjwt 0.11.5) |
| **Realtime** | Spring WebSocket / STOMP / SockJS |
| **Storage** | AWS S3 (AWS SDK for Java v2) |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Monitoring** | Spring Boot Actuator |
| **Infra** | Docker (multi-stage build) · AWS EC2 · S3 · Route 53 · CloudFront |
| **CI/CD** | GitHub Actions · DockerHub · nginx + Let's Encrypt(SSL) |

---

## 🏗 아키텍처 & 패키지 구조

도메인별로 패키지를 분리하고, 각 도메인은 `controller → service → repository → domain` 4계층으로 구성했습니다.
거래 관련 게시글은 **공통 엔티티(`MarketPost`)를 중심으로 한 단일 테이블 + 타입 분기 구조**로 설계해, 중고/특전·한정판/경매/구해요가 공통 로직을 재사용하도록 했습니다.

```
com.akiba.backend
├── user          # 회원 / OAuth2 로그인 / JWT 인증
├── profile       # 프로필 조회, 팔로우/팔로잉
├── market        # 마켓 통합 서비스 (중고 + 특전/한정판 공통 핵심 로직)
├── used          # 중고거래 + 공통 도메인(MarketPost, Tag, Category 등)
├── limited       # 특전/한정판 (MarketPostService 재사용, type=LIMITED)
├── auction       # 경매 (입찰 / 즉시구매 / 자동 종료 스케줄러)
├── wanted        # 구해요 (MarketPost + WantedPost 이중 저장)
├── board         # 커뮤니티 게시판 (자유/정품감정/Q&A)
├── chat          # WebSocket/STOMP 실시간 채팅
├── search        # 인기 검색어 집계 + 정리 스케줄러
├── media         # 파일 업로드/다운로드 (AWS S3)
├── notification  # 알림 도메인 + Discord Webhook 연동
├── report        # 회원/게시글 신고
├── deal          # 거래 / 거래 후기 도메인
└── config        # Security, JWT, WebSocket, S3, Swagger, 전역 예외 처리
```

### 거래 게시글의 공통 설계 (MarketPost 중심)

| 타입 | 전용 테이블 | 특징 |
|---|---|---|
| `USED` (중고) | - | MarketPost 단독 |
| `LIMITED` (특전/한정판) | - | URL/필터만 분리, 서비스 재사용 |
| `AUCTION` (경매) | `AuctionPost`, `AuctionBid` | 입찰/낙찰 로직 추가 |
| `WANTED` (구해요) | `WantedPost` | 희망 가격대·상태 별도 저장 |

> 공통 정보(제목·내용·상태·이미지·태그)는 `MarketPost`에, 도메인 전용 정보는 각 전용 테이블에 저장하고 조회 시 JOIN하는 구조로, 중복 코드를 줄이고 검색·인기글·유사상품 로직을 한 곳에서 관리합니다.

---

## ✨ 구현 기능

### 👤 회원 / 인증 (`/api/users`, `/api/profile`)
- **OAuth2 소셜 로그인** — Naver, Google (인가 코드 → 액세스 토큰 → 프로필 조회 직접 구현)
- **JWT 인증** — 액세스 토큰(24h) / 리프레시 토큰(14d) 발급·갱신, `RefreshToken` 엔티티로 서버 측 관리
- Stateless 세션 정책 + 커스텀 `JwtAuthenticationFilter`로 매 요청 토큰 검증
- 닉네임 변경 / 중복 확인, 프로필(bio·이미지) 수정, 회원 탈퇴(소프트 삭제)
- **팔로우 / 언팔로우**, 팔로워·팔로잉 목록 조회 (자기 자신 팔로우·중복 팔로우 예외 처리)

### 🛒 마켓 (`/api/market`, `/api/used`, `/api/limited`)
- 게시글 CRUD + 상태 변경(`ACTIVE` / `RESERVED` / `SOLD` / `CLOSED`)
- **키워드 검색** — JPA Specification 기반 동적 쿼리 (공백 분리 토큰 매칭)
- **인기 매물** — 조회수 기준 집계
- **유사 상품 추천** — 제목·태그 토큰 점수 + 최신성 보너스 스코어링
- **최근 본 상품** 조회, **추천 검색 태그**(활성 게시글 상위 태그), 카테고리 트리 조회
- 특전/한정판은 동일 서비스(`MarketPostService`)를 `type=LIMITED`로 재사용

### 🔨 경매 (`/api/auction`)
- 경매 게시글 CRUD (입찰이 있으면 수정·삭제 차단)
- **입찰** — 현재 최고가 + `bidStep` 이상 검증, **비관적 락(`PESSIMISTIC_WRITE`)으로 동시 입찰 정합성 보장**
- **즉시구매(Buy Now)** — `buyNowPrice`로 즉시 낙찰 처리
- 입찰 내역 / 내 입찰 / 내 경매 / 낙찰 성공 목록 조회
- 인기 경매 · 마감 임박 경매 조회
- **경매 자동 종료 스케줄러** — `@Scheduled` 1분 주기, 낙찰 → `SOLD` / 유찰 → `CLOSED` 자동 처리

### 🙏 구해요 (`/api/wanted`)
- 구해요 게시글 CRUD (`MarketPost(type=WANTED)` + `WantedPost` 이중 저장)
- 희망 가격 범위·희망 상태 관리, 유사 구해요 조회

### 💬 커뮤니티 게시판 (`/api/boards`)
- 게시판 3종 지원: 자유(`FREE`) / 정품감정(`AUTHENTICITY`) / Q&A·도움(`QNA_HELP`)
- 게시글 CRUD, 댓글·대댓글(1depth 제한 검증), 게시글·댓글 좋아요 토글
- **정품 감정 투표** (`AUTHENTIC` / `FAKE`)
- 인기 게시글(좋아요 TOP), 해시태그 검색 / 전체 검색
- 앱 기동 시 게시판 초기 데이터 자동 생성(`BoardDataInitializer`)

### 💬 실시간 채팅 (`/api/chat`, WebSocket)
- **STOMP over WebSocket** + SockJS 폴백, 엔드포인트 `/ws/chat`
- 메시지 브로커 `/topic`, 발행 prefix `/app`
- **WebSocket 핸드셰이크 단계에서 JWT 인증**(`WebSocketAuthInterceptor`)
- 마켓 채팅룸 생성, 채팅방 목록·메시지 조회, 채팅방 나가기

### 🔍 검색어 (`/api/market/search/popular` 등)
- 검색 시 키워드 자동 기록·카운트, 인기 검색어 TOP N 제공
- **검색어 정리 스케줄러** — 매일 03:30, 저품질·만료 키워드 자동 정리

### 📁 미디어 (`/api/media`)
- **AWS S3 파일 업로드/다운로드** (UUID 기반 키, `uploads/{uuid}.ext`)
- `media_files` 테이블로 메타데이터 관리, S3 URL 직접 생성
- (기존 EC2 로컬 디스크 저장 → S3 전환 완료)

### 🔔 알림 / 신고 (`/api/reports`)
- 알림 도메인 + 미읽음 카운트, **Discord Webhook 연동**(주요 이벤트 알림)
- 회원 신고(`MEMBER`) / 게시글 신고(`MARKET_POST`) 처리

---

## 🔐 보안 설계

- `SecurityFilterChain` 기반 Stateless 인증 (세션 미사용)
- `UsernamePasswordAuthenticationFilter` 앞단에 커스텀 **`JwtAuthenticationFilter`** 등록 → `@AuthenticationPrincipal Long userId`로 컨트롤러에서 사용자 식별
- CORS 설정(메서드·헤더·credentials 허용), CSRF 비활성화(토큰 기반 API)
- JWT 비밀키·OAuth 클라이언트 정보는 모두 환경변수로 분리 관리

---

## 🚀 인프라 & CI/CD

> 인프라 담당자 이탈 후 **배포 파이프라인과 AWS 운영을 직접 인수·운영**한 영역입니다.

### 배포 파이프라인

```
develop 브랜치 push  →  Docker 이미지 빌드(tag: dev)   →  EC2 dev 서버 배포 (port 8081)
main    브랜치 push  →  Docker 이미지 빌드(tag: latest) →  EC2 prod 서버 배포 (port 8080)
develop/main PR      →  Gradle 빌드 검증 (CI Check)
```

- **`deploy.yml`** — DockerHub 로그인 → `docker buildx --platform linux/amd64`로 멀티 아키텍처 빌드 & 푸시 → `appleboy/ssh-action`으로 EC2 접속 → 브랜치별 `.env` 동적 생성 → `deploy.sh` 실행
- **`ci-check.yml`** — PR 시 JDK 21 환경에서 Gradle 빌드 검증
- **Dockerfile** — `gradle:8.12-jdk21-alpine` 빌드 스테이지 → `eclipse-temurin:21-jre-alpine` 런타임의 **멀티 스테이지 빌드**로 이미지 경량화
- nginx + Let's Encrypt SSL, Route 53 DNS, 환경별 설정 파일(`application-dev.yml` / `application-prod.yml`) 분리

### 운영 환경
- **EC2** Ubuntu 24.04 LTS, Elastic IP 고정
- dev/prod 컨테이너 분리(`app-dev`/`app-prod`, `db-dev`/`db-prod`), Docker 네트워크 분리
- GitHub Actions Secrets로 DB·OAuth·JWT·AWS·Discord 자격증명 일괄 관리

---

## 🧩 트러블슈팅 & 핵심 학습

실제 운영하며 마주친 문제와 해결 과정입니다.

### 1. S3 이미지 CORS 표시 오류 (리다이렉트 헤더 유실)
S3 전환 후 게시글 이미지가 표시되지 않는 문제 발생. 원인은 **리전이 누락된 레거시 S3 URL 형식이 301 리다이렉트를 유발**하고, 그 과정에서 `Access-Control-Allow-Origin` 헤더가 조용히 사라지는 것이었음. → URL 생성 로직(`MediaService`)을 리전 포함 형식으로 수정해 해결.
> **학습:** CORS 헤더는 리다이렉트 체인을 거치며 소리 없이 유실될 수 있다.

### 2. EC2 메모리 부족(OOM)
t3.small(1.9GB RAM)에서 dev/prod 컨테이너 동시 구동 시 OOM 발생. → **2GB Swap을 추가하고 `/etc/fstab`에 영속화**, 불필요한 prod 컨테이너는 미사용 시 중지하는 운영 정책 수립.

### 3. ARM ↔ AMD64 아키텍처 불일치
Apple Silicon(M시리즈, ARM)에서 빌드한 이미지가 AMD64 EC2에서 실행 실패. → CI에서 `docker buildx build --platform linux/amd64`로 타깃 아키텍처 명시.

### 4. 배포 스크립트 false rollback
Spring Boot 기동에 약 18초 소요되어, 배포 헬스체크의 `sleep` 타임아웃이 짧으면 정상 기동을 실패로 오판. → 기동 시간을 반영해 타임아웃 조정.

### 5. `@RequiredArgsConstructor` NPE
주입 필드에 `final`이 빠지면 생성자 주입이 생성되지 않아 NullPointerException 발생 → 의존성 필드 `final` 일관 적용.

---

## 📡 API 엔드포인트 요약

| 도메인 | Base URL | 대표 기능 |
|---|---|---|
| 인증 | `/api/users` | 로그인, 토큰 갱신, 내 정보, 닉네임 |
| 프로필 | `/api/profile` | 프로필 조회, 팔로우/언팔로우 |
| 마켓 통합 | `/api/market` | 검색, 인기/유사 매물, 최근 본 상품 |
| 중고거래 | `/api/used` | 게시글 CRUD, 인기 매물 |
| 특전/한정판 | `/api/limited` | 게시글 CRUD, 상태 변경, 유사글 |
| 경매 | `/api/auction` | 입찰, 즉시구매, 내 입찰/낙찰 |
| 구해요 | `/api/wanted` | 구해요 CRUD |
| 커뮤니티 | `/api/boards` | 게시글·댓글·좋아요·정품투표 |
| 채팅 | `/api/chat`, `/ws/chat` | 채팅방·메시지 (WebSocket) |
| 미디어 | `/api/media` | S3 업로드/다운로드 |
| 신고 | `/api/reports` | 회원/게시글 신고 |

---

## ⚙️ 로컬 실행

```bash
# 1. 환경 변수 (.env 또는 IDE Run Configuration)
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
JWT_SECRET_KEY=...
DB_NAME=...
DB_PASSWORD=...
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_S3_BUCKET_NAME=...
AWS_S3_REGION=...

# 2. 빌드 및 실행
./gradlew clean build -x test
java -jar build/libs/akiba-backend-0.0.1-SNAPSHOT.jar
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## 🗺 로드맵 (진행 예정)

- [ ] **검색 고도화** — JPA Specification + LIKE → MySQL ngram Full-Text Search 전환
- [ ] **Apple Sign-In** 추가 (프론트와 Bundle ID 연동)
- [ ] 알림 발송 로직 — 입찰/댓글/채팅 이벤트 실시간 연동
- [ ] 거래(Deal) 생성·완료·후기 작성 API
- [ ] 테스트 코드 작성 (현재 `-x test` 빌드)
- [ ] 프로덕션 배포 마무리 (`test-login` 엔드포인트 제거, prod nginx conf 추가)

---

> 본 문서는 `develop` 브랜치 기준으로 작성되었습니다.
