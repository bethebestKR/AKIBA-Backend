// ========================================================================
// 파일 경로: com/akiba/backend/limited/controller/LimitedPostController.java
// 설명: 특전/한정판 관련 REST API 엔드포인트
//
// [특전/한정판의 구조]
// - 실제 데이터는 MarketPost(type=LIMITED)로 저장
// - market과 같은 테이블을 사용하지만, URL과 필터가 다름
// - MarketPostService를 재사용하여 type=LIMITED로 필터링
// - 별도의 Service 없이 MarketPostService에 위임
//
// [market과의 차이점]
// - URL: /api/limited/... (market은 /api/market/...)
// - type 파라미터 고정: LIMITED
// - 특전/한정판 전용 카테고리 필터 가능
// - 구매처/영수증 인증이 더 중요
// ========================================================================
package com.akiba.backend.limited.controller;

import com.akiba.backend.market.dto.request.*;
import com.akiba.backend.market.dto.response.*;
import com.akiba.backend.market.service.MarketPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/limited")
@RequiredArgsConstructor
public class LimitedPostController {

    private final MarketPostService marketPostService;

    // =========================================================================
    // 1. 특전/한정판 글 작성
    // POST /api/limited/posts
    // =========================================================================
    // 프론트에서 type을 "LIMITED"로 보내야 함
    // MarketPostCreateRequest의 type 필드가 "LIMITED"
    // =========================================================================
    @PostMapping("/posts")
    public ResponseEntity<Map<String, Object>> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MarketPostCreateRequest request) {

        // type을 LIMITED로 강제 설정하기 위해 별도 처리도 가능하지만
        // 프론트에서 type: "LIMITED"로 보내는 것을 신뢰
        Long postId = marketPostService.createPost(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("postId", postId, "message", "특전/한정판 게시글이 등록되었습니다."));
    }

    // =========================================================================
    // 2. 특전/한정판 목록 조회
    // GET /api/limited/posts?sort=latest&page=0&size=20
    // =========================================================================
    // type=LIMITED로 고정하여 MarketPostService 호출
    // =========================================================================
    @GetMapping("/posts")
    public ResponseEntity<Map<String, Object>> getPostList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> result = marketPostService.getPostList(
                "LIMITED", categoryId, status, sort, page, size);
        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // 3. 특전/한정판 상세 조회
    // GET /api/limited/posts/{postId}
    // =========================================================================
    // MarketPostService.getPostDetail()을 그대로 사용
    // 응답에 type이 "LIMITED"로 포함됨
    // =========================================================================
    @GetMapping("/posts/{postId}")
    public ResponseEntity<MarketPostDetailResponse> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {

        return ResponseEntity.ok(marketPostService.getPostDetail(postId, userId));
    }

    // =========================================================================
    // 4. 특전/한정판 글 수정
    // PUT /api/limited/posts/{postId}
    // =========================================================================
    @PutMapping("/posts/{postId}")
    public ResponseEntity<Map<String, String>> updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MarketPostUpdateRequest request) {

        marketPostService.updatePost(postId, userId, request);
        return ResponseEntity.ok(Map.of("message", "특전/한정판 게시글이 수정되었습니다."));
    }

    // =========================================================================
    // 5. 특전/한정판 글 삭제
    // DELETE /api/limited/posts/{postId}
    // =========================================================================
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Map<String, String>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {

        marketPostService.deletePost(postId, userId);
        return ResponseEntity.ok(Map.of("message", "특전/한정판 게시글이 삭제되었습니다."));
    }

    // =========================================================================
    // 6. 특전/한정판 상태 변경
    // PATCH /api/limited/posts/{postId}/status
    // =========================================================================
    @PatchMapping("/posts/{postId}/status")
    public ResponseEntity<Map<String, String>> changeStatus(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @jakarta.validation.Valid @RequestBody MarketPostStatusRequest request) {

        marketPostService.changeStatus(postId, userId, request);
        return ResponseEntity.ok(Map.of("message", "상태가 변경되었습니다."));
    }

    // =========================================================================
    // 7. 특전/한정판 인기 매물 조회
    // GET /api/limited/posts/popular?limit=10
    // =========================================================================
    @GetMapping("/posts/popular")
    public ResponseEntity<Map<String, Object>> getPopularPosts(
            @RequestParam(defaultValue = "10") int limit) {

        List<MarketPostSimpleResponse> posts = marketPostService.getPopularPosts("LIMITED", limit);
        return ResponseEntity.ok(Map.of("posts", posts));
    }

    // =========================================================================
    // 8. 특전/한정판 검색
    // GET /api/limited/search?keyword=주술회전&sort=latest&page=0&size=20
    // =========================================================================
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(marketPostService.searchPosts(keyword, "LIMITED", true, false, sort, page, size));
    }

    // =========================================================================
    // 9. 유사 특전/한정판 조회
    // GET /api/limited/posts/{postId}/similar?limit=10
    // =========================================================================
    @GetMapping("/posts/{postId}/similar")
    public ResponseEntity<Map<String, Object>> getSimilarPosts(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(Map.of("posts", marketPostService.getSimilarPostsByKeywordScore(postId, limit)));
    }

    // =========================================================================
    // 10. 카테고리 목록 조회 (market과 공유)
    // GET /api/limited/categories
    // =========================================================================
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        return ResponseEntity.ok(Map.of("categories", marketPostService.getCategories()));
    }
}
