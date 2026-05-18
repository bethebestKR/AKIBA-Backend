package com.akiba.backend.board.dto;

import com.akiba.backend.board.domain.AuthenticityVoteChoice;
import com.akiba.backend.board.domain.BoardCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class BoardDtos {

    public record BoardSummaryResponse(
            Long boardId,
            BoardCode boardCode,
            String boardName,
            String description
    ) {
    }

    public record CreatePostRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            @Size(max = 10) List<@NotBlank String> imageUrls,
            @Size(max = 500) String saleOrAuctionLink,
            List<@NotBlank String> hashtags
    ) {
    }

    public record CreateCommentRequest(
            Long parentId,
            @NotBlank @Size(max = 1000) String content
    ) {
    }

    public record UpdatePostRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            @Size(max = 10) List<@NotBlank String> imageUrls,
            @Size(max = 500) String saleOrAuctionLink,
            List<@NotBlank String> hashtags
    ) {
    }

    public record CommentLikeToggleResponse(
            Long commentId,
            boolean liked,
            int likeCount
    ) {
    }

    public record VoteRequest(
            @NotNull AuthenticityVoteChoice choice
    ) {
    }

    public record CommentResponse(
            Long commentId,
            Long postId,
            Long userId,
            String author,
            Long parentId,
            String content,
            LocalDateTime createdAt,
            int likeCount,
            List<CommentResponse> replies
    ) {
    }

    public record PostSummaryResponse(
            Long postId,
            BoardCode boardCode,
            Long userId,
            String author,
            String title,
            String content,
            LocalDateTime createdAt,
            int likeCount,
            int commentCount,
            List<String> imageUrls,
            List<String> hashtags,
            String saleOrAuctionLink,
            int authenticVoteCount,
            int fakeVoteCount
    ) {
    }

    public record PostDetailResponse(
            Long postId,
            BoardCode boardCode,
            Long userId,
            String author,
            String title,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int likeCount,
            int commentCount,
            List<String> imageUrls,
            List<String> hashtags,
            String saleOrAuctionLink,
            int authenticVoteCount,
            int fakeVoteCount,
            List<CommentResponse> comments
    ) {
    }
}