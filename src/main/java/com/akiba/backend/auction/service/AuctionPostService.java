// ========================================================================
// 파일 경로: com/akiba/backend/auction/service/AuctionPostService.java
// 설명: 경매 관련 모든 비즈니스 로직
//
// [경매의 특이점]
// - MarketPost(type=AUCTION) + AuctionPost 두 테이블 사용
// - 입찰(AuctionBid)은 별도 테이블, 입찰 시 검증 로직이 복잡
// - 경매 종료 시 낙찰자(winner) 결정 로직 필요
// - 즉시구매(buyNow) 기능: buyNowPrice로 바로 낙찰
//
// [주요 기능]
//  1. 경매 글 작성
//  2. 경매 목록 조회
//  3. 경매 상세 조회
//  4. 경매 글 수정 (입찰 없을 때만)
//  5. 경매 글 삭제 (입찰 없을 때만)
//  6. 입찰하기
//  7. 즉시구매
//  8. 입찰 내역 조회
//  9. 내 입찰 목록 조회
// 10. 인기 경매 조회
// 11. 마감 임박 경매 조회
// 12. 경매 검색
// 13. 경매 종료 처리 (스케줄러용)
// ========================================================================
package com.akiba.backend.auction.service;

import com.akiba.backend.auction.dto.request.*;
import com.akiba.backend.auction.dto.response.*;
import com.akiba.backend.auction.domain.AuctionBid;
import com.akiba.backend.auction.domain.AuctionPost;
import com.akiba.backend.auction.repository.AuctionBidRepository;
import com.akiba.backend.auction.repository.AuctionPostRepository;
import com.akiba.backend.media.domain.MediaFile;
import com.akiba.backend.media.repository.MediaFileRepository;
import com.akiba.backend.search.service.SearchKeywordService;
import com.akiba.backend.used.domain.DeliveryMethod;
import com.akiba.backend.used.domain.MarketPost;
import com.akiba.backend.used.domain.MarketPostStatus;
import com.akiba.backend.used.domain.MarketSpecialType;
import com.akiba.backend.used.domain.MarketPostType;
import com.akiba.backend.used.domain.MarketPostImage;
import com.akiba.backend.used.domain.MarketPostTag;
import com.akiba.backend.used.domain.ProductCondition;
import com.akiba.backend.used.domain.Tag;
import com.akiba.backend.used.repository.MarketPostFavoriteRepository;
import com.akiba.backend.used.repository.MarketPostImageRepository;
import com.akiba.backend.used.repository.MarketPostRepository;
import com.akiba.backend.used.repository.MarketPostTagRepository;
import com.akiba.backend.used.repository.MarketCategoryRepository;
import com.akiba.backend.used.repository.TagRepository;
import com.akiba.backend.user.domain.User;
import com.akiba.backend.user.domain.UserProfile;
import com.akiba.backend.user.repository.UserProfileRepository;
import com.akiba.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionPostService {

    private final MarketPostRepository marketPostRepository;
    private final AuctionPostRepository auctionPostRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final MarketPostImageRepository marketPostImageRepository;
    private final MarketPostTagRepository marketPostTagRepository;
    private final MarketCategoryRepository marketCategoryRepository;
    private final MarketPostFavoriteRepository marketPostFavoriteRepository;
    private final TagRepository tagRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SearchKeywordService searchKeywordService;

    // =========================================================================
    // 1. 경매 글 작성
    // =========================================================================
    // MarketPost(type=AUCTION) 먼저 저장 → AuctionPost에 경매 정보 저장
    // =========================================================================
    @Transactional
    public Long createPost(Long userId, AuctionPostCreateRequest request) {
        validateCategoryId(request.getCategoryId());

        // 1) MarketPost 생성 (공통 정보)
        MarketPost marketPost = MarketPost.builder()
                .userId(userId)
                .categoryId(request.getCategoryId())
                .type(MarketPostType.AUCTION)
                .productCondition(parseProductCondition(request.getProductCondition()))
                .specialType(parseSpecialType(request.getSpecialType()))
                .title(request.getTitle())
                .content(request.getContent())
                .price(request.getStartPrice())             // 대표 가격 = 시작가
                .deliveryMethod(parseDeliveryMethod(request.getDeliveryMethod()))
                .purchaseSource(request.getPurchaseSource())
                .receiptMediaId(request.getReceiptMediaId())
                .build();

        MarketPost savedMarketPost = marketPostRepository.save(marketPost);

        // 2) AuctionPost 생성 (경매 전용 정보)
        AuctionPost auctionPost = AuctionPost.builder()
                .marketPost(savedMarketPost)
                .startPrice(request.getStartPrice())
                .buyNowPrice(request.getBuyNowPrice())
                .bidStep(request.getBidStep())
                .endsAt(request.getEndsAt())
                .build();

        auctionPostRepository.save(auctionPost);

        // 3) 이미지 & 태그 저장
        saveImages(savedMarketPost.getPostId(), request.getImageMediaIds());
        saveTags(savedMarketPost.getPostId(), request.getTagNames());

        return savedMarketPost.getPostId();
    }

    // =========================================================================
    // 2. 경매 목록 조회
    // =========================================================================
    public Map<String, Object> getPostList(String status, String sort, int page, int size) {

        Sort sortOption = switch (sort != null ? sort : "latest") {
            case "ending_soon" -> Sort.by("createdAt").ascending();  // endsAt으로 변경 필요시 커스텀 쿼리
            case "popular" -> Sort.by("viewCount").descending();
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sortOption);

        MarketPostStatus postStatus = (status != null) ?
                MarketPostStatus.valueOf(status) : MarketPostStatus.ACTIVE;

        Page<MarketPost> postPage = marketPostRepository.findByTypeAndStatus(
                MarketPostType.AUCTION, postStatus, pageable);

        List<AuctionPostListResponse> content = postPage.getContent().stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", postPage.getTotalElements());
        result.put("totalPages", postPage.getTotalPages());
        result.put("currentPage", postPage.getNumber());
        return result;
    }

    // =========================================================================
    // 3. 경매 상세 조회
    // =========================================================================
    @Transactional
    public AuctionPostDetailResponse getPostDetail(Long postId, Long currentUserId) {

        MarketPost marketPost = findMarketPostOrThrow(postId);
        AuctionPost auctionPost = findAuctionPostOrThrow(postId);

        marketPost.increaseViewCount();

        // 현재 최고 입찰가
        BigDecimal currentPrice = auctionBidRepository
                .findTopByPostIdOrderByBidAmountDesc(postId)
                .map(AuctionBid::getBidAmount)
                .orElse(null);

        // 이미지, 태그
        List<AuctionPostDetailResponse.ImageResponse> images = getImageResponses(postId);
        List<String> tags = getTagNames(postId);

        // 찜
        int favoriteCount = marketPostFavoriteRepository.countByPostId(postId);
        boolean isFavorite = currentUserId != null &&
                marketPostFavoriteRepository.existsByPostIdAndUserId(postId, currentUserId);

        // 내 글인지, 내가 입찰했는지
        boolean isMyPost = currentUserId != null && marketPost.getUserId().equals(currentUserId);
        boolean hasBid = currentUserId != null &&
                auctionBidRepository.existsByPostIdAndUserId(postId, currentUserId);

        // 판매자 정보
        AuctionPostDetailResponse.SellerResponse seller = getSellerInfo(marketPost.getUserId());

        // 최근 입찰 내역 5건
        List<AuctionPostDetailResponse.BidHistoryResponse> recentBids = getRecentBids(postId, 5);

        return AuctionPostDetailResponse.builder()
                .postId(postId)
                .title(marketPost.getTitle())
                .content(marketPost.getContent())
                .productCondition(toClientCondition(marketPost.getProductCondition()))
                .specialType(marketPost.getSpecialType().name())
                .startPrice(auctionPost.getStartPrice())
                .currentPrice(currentPrice)
                .buyNowPrice(auctionPost.getBuyNowPrice())
                .bidStep(auctionPost.getBidStep())
                .bidCount(auctionPost.getBidCount())
                .deliveryMethod(marketPost.getDeliveryMethod())
                .purchaseSource(marketPost.getPurchaseSource())
                .status(marketPost.getStatus().name())
                .endsAt(auctionPost.getEndsAt())
                .viewCount(marketPost.getViewCount())
                .favoriteCount(favoriteCount)
                .isFavorite(isFavorite)
                .isMyPost(isMyPost)
                .hasBid(hasBid)
                .createdAt(marketPost.getCreatedAt())
                .images(images)
                .tags(tags)
                .seller(seller)
                .recentBids(recentBids)
                .build();
    }

    // =========================================================================
    // 4. 경매 글 수정 (입찰이 없을 때만 가능)
    // =========================================================================
    @Transactional
    public void updatePost(Long postId, Long userId, AuctionPostUpdateRequest request) {

        MarketPost marketPost = findMarketPostOrThrow(postId);
        AuctionPost auctionPost = findAuctionPostOrThrow(postId);

        validateOwner(marketPost, userId);
        validateCategoryId(request.getCategoryId());

        // 입찰이 있으면 수정 불가
        if (auctionPost.getBidCount() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "입찰이 있는 경매는 수정할 수 없습니다.");
        }

        // MarketPost 수정
        marketPost.update(
                null,
                request.getTitle(),
                request.getContent(),
                request.getStartPrice(),
                parseProductCondition(request.getProductCondition()),
                parseSpecialType(request.getSpecialType()),
                request.getCategoryId(),
                parseDeliveryMethod(request.getDeliveryMethod()),
                request.getPurchaseSource(),
                request.getReceiptMediaId()
        );

        // AuctionPost 수정
        auctionPost.update(
                request.getStartPrice(),
                request.getBuyNowPrice(),
                request.getBidStep(),
                request.getEndsAt()
        );

        // 이미지 & 태그 교체
        if (request.getImageMediaIds() != null) {
            marketPostImageRepository.deleteByPostId(postId);
            saveImages(postId, request.getImageMediaIds());
        }
        if (request.getTagNames() != null) {
            marketPostTagRepository.deleteByPostId(postId);
            saveTags(postId, request.getTagNames());
        }
    }

    // =========================================================================
    // 5. 경매 글 삭제 (입찰이 없을 때만 가능)
    // =========================================================================
    @Transactional
    public void deletePost(Long postId, Long userId) {

        MarketPost marketPost = findMarketPostOrThrow(postId);
        AuctionPost auctionPost = findAuctionPostOrThrow(postId);

        validateOwner(marketPost, userId);

        if (auctionPost.getBidCount() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "입찰이 있는 경매는 삭제할 수 없습니다.");
        }

        marketPost.changeStatus(MarketPostStatus.DELETED);
    }

    // =========================================================================
    // 6. 입찰하기
    // =========================================================================
    // [검증 로직]
    // - 본인 경매에 입찰 불가
    // - 경매 종료 시간 지났으면 입찰 불가
    // - 입찰 금액이 현재 최고가 + bidStep 이상이어야 함
    // - 첫 입찰이면 시작가 이상이어야 함
    // =========================================================================
    @Transactional
    public BidResponse placeBid(Long postId, Long userId, BidRequest request) {

        // 🔒 비관적 락으로 AuctionPost 먼저 조회 — 락 획득 시점에 read view를 고정시켜 동시 입찰 직렬화
        AuctionPost auctionPost = auctionPostRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매 글이 없습니다."));

        MarketPost marketPost = findMarketPostOrThrow(postId);

        // 본인 경매에 입찰 불가
        if (marketPost.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 경매에는 입찰할 수 없습니다.");
        }

        // 경매 상태 확인
        if (marketPost.getStatus() != MarketPostStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "종료된 경매입니다.");
        }

        // 경매 시간 확인
        if (auctionPost.getEndsAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "경매 시간이 종료되었습니다.");
        }

        // 현재 최고 입찰가 확인 (락 보유 중이라 안전)
        Optional<AuctionBid> topBid = auctionBidRepository
                .findTopByPostIdOrderByBidAmountDesc(postId);

        BigDecimal minimumBid;
        if (topBid.isPresent()) {
            // 기존 입찰이 있으면: 최고가 + bidStep 이상
            minimumBid = topBid.get().getBidAmount().add(auctionPost.getBidStep());
        } else {
            // 첫 입찰이면: 시작가 이상
            minimumBid = auctionPost.getStartPrice();
        }

        if (request.getBidAmount().compareTo(minimumBid) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "입찰 금액은 " + minimumBid + "원 이상이어야 합니다.");
        }

        // 입찰 저장
        AuctionBid bid = AuctionBid.builder()
                .postId(postId)
                .userId(userId)
                .bidAmount(request.getBidAmount())
                .build();

        AuctionBid savedBid = auctionBidRepository.save(bid);

        // 입찰 수 원자적 증가 (UPDATE ... bid_count = bid_count + 1)
        auctionPostRepository.incrementBidCount(postId);

        return BidResponse.builder()
                .bidId(savedBid.getBidId())
                .postId(postId)
                .bidAmount(savedBid.getBidAmount())
                .bidCount(auctionPost.getBidCount() + 1)
                .bidAt(savedBid.getCreatedAt())
                .message("입찰이 완료되었습니다.")
                .build();
    }

    // =========================================================================
    // 7. 즉시구매
    // =========================================================================
    // - 즉시구매가가 설정되어 있어야 함
    // - 본인 경매에 불가
    // - 즉시구매 시 경매 종료 → 낙찰자 = 즉시구매자
    // =========================================================================
    @Transactional
    public BidResponse buyNow(Long postId, Long userId) {

        // 🔒 비관적 락 먼저 — 락 획득 시점에 read view를 고정시켜 즉시구매와 입찰 동시 충돌 방지
        AuctionPost auctionPost = auctionPostRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매 글이 없습니다."));

        MarketPost marketPost = findMarketPostOrThrow(postId);

        // 본인 경매 불가
        if (marketPost.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 경매는 즉시구매할 수 없습니다.");
        }

        // 상태 확인
        if (marketPost.getStatus() != MarketPostStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "종료된 경매입니다.");
        }

        // 경매 시간 확인
        if (auctionPost.getEndsAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "경매 시간이 종료되었습니다.");
        }

        // 즉시구매가 설정 확인
        if (auctionPost.getBuyNowPrice() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "즉시구매가가 설정되지 않은 경매입니다.");
        }

        // 즉시구매 입찰 저장
        AuctionBid bid = AuctionBid.builder()
                .postId(postId)
                .userId(userId)
                .bidAmount(auctionPost.getBuyNowPrice())
                .build();

        AuctionBid savedBid = auctionBidRepository.save(bid);

        // 입찰 수 원자적 증가
        auctionPostRepository.incrementBidCount(postId);

        // 경매 종료 처리
        auctionPost.endAuction(userId, auctionPost.getBuyNowPrice());
        marketPost.changeStatus(MarketPostStatus.SOLD);

        return BidResponse.builder()
                .bidId(savedBid.getBidId())
                .postId(postId)
                .bidAmount(auctionPost.getBuyNowPrice())
                .bidCount(auctionPost.getBidCount() + 1)
                .bidAt(savedBid.getCreatedAt())
                .message("즉시구매가 완료되었습니다.")
                .build();
    }



    public BidHistoryListResponse getBidHistoryList(Long postId) {

        // 삭제된 경매글은 입찰 내역 조회도 404 처리
        findMarketPostOrThrow(postId);
        AuctionPost auctionPost = findAuctionPostOrThrow(postId);

        // getCurrentHighestBid 대신 직접 조회
        BigDecimal currentHighest = auctionBidRepository
                .findTopByPostIdOrderByBidAmountDesc(postId)
                .map(AuctionBid::getBidAmount)
                .orElse(auctionPost.getStartPrice());

        // findByPostIdOrderByBidPriceDesc → 페이지 버전 사용
        Pageable allBids = PageRequest.of(0, 100, Sort.by("bidAmount").descending());
        List<AuctionBid> bids = auctionBidRepository.findByPostId(postId, allBids).getContent();

        List<BidHistoryListResponse.BidItem> bidItems = bids.stream()
                .map(bid -> {
                    User user = userRepository.findById(bid.getUserId()).orElse(null);
                    UserProfile profile = user != null ?
                            userProfileRepository.findByUserId(user.getUserId()).orElse(null) : null;

                    return BidHistoryListResponse.BidItem.builder()
                            .userId(bid.getUserId())
                            .nickname(user != null ? user.getNickname() : "알 수 없음")
                            .profileImageUrl(profile != null ? profile.getProfileImageUrl() : null)
                            .bidAmount(bid.getBidAmount())       // bidPrice → bidAmount
                            .createdAt(bid.getCreatedAt() != null ?
                                    bid.getCreatedAt().toString().replace("T", " ") : null)
                            .isHighest(bid.getBidAmount().compareTo(currentHighest) == 0)
                            .build();
                })
                .collect(Collectors.toList());

        return BidHistoryListResponse.builder()
                .postId(postId)
                .totalBidCount(auctionPost.getBidCount())
                .currentHighestBid(currentHighest)
                .startPrice(auctionPost.getStartPrice())
                .bidStep(auctionPost.getBidStep())
                .nextMinBid(currentHighest.add(auctionPost.getBidStep()))
                .bids(bidItems)
                .build();
    }

    // =========================================================================
