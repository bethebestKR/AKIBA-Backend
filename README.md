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

> **이 저장소에 대해**
> 팀 저장소(`ggggyeong/AKIBA-Backend`)를 포크해 작업했으며, **실제 개발 히스토리는 이 저장소에 있습니다.**
> 상단의 fork 표기와 달리 원본에는 초기 커밋만 있고, 129개 커밋과 CI/CD 설정, 배포 구성은 모두 이곳에서 작성됐습니다.

> **현재 상태**
> 비용 문제로 AWS 인스턴스를 종료해 API 서버와 Swagger UI는 접속되지 않습니다.
> 코드와 아래 트러블슈팅 기록으로 확인해 주세요.

---

## 📌 프로젝트 개요

AKIBA는 오타쿠 굿즈를 사고팔고, 정보를 나누는 마켓플레이스 + 커뮤니티 서비스입니다.
일반적인 중고거래에 더해 **특전/한정판 거래**, **경매**, **구해요(역경매)**, **정품 감정 투표** 등 오타쿠 굿즈 도메인에 특화된 기능을 제공합니다.

본 저장소는 백엔드 API 서버이며, 단일 서비스로 14개 도메인 / 11개 REST 컨트롤러 / 54개 JPA 엔티티로 구성되어 있습니다.

### 팀 구성과 담당 범위

5인 팀(백엔드 2 · 프론트엔드 1,디자인 1, PM 1)으로 진행했습니다.

**제가 담당한 영역**

| 영역 | 내용 |
|---|---|
| AWS 인프라 | EC2 서버 구축, Docker 컨테이너 구성, Route 53, nginx + Let's Encrypt SSL |
| CI/CD | 전임자의 배포 파이프라인 인수 → 새 EC2로 이전 · dev/prod 환경 분리 |
| 인증 | OAuth2 소셜 로그인(Google, Naver) 직접 구현 + JWT 발급·갱신 |
| 실시간 통신 | STOMP over WebSocket 채팅, CONNECT 시점 JWT 인증 |
| 미디어 | 컨테이너 내부 저장 → AWS S3 전환 |

인프라는 별도 담당자 없이 직접 구축·운영했습니다.

**다른 백엔드 팀원 담당** — 마켓(중고·특전/한정판) 도메인 설계, 경매 도메인, 커뮤니티 게시판, 검색·추천 로직

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
| **Infra** | Docker (multi-stage build) · AWS EC2 · S3 · Route 53 |
| **CI/CD** | GitHub Actions · DockerHub · nginx + Let's Encrypt(SSL) |

---

## 🏗 아키텍처 & 패키지 구조

도메인별로 패키지를 분리하고, 각 도메인은 `controller → service → repository → domain` 4계층으로 구성했습니다.

```
com.akiba.backend
├── user          # 회원 / OAuth2 로그인 / JWT 인증
├── profile       # 프로필 조회, 팔로우/팔로잉
├── market        # 마켓 통합 서비스 (중고 + 특전/한정판 공통 로직)
├── used          # 중고거래 + 공통 도메인(MarketPost, Tag, Category 등)
├── limited       # 특전/한정판 (MarketPostService 재사용, type=LIMITED)
├── auction       # 경매 (입찰 / 즉시구매 / 자동 종료 스케줄러)
├── wanted        # 구해요 (MarketPost + WantedPost)
├── board         # 커뮤니티 게시판 (자유/정품감정/Q&A)
├── chat          # WebSocket/STOMP 실시간 채팅
├── search        # 인기 검색어 집계 + 정리 스케줄러
├── media         # 파일 업로드/다운로드 (AWS S3)
├── notification  # 알림 도메인 + Discord Webhook 연동
├── report        # 회원/게시글 신고
├── deal          # 거래 / 거래 후기 도메인
└── config        # Security, JWT, WebSocket, S3, Swagger, 전역 예외 처리
```

거래 관련 게시글은 공통 엔티티 `MarketPost`에 공통 정보(제목·내용·상태·이미지·태그)를 두고, 타입별 전용 정보만 별도 테이블에 저장한 뒤 조회 시 JOIN하는 구조입니다. 검색·인기글·유사상품 로직을 한 곳에서 관리하기 위한 설계입니다.

| 타입 | 전용 테이블 | 특징 |
|---|---|---|
| `USED` (중고) | - | MarketPost 단독 |
| `LIMITED` (특전/한정판) | - | URL/필터만 분리, 서비스 재사용 |
| `AUCTION` (경매) | `AuctionPost`, `AuctionBid` | 입찰/낙찰 로직 추가 |
| `WANTED` (구해요) | `WantedPost` | 희망 가격대·상태 별도 저장 (`@MapsId`로 PK 공유) |

---

## ✨ 구현 기능

