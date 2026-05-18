package com.akiba.backend.auction;

import com.akiba.backend.auction.domain.AuctionPost;
import com.akiba.backend.auction.dto.request.BidRequest;
import com.akiba.backend.auction.repository.AuctionBidRepository;
import com.akiba.backend.auction.repository.AuctionPostRepository;
import com.akiba.backend.auction.service.AuctionPostService;
import com.akiba.backend.used.domain.MarketPost;
import com.akiba.backend.used.domain.MarketPostStatus;
import com.akiba.backend.used.domain.MarketPostType;
import com.akiba.backend.used.domain.MarketSpecialType;
import com.akiba.backend.used.domain.ProductCondition;
import com.akiba.backend.used.repository.MarketPostRepository;
import com.akiba.backend.user.domain.AuthProvider;
import com.akiba.backend.user.domain.User;
import com.akiba.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 입찰 동시성 테스트
 *
 * - Day 6 의 비관적 락이 실제로 Lost Update 를 막는지 검증
 * - 100명이 동시에 같은 금액으로 입찰 → 정확히 1명만 성공해야 정상
 * - 락 없으면 여러 명이 같은 금액 입찰에 성공함
 *
 * 실행 방법:
 *   1. 로컬 MySQL 실행 중일 것 (application-local.yml 의 DB 사용)
 *   2. profile = local 로 실행
 *      ./gradlew test --tests "*ConcurrencyTest*" -Dspring.profiles.active=local
 */
@SpringBootTest
@ActiveProfiles("local")
@DisplayName("입찰 동시성 테스트")
class AuctionPostServiceConcurrencyTest {

    @Autowired private AuctionPostService auctionPostService;
    @Autowired private AuctionPostRepository auctionPostRepository;
    @Autowired private AuctionBidRepository auctionBidRepository;
    @Autowired private MarketPostRepository marketPostRepository;
    @Autowired private UserRepository userRepository;
    @PersistenceContext private EntityManager em;
    @Autowired private PlatformTransactionManager txManager;

    private Long testAuctionPostId;
    private List<Long> testUserIds;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.execute(status -> {
            // 1) 판매자 1명 + 입찰자 100명 생성
            User seller = userRepository.save(buildUser("seller_" + UUID.randomUUID()));

            testUserIds = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                User bidder = userRepository.save(buildUser("bidder_" + i + "_" + UUID.randomUUID()));
                testUserIds.add(bidder.getUserId());
            }

            // 2) MarketPost + AuctionPost — @MapsId 패턴은 EntityManager 로 직접 관리
            MarketPost marketPost = MarketPost.builder()
                    .userId(seller.getUserId())
                    .type(MarketPostType.AUCTION)
                    .title("동시성 테스트 경매")
                    .content("Test")
                    .price(BigDecimal.valueOf(10000))
                    .productCondition(ProductCondition.NEW)
                    .specialType(MarketSpecialType.NONE)
                    .deliveryMethod("택배")
                    .purchaseSource("test")
                    .status(MarketPostStatus.ACTIVE)
                    .build();
            em.persist(marketPost);
            em.flush();

            AuctionPost auctionPost = AuctionPost.builder()
                    .marketPost(marketPost)
                    .startPrice(BigDecimal.valueOf(10000))
                    .bidStep(BigDecimal.valueOf(1000))
                    .endsAt(LocalDateTime.now().plusDays(1))
                    .bidCount(0)
                    .build();
            em.persist(auctionPost);
            em.flush();

            this.testAuctionPostId = auctionPost.getPostId();
            return null;
        });
    }

    @AfterEach
    void tearDown() {
        if (testAuctionPostId != null) {
            auctionBidRepository.deleteAll(auctionBidRepository.findAll());
            auctionPostRepository.deleteById(testAuctionPostId);
            marketPostRepository.deleteById(testAuctionPostId);
        }
        if (testUserIds != null) {
            userRepository.deleteAllById(testUserIds);
        }
    }

    @Test
    @DisplayName("100명이 동시에 같은 금액 입찰 시 정확히 1명만 성공해야 한다")
    void concurrentBids_onlyOneShouldSucceed() throws Exception {
        // given
        int threadCount = 100;
        BigDecimal sameAmount = BigDecimal.valueOf(11000);

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when — 모두 같은 시점에 입찰 시도
        for (int i = 0; i < threadCount; i++) {
            final Long bidderId = testUserIds.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    BidRequest req = new BidRequest();
                    setBidAmount(req, sameAmount);
                    auctionPostService.placeBid(testAuctionPostId, bidderId, req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();  // 동시 시작
        doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        long actualBidCountInDb = auctionBidRepository.findAll().stream()
                .filter(b -> b.getPostId().equals(testAuctionPostId))
                .filter(b -> b.getBidAmount().compareTo(sameAmount) == 0)
                .count();

        AuctionPost result = auctionPostRepository.findById(testAuctionPostId).orElseThrow();

        System.out.println("========================================");
        System.out.println("성공:  " + successCount.get());
        System.out.println("실패:  " + failCount.get());
        System.out.println("DB 입찰 건수: " + actualBidCountInDb);
        System.out.println("post.bidCount: " + result.getBidCount());
        System.out.println("========================================");

        // 핵심 검증 — 동일 금액 11000 입찰은 정확히 1건만 성공
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(actualBidCountInDb).isEqualTo(1);
        assertThat(result.getBidCount()).isEqualTo(1);
    }

    // ---- helpers ----

    private User buildUser(String uid) {
        return User.builder()
                .provider(AuthProvider.NAVER)
                .oauthId(uid)
                .email(uid + "@test.com")
                .nickname(uid.length() > 20 ? uid.substring(0, 20) : uid)
                .build();
    }

    private void setBidAmount(BidRequest req, BigDecimal amount) throws Exception {
        Field f = BidRequest.class.getDeclaredField("bidAmount");
        f.setAccessible(true);
        f.set(req, amount);
    }
}