// 9. 내 입찰 목록 조회
// =========================================================================
    public Map<String, Object> getMyBids(Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuctionBid> bidPage = auctionBidRepository.findByUserId(userId, pageable);

        List<BidResponse> content = bidPage.getContent().stream()
                .map(bid -> BidResponse.builder()
                        .bidId(bid.getBidId())
                        .postId(bid.getPostId())
                        .bidAmount(bid.getBidAmount())
                        .bidCount(0)  // 개별 입찰에서는 불필요
                        .bidAt(bid.getCreatedAt())
                        .message(null)
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", bidPage.getTotalElements());
        result.put("totalPages", bidPage.getTotalPages());
        result.put("currentPage", bidPage.getNumber());
        return result;
    }

    // =========================================================================
    // 10. 내 경매 목록 조회
    // =========================================================================
    // 기준: 내가 작성한 경매글 (market_posts.user_id = me)
    // 정렬: ACTIVE 우선 -> SOLD 다음 -> 그 외, 같은 그룹 내 createdAt DESC
    public Map<String, Object> getMyPosts(Long userId, int page, int size) {

        List<MarketPost> myAuctionPosts = marketPostRepository.findByTypeAndUserIdAndStatusNot(
                MarketPostType.AUCTION, userId, MarketPostStatus.DELETED);

        Comparator<MarketPost> statusPriorityComparator = Comparator
                .comparingInt((MarketPost post) -> statusPriority(post.getStatus()))
                .thenComparing(MarketPost::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

        List<MyAuctionResponse> sorted = myAuctionPosts.stream()
                .sorted(statusPriorityComparator)
                .map(this::toMyAuctionResponse)
                .collect(Collectors.toList());

        int totalElements = sorted.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        Map<String, Object> result = new HashMap<>();
        result.put("content", sorted.subList(fromIndex, toIndex));
        result.put("totalElements", totalElements);
        result.put("totalPages", size == 0 ? 0 : (int) Math.ceil((double) totalElements / size));
        result.put("currentPage", page);
        return result;
    }

    // =========================================================================
    // 11. 낙찰 성공 목록 조회
    // =========================================================================
    // 판단 기준:
    // - auction_posts.winner_user_id == 현재 사용자
    // - market_posts.status == SOLD 인 경매만 노출
    public Map<String, Object> getMyWonPosts(Long userId, int page, int size) {

        List<AuctionPost> wonAuctionPosts = auctionPostRepository.findByWinnerUserIdOrderByEndsAtDesc(userId);

        List<MyAuctionResponse> wonContent = wonAuctionPosts.stream()
                .map(auctionPost -> {
                    MarketPost marketPost = marketPostRepository.findById(auctionPost.getPostId()).orElse(null);
                    if (marketPost == null
                            || marketPost.getType() != MarketPostType.AUCTION
                            || marketPost.getStatus() != MarketPostStatus.SOLD) {
                        return null;
                    }
                    return toMyAuctionResponse(marketPost);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int totalElements = wonContent.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        Map<String, Object> result = new HashMap<>();
        result.put("content", wonContent.subList(fromIndex, toIndex));
        result.put("totalElements", totalElements);
        result.put("totalPages", size == 0 ? 0 : (int) Math.ceil((double) totalElements / size));
        result.put("currentPage", page);
        return result;
    }
    // =========================================================================
    // 12. 인기 경매 조회
    // =========================================================================
    public List<AuctionPostSimpleResponse> getPopularAuctions(int limit) {

        Pageable pageable = PageRequest.of(0, limit, Sort.by("viewCount").descending());
        Page<MarketPost> postPage = marketPostRepository.findByTypeAndStatus(
                MarketPostType.AUCTION, MarketPostStatus.ACTIVE, pageable);

        return postPage.getContent().stream()
                .map(this::toSimpleResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 13. 마감 임박 경매 조회 ("곧 끝나는 경매!")
    // =========================================================================
    // 현재 시간 이후에 종료되는 ACTIVE 경매를 endsAt 오름차순으로 정렬
    // =========================================================================
    public List<AuctionPostSimpleResponse> getEndingSoonAuctions(int limit) {

        List<AuctionPost> endingSoon = auctionPostRepository
                .findByEndsAtAfterOrderByEndsAtAsc(LocalDateTime.now(),
                        PageRequest.of(0, limit));

        return endingSoon.stream()
                .map(ap -> {
                    MarketPost mp = marketPostRepository.findById(ap.getPostId()).orElse(null);
                    if (mp == null
                            || mp.getType() != MarketPostType.AUCTION
                            || mp.getStatus() != MarketPostStatus.ACTIVE) {
                        return null;
                    }

                    BigDecimal currentPrice = auctionBidRepository
                            .findTopByPostIdOrderByBidAmountDesc(ap.getPostId())
                            .map(AuctionBid::getBidAmount)
                            .orElse(ap.getStartPrice());

                    return AuctionPostSimpleResponse.builder()
                            .postId(ap.getPostId())
                            .title(mp.getTitle())
                            .startPrice(ap.getStartPrice())
                            .currentPrice(currentPrice)
                            .bidCount(ap.getBidCount())
                            .thumbnailUrl(getThumbnailUrl(ap.getPostId()))
                            .endsAt(ap.getEndsAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 14. 경매 종료 처리 (스케줄러에서 호출)
    // =========================================================================
    // - 종료 시간이 지난 ACTIVE 경매를 찾아서 낙찰/유찰 처리
    // - 입찰이 있으면 → 최고 입찰자가 낙찰자 (SOLD)
    // - 입찰이 없으면 → 유찰 (CLOSED)
    // =========================================================================
    @Transactional
    public void processEndedAuctions() {

        // 종료 시간이 지난 ACTIVE 경매 목록 조회
        List<AuctionPost> endedAuctions = auctionPostRepository
                .findByEndsAtBeforeAndWinnerUserIdIsNull(LocalDateTime.now());

        for (AuctionPost auctionPost : endedAuctions) {
            MarketPost marketPost = marketPostRepository.findById(auctionPost.getPostId())
                    .orElse(null);

            if (marketPost == null || marketPost.getStatus() != MarketPostStatus.ACTIVE) {
                continue;
            }

            // 최고 입찰 확인
            Optional<AuctionBid> topBid = auctionBidRepository
                    .findTopByPostIdOrderByBidAmountDesc(auctionPost.getPostId());

            if (topBid.isPresent()) {
                // 낙찰 처리
                auctionPost.endAuction(topBid.get().getUserId(), topBid.get().getBidAmount());
                marketPost.changeStatus(MarketPostStatus.SOLD);
            } else {
                // 유찰 처리 (입찰 없음)
                marketPost.changeStatus(MarketPostStatus.CLOSED);
            }
        }
    }

    // =========================================================================
    // ===== 공통 헬퍼 메서드 =====
    // =========================================================================

    private MarketPost findMarketPostOrThrow(Long postId) {
        MarketPost post = marketPostRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (post.getStatus() == MarketPostStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }
        return post;
    }

    private AuctionPost findAuctionPostOrThrow(Long postId) {
        return auctionPostRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매 정보를 찾을 수 없습니다."));
    }

    private void validateOwner(MarketPost post, Long userId) {
        if (!post.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
    }

    private ProductCondition parseProductCondition(String productCondition) {
        return switch (productCondition) {
            case "미개봉", "UNOPENED", "NEW" -> ProductCondition.NEW;
            case "개봉", "OPENED", "LIKE_NEW", "USED", "JUNK" -> ProductCondition.USED;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 productCondition 값입니다.");
        };
    }

    private MarketSpecialType parseSpecialType(String specialType) {
        try {
            return MarketSpecialType.fromInput(specialType);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 specialType 값입니다.");
        }
    }

    private String parseDeliveryMethod(String deliveryMethod) {
        return DeliveryMethod.fromInput(deliveryMethod).getLabel();
    }

    private String toClientCondition(ProductCondition productCondition) {
        return productCondition == ProductCondition.NEW ? "미개봉" : "개봉";
    }

    private void saveImages(Long postId, List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) return;
        for (int i = 0; i < mediaIds.size(); i++) {
            marketPostImageRepository.save(MarketPostImage.builder()
                    .postId(postId)
                    .mediaId(mediaIds.get(i))
                    .sortOrder(i + 1)
                    .build());
        }
    }

    private void saveTags(Long postId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
            marketPostTagRepository.save(MarketPostTag.builder()
                    .postId(postId)
                    .tagId(tag.getTagId())
                    .build());
        }
    }

    private void validateCategoryId(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (!marketCategoryRepository.existsById(categoryId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 categoryId입니다.");
        }
    }

    private String getThumbnailUrl(Long postId) {
        return marketPostImageRepository.findFirstByPostIdOrderBySortOrder(postId)
                .flatMap(img -> mediaFileRepository.findById(img.getMediaId()))
                .map(MediaFile::getUrl)
                .orElse(null);
    }

    private List<AuctionPostDetailResponse.ImageResponse> getImageResponses(Long postId) {
        return marketPostImageRepository.findByPostIdOrderBySortOrder(postId).stream()
                .map(img -> {
                    String url = mediaFileRepository.findById(img.getMediaId())
                            .map(MediaFile::getUrl).orElse(null);
                    return AuctionPostDetailResponse.ImageResponse.builder()
                            .mediaId(img.getMediaId())
                            .imageUrl(url)
                            .sortOrder(img.getSortOrder())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<String> getTagNames(Long postId) {
        return marketPostTagRepository.findByPostId(postId).stream()
                .map(pt -> tagRepository.findById(pt.getTagId()).map(Tag::getName).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private AuctionPostDetailResponse.SellerResponse getSellerInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        return AuctionPostDetailResponse.SellerResponse.builder()
                .userId(userId)
                .nickname(user.getNickname())
                .profileImageUrl(profile != null ? profile.getProfileImageUrl() : null)
                .build();
    }

    private List<AuctionPostDetailResponse.BidHistoryResponse> getRecentBids(Long postId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return auctionBidRepository.findByPostId(postId, pageable).getContent().stream()
                .map(bid -> {
                    String nickname = userRepository.findById(bid.getUserId())
                            .map(User::getNickname).orElse("알 수 없음");
                    return AuctionPostDetailResponse.BidHistoryResponse.builder()
                            .bidderNickname(nickname)
                            .bidAmount(bid.getBidAmount())
                            .bidAt(bid.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private AuctionPostListResponse toListResponse(MarketPost post) {
        AuctionPost auctionPost = auctionPostRepository.findById(post.getPostId()).orElse(null);
        BigDecimal currentPrice = auctionBidRepository
                .findTopByPostIdOrderByBidAmountDesc(post.getPostId())
                .map(AuctionBid::getBidAmount).orElse(null);

        return AuctionPostListResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .specialType(post.getSpecialType().name())
                .startPrice(auctionPost != null ? auctionPost.getStartPrice() : BigDecimal.ZERO)
                .currentPrice(currentPrice)
                .buyNowPrice(auctionPost != null ? auctionPost.getBuyNowPrice() : null)
                .bidCount(auctionPost != null ? auctionPost.getBidCount() : 0)
                .thumbnailUrl(getThumbnailUrl(post.getPostId()))
                .endsAt(auctionPost != null ? auctionPost.getEndsAt() : null)
                .status(post.getStatus().name())
                .viewCount(post.getViewCount())
                .favoriteCount(marketPostFavoriteRepository.countByPostId(post.getPostId()))
                .build();
    }

    private AuctionPostSimpleResponse toSimpleResponse(MarketPost post) {
        AuctionPost auctionPost = auctionPostRepository.findById(post.getPostId()).orElse(null);
        BigDecimal currentPrice = auctionBidRepository
                .findTopByPostIdOrderByBidAmountDesc(post.getPostId())
                .map(AuctionBid::getBidAmount).orElse(null);

        return AuctionPostSimpleResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .specialType(post.getSpecialType().name())
                .startPrice(auctionPost != null ? auctionPost.getStartPrice() : BigDecimal.ZERO)
                .currentPrice(currentPrice)
                .bidCount(auctionPost != null ? auctionPost.getBidCount() : 0)
                .viewCount(post.getViewCount())
                .thumbnailUrl(getThumbnailUrl(post.getPostId()))
                .endsAt(auctionPost != null ? auctionPost.getEndsAt() : null)
                .build();
    }

    private MyAuctionResponse toMyAuctionResponse(MarketPost post) {
        AuctionPost auctionPost = auctionPostRepository.findById(post.getPostId()).orElse(null);
        BigDecimal currentHighestBid = auctionBidRepository
                .findTopByPostIdOrderByBidAmountDesc(post.getPostId())
                .map(AuctionBid::getBidAmount)
                .orElse(auctionPost != null ? auctionPost.getStartPrice() : post.getPrice());

        return MyAuctionResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .startPrice(auctionPost != null ? auctionPost.getStartPrice() : post.getPrice())
                .currentHighestBid(currentHighestBid)
                .buyNowPrice(auctionPost != null ? auctionPost.getBuyNowPrice() : null)
                .bidCount(auctionPost != null ? auctionPost.getBidCount() : 0)
                .myBidPrice(null)
                .finalPrice(auctionPost != null ? auctionPost.getFinalPrice() : null)
                .status(post.getStatus().name())
                .endsAt(auctionPost != null ? auctionPost.getEndsAt() : null)
                .thumbnailUrl(getThumbnailUrl(post.getPostId()))
                .build();
    }

    private int statusPriority(MarketPostStatus status) {
        if (status == MarketPostStatus.ACTIVE) return 0;
        if (status == MarketPostStatus.SOLD) return 1;
        return 2;
    }
}
