# AKIBA Backend

> 아키바(AKIBA) — 오타쿠 굿즈 마켓플레이스 & 커뮤니티 백엔드 서버

현재 `develop` 브랜치에서 개발 중입니다.

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Build | Gradle 8.14 |
| Database | MySQL |
| ORM | Spring Data JPA / Hibernate |
| Auth | OAuth2 (Naver) + JWT |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Infra | Docker, GitHub Actions, AWS EC2 |

---

## 프로젝트 구조

```
com.akiba.backend
├── auction      # 경매
├── board        # 커뮤니티 게시판
├── chat         # 채팅
├── config       # Security, JWT 설정
├── deal         # 거래 / 거래 후기
├── limited      # 특전·한정판
├── market       # 마켓 통합 (중고 + 특전·한정판)
├── media        # 파일 업로드
├── notification # 알림
├── search       # 인기 검색어
├── used         # 공통 도메인 (MarketPost 등)
├── user         # 회원 / 인증
└── wanted       # 구해요
```

---

## 구현 완료 기능

### 👤 회원 (User)
- 네이버 OAuth2 소셜 로그인
- JWT 액세스 토큰 / 리프레시 토큰 발급 및 갱신
- 닉네임 변경 및 중복 확인
- 프로필(bio, 프로필 이미지) 수정
- 회원 탈퇴 (소프트 삭제)
- 내 정보 조회

### 🛒 마켓 (Market)
중고거래(`USED`)·특전한정판(`LIMITED`) 공통 서비스를 `MarketPostService` 하나로 처리합니다.

- 게시글 CRUD (작성 / 목록 / 상세 / 수정 / 삭제)
- 상태 변경 (`ACTIVE` / `RESERVED` / `SOLD` / `CLOSED`)
- 키워드 검색 (JPA Specification, 공백 분리 토큰 매칭)
- 인기 매물 조회 (조회수 기준)
- 유사 상품 추천 (제목·태그 토큰 점수 + 최신성 보너스)
- 최근 본 상품 조회
- 추천 검색 태그 (ACTIVE 게시글 기준 상위 태그)
- 카테고리 트리 조회

#### 특전·한정판 (`/api/limited`)
- `MarketPostService` 재사용, `type=LIMITED` 고정
- 별도 검색·유사 상품·인기 매물 엔드포인트 제공

### 🏷 중고거래 (`/api/used`)
- `MarketPostService` 위임 구조 (`UsedPostService` → `MarketPostService`)
- 인기 매물 조회

### 🔨 경매 (Auction)
- 경매 게시글 CRUD (입찰 있으면 수정·삭제 불가)
- 입찰하기 (최고가 + bidStep 이상 검증)
- 즉시구매 (buyNowPrice로 즉시 낙찰 처리)
- 입찰 내역 전체 조회
- 내 입찰 목록 / 내 경매 목록 / 낙찰 성공 목록
- 인기 경매 / 마감 임박 경매 조회
- **경매 자동 종료 스케줄러** (1분 주기, 낙찰 → `SOLD` / 유찰 → `CLOSED`)

### 🙏 구해요 (Wanted)
- 구해요 게시글 CRUD
- `MarketPost(type=WANTED)` + `WantedPost` 이중 저장 구조
- 유사 구해요 글 조회

### 💬 커뮤니티 게시판 (Board)
세 가지 게시판 코드 지원: `FREE` / `AUTHENTICITY` / `QNA_HELP`

- 게시글 CRUD
- 댓글 / 대댓글 작성 및 삭제
- 게시글·댓글 좋아요 토글
- 정품감정 투표 (AUTHENTIC / FAKE)
- 인기 게시글 조회 (좋아요 TOP 10)
- 해시태그 검색 / 전체 검색
- 앱 기동 시 게시판 초기 데이터 자동 생성

### 🔍 검색어 (Search)
- 검색 시 키워드 자동 기록 및 카운트
- 인기 검색어 TOP N 조회
- **검색어 정리 스케줄러** (매일 03:30, 저품질·만료 키워드 삭제)

### 📁 미디어 (Media)
- 파일 업로드 (로컬 스토리지, UUID 파일명)
- 파일 다운로드 / 인라인 서빙

### 🔔 알림 (Notification)
- 알림 도메인 및 레포지토리 구현 완료
- 미읽음 카운트 조회 지원

### 💬 채팅 (Chat)
- 채팅 기능 웹소켓 통신으로 활성화
- 마켓 채팅룸 생성 지원

---

## 미구현 / TODO

- [ ] 알림 발송 로직 (입찰, 댓글, 채팅 이벤트 연동)
- [ ] 찜(Favorite) 추가·제거 API
- [ ] 팔로우/팔로잉 API
- [ ] 거래(Deal) 생성·완료·후기 작성 API
- [ ] 테스트 코드 작성 (현재 `-x test` 빌드 중)
- [ ] 파일 스토리지 → S3 전환
- [ ] 경매 입찰 알림 실시간 처리

---

## CI/CD

```
Push to develop  →  Docker 이미지 빌드 (tag: dev)  →  EC2 dev 서버 배포
Push to main     →  Docker 이미지 빌드 (tag: latest)  →  EC2 prod 서버 배포
PR to develop/main  →  Gradle 빌드 검증 (CI Check)
```

환경별 설정 파일: `application-dev.yml` / `application-prod.yml`

---

## 로컬 실행

```bash
# 환경 변수 설정 (.env 또는 IDE Run Configuration)
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
JWT_SECRET_KEY=...
DB_NAME=...
DB_PASSWORD=...

# 빌드 및 실행
./gradlew clean build -x test
java -jar build/libs/akiba-backend-0.0.1-SNAPSHOT.jar
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## API 엔드포인트 요약

| 도메인 | Base URL |
|---|---|
| 인증 | `/api/users` |
| 마켓 통합 | `/api/market` |
| 중고거래 | `/api/used` |
| 특전·한정판 | `/api/limited` |
| 경매 | `/api/auction` |
| 구해요 | `/api/wanted` |
| 커뮤니티 | `/api/boards` |
| 미디어 | `/api/media` |
