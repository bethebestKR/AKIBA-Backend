// ========================================================================
// 파일 경로: com/akiba/backend/market/controller/MarketPostController.java
// 설명: 중고거래/특전한정판 관련 REST API 엔드포인트
//
// [역할]
// - 프론트에서 오는 HTTP 요청을 받아서 Service에 전달
// - Service에서 받은 결과를 HTTP 응답으로 반환
// - Controller는 로직을 직접 처리하지 않고, Service에 위임만 함
// ========================================================================
package com.akiba.backend.market.controller;

import com.akiba.backend.market.dto.request.*;
import com.akiba.backend.market.dto.response.*;
import com.akiba.backend.market.service.MarketPostService;
import com.akiba.backend.search.service.SearchKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketPostController {

    private final MarketPostService marketPostService;
    private final SearchKeywordService searchKeywordService;

    // 1. 마켓 통합 검색 — GET /api/market/search?keyword=나루토&type=USED
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "true") boolean onlyActive,
            @RequestParam(defaultValue = "false") boolean unOpenedOnly,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                marketPostService.searchPosts(keyword, type, onlyActive, unOpenedOnly, sort, page, size)
        );
    }

    // 2. 인기 검색어 조회 — GET /api/market/search/popular
    @GetMapping("/search/popular")
    public ResponseEntity<PopularKeywordResponse> getPopularKeywords(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchKeywordService.getPopularKeywords(limit));
    }

    // 3. 추천 검색 태그 조회 — GET /api/market/tags/recommended
    @GetMapping("/tags/recommended")
    public ResponseEntity<Map<String, Object>> getRecommendedTags(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(Map.of("tags", marketPostService.getRecommendedTags(type, limit)));
    }

    // 4. 카테고리 목록 조회 — GET /api/market/categories
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        return ResponseEntity.ok(Map.of("categories", marketPostService.getCategories()));
    }

    // 5. 최근 본 상품 조회 — GET /api/market/posts/recent-views?limit=10
    @GetMapping("/posts/recent-views")
    public ResponseEntity<Map<String, Object>> getRecentViews(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        Long resolvedUserId = requireUserId(userId);
        return ResponseEntity.ok(Map.of("posts", marketPostService.getRecentViews(resolvedUserId, limit)));
    }

    // 6. 마켓 통합 목록 조회 — GET /api/market/posts
    @GetMapping("/posts")
    public ResponseEntity<Map<String, Object>> getPosts(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") boolean onlyActive,
            @RequestParam(defaultValue = "false") boolean unOpenedOnly,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                marketPostService.getIntegratedPostList(
                        type, status, keyword, onlyActive, unOpenedOnly, sort, page, size
                )
        );
    }

    // 7. 마켓 통합 상세 조회 — GET /api/market/posts/{postId}
    @GetMapping("/posts/{postId}")
    public ResponseEntity<MarketPostDetailResponse> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {

        return ResponseEntity.ok(marketPostService.getPostDetail(postId, userId));
    }

    // 8. 유사 상품 조회 — GET /api/market/posts/{postId}/similar?limit=10
    @GetMapping("/posts/{postId}/similar")
    public ResponseEntity<Map<String, Object>> getSimilarPosts(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(Map.of("posts", marketPostService.getSimilarPostsByKeywordScore(postId, limit)));
    }

    // 9. 인기 매물 조회 — GET /api/market/posts/popular?type=USED&limit=10
    @GetMapping("/posts/popular")
    public ResponseEntity<Map<String, Object>> getPopularPosts(
            @RequestParam String type,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(Map.of("posts", marketPostService.getPopularPosts(type, limit)));
    }

    // 10. 게시글 상태 변경 — PATCH /api/market/posts/{postId}/status
    @PatchMapping("/posts/{postId}/status")
    public ResponseEntity<Map<String, String>> changeStatus(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @jakarta.validation.Valid @RequestBody MarketPostStatusRequest request) {

        marketPostService.changeStatus(postId, requireUserId(userId), request);
        return ResponseEntity.ok(Map.of("message", "상태가 변경되었습니다."));
    }

    private Long requireUserId(Long principalUserId) {
        if (principalUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보가 없습니다.");
        }
        return principalUserId;
    }
}
