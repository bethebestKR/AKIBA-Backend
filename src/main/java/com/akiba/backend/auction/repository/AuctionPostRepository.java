package com.akiba.backend.auction.repository;

import com.akiba.backend.auction.domain.AuctionPost;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionPostRepository extends JpaRepository<AuctionPost, Long> {
    List<AuctionPost> findByEndsAtAfterOrderByEndsAtAsc(LocalDateTime now, Pageable pageable);

    // 종료 시간 지났지만 아직 낙찰자 없는 경매 (스케줄러용)
    List<AuctionPost> findByEndsAtBeforeAndWinnerUserIdIsNull(LocalDateTime now);

    // 낙찰자 기준 조회
    List<AuctionPost> findByWinnerUserIdOrderByEndsAtDesc(Long winnerUserId);

    // ====================================================================
    // 비관적 락 — 입찰/즉시구매 시 동시성 보장
    // SELECT ... FOR UPDATE 발생 → 다른 트랜잭션이 같은 row 접근 시 대기
    // lock timeout 3초 — 그 안에 획득 못 하면 예외
    // ====================================================================
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT a FROM AuctionPost a WHERE a.postId = :postId")
    Optional<AuctionPost> findByIdForUpdate(@Param("postId") Long postId);

    // ====================================================================
    // 입찰 수 원자적 증가 (UPDATE ... SET bid_count = bid_count + 1)
    // 메모리 read-modify-write 패턴의 Lost Update 차단
    // ====================================================================
    @Modifying
    @Query("UPDATE AuctionPost a SET a.bidCount = a.bidCount + 1 WHERE a.postId = :postId")
    int incrementBidCount(@Param("postId") Long postId);
}