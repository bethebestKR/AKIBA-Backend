package com.akiba.backend.used.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import lombok.Getter;
import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.math.BigDecimal;

@Getter
@Builder
public class UsedPostDetailResponse {

    private Long postId;
    private String type;
    private String title;
    private String content;
    private BigDecimal price;
    private String productCondition;
    private String specialType;
    private String status;
    private String deliveryMethod;
    private String purchaseSource;
    private Long receiptMediaId;
    private int viewCount;
    private int favoriteCount;
    private boolean isFavorite;
    private LocalDateTime createdAt;
    private List<ImageResponse> images;
    private List<String> tags;
    private SellerResponse seller;

    @Getter
    @Builder
    public static class ImageResponse {
        private Long mediaId;
        private String imageUrl;
        private int sortOrder;
    }

    @Getter
    @Builder
    public static class SellerResponse {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private String bio;
        private int dealCount;
        private int reviewCount;
    }
}