### 👤 회원 / 인증 (`/api/users`, `/api/profile`)

- **OAuth2 소셜 로그인** — Naver, Google (인가 코드 → 액세스 토큰 → 프로필 조회 직접 구현)
- **JWT 인증** — 액세스 토큰 / 리프레시 토큰 발급·갱신
- **리프레시 토큰은 `RefreshToken` 엔티티로 서버 측 저장** — 유출 시 서버에서 레코드를 지워 재발급을 차단할 수 있게 하기 위함
- Stateless 세션 정책 + 커스텀 `JwtAuthenticationFilter`로 요청별 토큰 검증
- 닉네임 변경 / 중복 확인, 프로필(bio·이미지) 수정, 회원 탈퇴(소프트 삭제)
- 팔로우 / 언팔로우, 팔로워·팔로잉 목록 조회

### 💬 실시간 채팅 (`/api/chat`, WebSocket)

- **STOMP over WebSocket** + SockJS 폴백, 엔드포인트 `/ws/chat`
- 메시지 브로커 `/topic`, 발행 prefix `/app`
- **STOMP CONNECT 시점에 JWT 검증** 후 세션에 `userId` 저장 (`WebSocketAuthInterceptor`) — 이후 메시지마다 토큰을 다시 파싱하지 않음
- 인증 실패 시 `MessageDeliveryException`으로 연결 차단
- 마켓 채팅룸 생성, 채팅방 목록·메시지 조회, 채팅방 나가기

### 📁 미디어 (`/api/media`)

- **AWS S3 파일 업로드/다운로드** (UUID 기반 키, `uploads/{uuid}.ext`)
- `media_files` 테이블로 메타데이터 관리
- 컨테이너 재배포 시 업로드 파일이 사라지는 문제로 EC2 로컬 디스크 저장에서 S3로 전환

### 🛒 마켓 · 🔨 경매 · 💬 게시판 · 🔍 검색 (다른 팀원 담당)

- 마켓: 게시글 CRUD 및 상태 변경, JPA Specification 기반 키워드 검색, 인기 매물, 유사 상품 추천
- 경매: 경매 글 CRUD, 입찰(최고가 + `bidStep` 검증), 즉시구매, `@Scheduled` 1분 주기 자동 종료
  - 동시 입찰 정합성은 **비관적 락**(`@Lock(PESSIMISTIC_WRITE)`, lock timeout 3초)으로 처리하고, 입찰 수는 원자적 `UPDATE`로 증가시켜 Lost Update를 차단합니다
- 커뮤니티: 게시판 3종, 댓글·대댓글, 좋아요 토글, 정품 감정 투표
- 검색어: 키워드 자동 기록·집계, 인기 검색어, 정리 스케줄러

---

## 🚀 인프라 & CI/CD

> 별도 담당자 없이 직접 구축·운영한 영역입니다.

### 서버 구조

```
Route 53 (DNS)
  ↓
EC2 (Ubuntu 24.04, t3.small, 탄력적 IP)
  ↓
nginx (80/443) — SSL 종료 + 리버스 프록시 + WebSocket Upgrade 전달
  ↓
app-dev (Spring Boot)
  ↓
db-dev / db-prod (MySQL 8, 컨테이너 분리 + Docker 네트워크 분리)
```

DB를 별도 관리형 서비스로 분리하지 않고 EC2 내부 컨테이너로 운영했습니다. 팀 프로젝트로 비용을 직접 부담해야 해서 인스턴스 한 대로 유지하는 것을 우선했고, 그 판단의 결과로 겪은 문제는 아래 트러블슈팅에 정리했습니다.

### 배포 파이프라인

```
develop 브랜치 push  →  이미지 빌드(tag: dev)     →  EC2 dev 배포
main    브랜치 push  →  이미지 빌드(tag: latest)  →  EC2 prod 배포
develop/main PR      →  Gradle 빌드 검증 (CI Check)
```

- **`deploy.yml`** — DockerHub 로그인 → `docker buildx build --platform linux/amd64`로 타깃 아키텍처 명시 빌드 & 푸시 → `appleboy/ssh-action`으로 EC2 접속 → 브랜치별 `.env` 동적 생성 → `deploy.sh` 실행
- **`ci-check.yml`** — PR 시 JDK 21 환경에서 Gradle 빌드 검증
- **Dockerfile** — 멀티 스테이지 빌드(빌드 스테이지 → JRE 런타임)로 이미지 경량화
- 환경별 설정 파일(`application-dev.yml` / `application-prod.yml`) 분리
- GitHub Actions Secrets로 DB · OAuth · JWT · AWS 자격증명 관리

---

## 🧩 트러블슈팅

실제 운영하며 마주친 문제와 해결 과정입니다.

### 1. STOMP CONNECT에 토큰을 보냈는데 세션의 `userId`가 `null`이었다

