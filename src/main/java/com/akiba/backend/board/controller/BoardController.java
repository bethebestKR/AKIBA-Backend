package com.akiba.backend.board.controller;

import com.akiba.backend.board.domain.BoardCode;
import com.akiba.backend.board.dto.BoardDtos;
import com.akiba.backend.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public List<BoardDtos.BoardSummaryResponse> listBoards() {
        return boardService.listBoards();
    }

    @GetMapping("/{boardCode}/posts")
    public List<BoardDtos.PostSummaryResponse> listPosts(@PathVariable BoardCode boardCode) {
        return boardService.listPosts(boardCode);
    }

    @GetMapping("/popular/posts")
    public List<BoardDtos.PostSummaryResponse> listPopularPosts() {
        return boardService.listPopularPosts();
    }

    @GetMapping("/search")
    public List<BoardDtos.PostSummaryResponse> searchPosts(@RequestParam String keyword) {
        return boardService.searchPosts(keyword);
    }

    @GetMapping("/hashtags/{hashtag}/posts")
    public List<BoardDtos.PostSummaryResponse> listPostsByHashtag(@PathVariable String hashtag) {
        return boardService.listPostsByHashtag(hashtag);
    }

    @PostMapping("/{boardCode}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public BoardDtos.PostDetailResponse createPost(
            @PathVariable BoardCode boardCode,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid BoardDtos.CreatePostRequest request
    ) {
        return boardService.createPost(boardCode, requireUserId(userId), request);
    }

    @GetMapping("/{boardCode}/posts/{postId}")
    public BoardDtos.PostDetailResponse getPost(
            @PathVariable BoardCode boardCode,
            @PathVariable Long postId
    ) {
        return boardService.getPost(boardCode, postId);
    }

    @PutMapping("/{boardCode}/posts/{postId}")
    public BoardDtos.PostDetailResponse updatePost(
            @PathVariable BoardCode boardCode,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid BoardDtos.UpdatePostRequest request
    ) {
        return boardService.updatePost(boardCode, postId, requireUserId(userId), request);
    }

    @DeleteMapping("/{boardCode}/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @PathVariable BoardCode boardCode,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ) {
        boardService.deletePost(boardCode, postId, requireUserId(userId));
    }

    @PostMapping("/{boardCode}/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public BoardDtos.CommentResponse createComment(
            @PathVariable BoardCode boardCode,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid BoardDtos.CreateCommentRequest request
    ) {
        return boardService.createComment(boardCode, postId, requireUserId(userId), request);
    }

    @GetMapping("/{boardCode}/posts/{postId}/comments")
    public List<BoardDtos.CommentResponse> listComments(
            @PathVariable BoardCode boardCode,
            @PathVariable Long postId
    ) {
        return boardService.listComments(boardCode, postId);
    }

    @DeleteMapping("/{boardCode}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable BoardCode boardCode,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId
    ) {
        boardService.deleteComment(boardCode, commentId, requireUserId(userId));
    }

    @PostMapping("/{boardCode}/posts/{postId}/like")
    public BoardDtos.PostDetailResponse toggleLike(
            @PathVariable BoardCode boardCode,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId
    ) {
        return boardService.toggleLike(boardCode, postId, requireUserId(userId));
    }

    @PostMapping("/{boardCode}/comments/{commentId}/like")
    public BoardDtos.CommentLikeToggleResponse toggleCommentLike(
            @PathVariable BoardCode boardCode,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId
    ) {
        return boardService.toggleCommentLike(boardCode, commentId, requireUserId(userId));
    }

    @PostMapping("/{boardCode}/posts/{postId}/votes")
    public BoardDtos.PostDetailResponse vote(
            @PathVariable BoardCode boardCode,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid BoardDtos.VoteRequest request
    ) {
        return boardService.vote(boardCode, postId, requireUserId(userId), request);
    }

    private Long requireUserId(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userId;
    }
}