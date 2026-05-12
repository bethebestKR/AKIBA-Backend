package com.akiba.backend.used.domain;

import com.akiba.backend.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarketPost {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_market_posts_user")
    )
    private User user;

    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketPostType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCondition productCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketSpecialType specialType;

    @Column(length = 20)
    private String deliveryMethod;      // 거래 방식 (택배/직거래)

    @Column(length = 200)
    private String purchaseSource;      // 구매처

    private Long receiptMediaId;        // 영수증 이미지 (선택)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketPostStatus status;

    @Column(nullable = false)
    private int viewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.status = MarketPostStatus.ACTIVE;
        this.viewCount = 0;
        if (this.specialType == null) {
            this.specialType = MarketSpecialType.NONE;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 조회수 증가
    public void increaseViewCount() {
        this.viewCount++;
    }

    // 상태 변경 (판매중 → 예약중 → 판매완료 등)
    public void changeStatus(MarketPostStatus status) {
        this.status = status;
    }

    // 게시글 수정
    public void update(MarketPostType type, String title, String content, BigDecimal price,
                       ProductCondition productCondition, MarketSpecialType specialType, Long categoryId,
                       String deliveryMethod, String purchaseSource,
                       Long receiptMediaId) {
        if (type != null) this.type = type;
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (price != null) this.price = price;
        if (productCondition != null) this.productCondition = productCondition;
        if (specialType != null) this.specialType = specialType;
        if (categoryId != null) this.categoryId = categoryId;
        if (deliveryMethod != null) this.deliveryMethod = deliveryMethod;
        if (purchaseSource != null) this.purchaseSource = purchaseSource;
        if (receiptMediaId != null) this.receiptMediaId = receiptMediaId;
    }

}