채팅 메시지 저장이 되지 않았고, 핸들러에서 세션의 `userId`가 `null`로 나왔습니다. 프론트는 CONNECT 프레임에 `Authorization: Bearer`를 정상적으로 실어 보내고 있었고 토큰 검증도 통과하는 상태였습니다.

인터셉터를 역추적해보니 `StompHeaderAccessor.wrap(message)`를 사용하고 있었습니다. 이 메서드는 원본 메시지를 **복사해** 새 accessor를 만들기 때문에, 여기에 저장한 값은 실제 WebSocket 세션에 반영되지 않습니다.

```java
// Before — 복사본이라 세션에 반영되지 않음
StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
accessor.getSessionAttributes().put("userId", userId);

// After — 원본 메시지의 accessor를 참조
StompHeaderAccessor accessor =
    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
```

같이 발견한 문제로, 토큰이 없거나 유효하지 않을 때 예외 없이 통과하는 구조였습니다. 인증 실패 시 `MessageDeliveryException`을 던지도록 함께 수정했습니다.

> `wrap()`은 읽기 전용 파싱에 적합하고, 세션 쓰기에는 원본 accessor를 참조해야 합니다.

### 2. 로컬에서 되던 WebSocket이 dev 서버에서만 400으로 끊겼다

`wss://` 연결이 핸드셰이크 단계에서 400으로 실패했고, 로컬에서는 재현되지 않았습니다. 원인이 두 개였습니다.

**클라이언트 경로** — 로컬 테스트는 SockJS 클라이언트로 했지만 실제 Flutter의 `stomp_dart_client`는 순수 WebSocket입니다. 서버가 `withSockJS()`로 등록돼 있으면 순수 WebSocket용 경로는 `/ws/chat/websocket`입니다. 테스트 클라이언트와 실제 클라이언트가 달라 로컬에서 걸러지지 않았습니다.

**nginx 설정 (핵심 원인)** — WebSocket은 HTTP 연결을 프로토콜 전환으로 승격시키는데, 전환 신호를 백엔드로 넘기는 헤더가 없어 프록시가 일반 HTTP 요청으로 전달하고 있었습니다.

```nginx
location / {
    proxy_pass http://app-dev:8080;
    ...
    proxy_http_version 1.1;                    # 추가
    proxy_set_header Upgrade $http_upgrade;    # 추가
    proxy_set_header Connection "upgrade";     # 추가
}
```

### 3. 인프라 담당자 이탈 후 파이프라인을 새 EC2로 이전했다

인프라를 맡던 팀원이 떠나면서 배포 파이프라인을 인수했습니다. 인계는 구두로 간단히 받은 정도였고 기존 서버를 계속 쓸 수 없어, EC2를 새로 띄워 옮겼습니다. 파이프라인은 남아 있었지만 대상 서버가 바뀌면서 전제가 달라진 부분이 드러났습니다.

| 인수 당시 | 변경 후 |
|---|---|
| `docker build` (빌드 장비 아키텍처 종속) | `docker buildx build --platform linux/amd64` |
| `ci-check.yml` JDK 17 (앱은 21로 동작) | JDK 21 |
| `NAVER_CLIENT_ID` 단일 시크릿 | `NAVER_CLIENT_ID_DEV` / `_PROD` 분리 |
| `jdbc:mysql://localhost` | `jdbc:mysql://db-dev` (컨테이너 이름) |

- **아키텍처 종속** — 로컬(ARM)에서 빌드한 이미지를 AMD64 EC2에서 pull할 수 없었습니다(`no matching manifest for linux/amd64`). 빌드 장비에 배포 성공 여부가 달리는 구조가 원인이라 보고, CI에서 플랫폼을 고정해 빌드하도록 옮겨 로컬 빌드를 배포 경로에서 제외했습니다.
- **CI 검증 JDK 불일치** — 애플리케이션은 21로 동작하는데 빌드 검증만 17이어서, PR 통과가 실제 빌드를 보장하지 못하는 상태였습니다.
- **단일 환경 전제** — dev/prod를 나누면 각 환경이 서로 다른 OAuth 앱을 써야 하므로 시크릿을 환경별로 분리했습니다.
- **컨테이너 네트워크** — 컨테이너 안에서 `localhost`는 자기 자신을 가리키므로, 같은 도커 네트워크의 다른 컨테이너는 컨테이너 이름으로 접근해야 합니다.

서버 변경에 따라 도메인을 새로 구매해 DNS와 SSL 인증서를 재발급하고, nginx 설정과 DB 연결을 맞춘 뒤 배포가 정상화됐습니다.

### 4. 배포가 롤백됐는데 원인은 18시간 전에 죽어 있던 DB 컨테이너였다

배포 후 헬스체크 실패로 롤백이 걸렸습니다. 컨테이너 로그에서 앱이 `db-dev` 호스트를 찾지 못하는 것을 확인하고 DB 쪽을 점검했습니다.

```
$ docker ps -a | grep db
997187ee3363  mysql:8.0  Exited (137) 18 hours ago  db-dev
26757946accb  mysql:8.0  Up 12 days                 db-prod

$ free -h
               total    used    free
Mem:           1.9Gi   1.1Gi   310Mi
Swap:             0B      0B      0B      ← Swap 없음
```

Exit 137은 `SIGKILL`이며, 메모리 부족 시 OOM Killer가 프로세스를 종료할 때 발생합니다. 1.9GB 인스턴스에 앱과 DB 컨테이너 네 개가 올라가 있고 Swap이 없는 상태였습니다.

해결 전에 선택지를 비교했습니다 — dev/prod를 EC2 두 대로 분리, DB 통합, 인스턴스 타입 상향, Swap 추가. 앞의 셋은 비용이 오르거나 개발 데이터를 매번 다시 넣어야 해서, 실사용자가 없는 단계에서는 과하다고 판단했습니다.

- 2GB Swap 추가 후 `/etc/fstab`에 등록해 재부팅 후에도 유지
- DB 컨테이너에 `restart=unless-stopped` 적용

Swap은 근본 해결이 아니라 안전망입니다. 원인은 한 인스턴스에 과하게 올린 구조 자체이므로, 실사용자가 붙는 시점의 인스턴스 상향 또는 DB 분리를 남은 과제로 둡니다.

### 5. 컨테이너를 재배포하면 업로드한 이미지가 사라졌다

파일이 앱 컨테이너 내부 디스크에 저장되고 있어, 컨테이너가 교체되면 함께 사라졌습니다. 컨테이너는 배포마다 교체되는 것을 전제로 쓰고 있었으니 파일을 그 안에 두는 것 자체가 전제와 맞지 않았습니다.

볼륨으로 호스트에 남기는 방법도 있었지만 그러면 파일이 특정 EC2에 묶여 인스턴스 교체 시 같은 문제가 재발합니다. 업로드 경로를 S3로 옮겨, 애플리케이션은 키와 URL만 DB에 남기도록 했습니다.

### 6. 배포 헬스체크가 정상 기동을 실패로 판단했다

Spring Boot 기동에 약 18초가 걸리는데 헬스체크 대기 시간이 그보다 짧아, 뜨는 중인 애플리케이션을 실패로 처리했습니다. 대기 시간을 기동 시간에 맞춰 조정했습니다. 고정 `sleep`에 의존하는 방식은 기동 시간이 늘어나면 다시 깨지므로, Actuator 헬스 엔드포인트 폴링으로 바꾸는 것을 남은 과제로 둡니다.

### 7. Swagger UI는 HTTPS인데 API 호출은 HTTP로 나갔다

배포 서버의 Swagger UI에서 호출이 막혔고 Postman에서는 정상이었습니다. 브라우저에서만 막히니 CORS를 의심했지만 실제 원인은 프로토콜이었습니다. UI는 `https`로 열렸는데 호출 주소가 `http`로 생성되어, 브라우저가 다른 출처로 간주한 것입니다. 호출 주소를 프로토콜에 맞춰 생성되도록 수정했습니다.

그 전에는 Swagger가 500(`Failed to load API definition`)을 반환하는 문제가 있었고, 예외 처리 어노테이션과의 버전 충돌이어서 버전을 올려 해결했습니다.

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

## ⚠️ 알려진 한계 / 남은 작업

현재 코드 기준으로 미완인 부분을 명시합니다.

- **인가 처리 위치** — 토큰 검증은 `JwtAuthenticationFilter`에서 매 요청 수행하지만, 경로별 인가 규칙을 Spring Security에 두지 않고 컨트롤러에서 `userId` null 체크로 처리했습니다. 마켓·경매·게시판 등은 이 방식으로 막고 있으나 채팅·프로필·신고 컨트롤러에는 체크가 빠져 있어, 인가를 필터 레벨로 통일하는 것이 남은 작업입니다.
- **배포 헬스체크** — 고정 대기 시간에 의존하고 있어 Actuator 폴링 방식으로 전환 예정입니다.
- **인스턴스 구성** — 앱과 DB가 한 인스턴스에 있어 메모리 여유가 없습니다. 실사용자 유입 시 DB 분리 또는 인스턴스 상향이 필요합니다.
- **채팅 메시지 저장소** — 모든 도메인을 MySQL에 저장하고 있습니다. 채팅 메시지는 append-only이고 조인 없이 방별 조회만 하며 증가 속도가 가장 빨라, 별도 저장소로 분리하는 것이 적절하다고 보고 있습니다.

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
