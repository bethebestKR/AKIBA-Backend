package com.akiba.backend.board.service;

import com.akiba.backend.board.domain.*;
import com.akiba.backend.board.dto.BoardDtos;
import com.akiba.backend.board.repository.*;
import com.akiba.backend.user.domain.User;
import com.akiba.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardPostRepository boardPostRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final BoardPostLikeRepository boardPostLikeRepository;
    private final BoardPostImageRepository boardPostImageRepository;
    private final BoardPostVoteRepository boardPostVoteRepository;
    private final BoardCommentLikeRepository boardCommentLikeRepository;
    private final BoardTagRepository boardTagRepository;
    private final BoardPostTagRepository boardPostTagRepository;
    private final UserRepository userRepository;

    public List<BoardDtos.BoardSummaryResponse> listBoards() {
        return boardRepository.findAll().stream()
                .sorted(Comparator.comparing(Board::getBoardId))
                .map(board -> new BoardDtos.BoardSummaryResponse(
                        board.getBoardId(),
                        board.getCode(),
                        board.getName(),
                        board.getDescription()
                ))
                .toList();
    }

    public List<BoardDtos.PostSummaryResponse> listPosts(BoardCode boardCode) {
        Board board = getBoard(boardCode);
        List<BoardPost> posts = boardPostRepository.findByBoardIdOrderByCreatedAtDesc(board.getBoardId());

        Map<Long, List<String>> imageMap = buildImageMap(posts);
        Map<Long, List<String>> hashtagMap = buildHashtagMap(posts);
        Map<Long, String> authorMap = buildAuthorMap(posts.stream().map(BoardPost::getUserId).toList());
        Map<Long, BoardCode> boardCodeMap = Map.of(board.getBoardId(), boardCode);

        return toPostSummaryResponses(posts, imageMap, hashtagMap, authorMap, boardCodeMap);
    }

    public List<BoardDtos.PostSummaryResponse> listPopularPosts() {
        List<BoardPost> posts = boardPostRepository.findTop10ByOrderByLikeCountDescCreatedAtDesc();
        return toPostSummaryResponses(
                posts,
                buildImageMap(posts),
                buildHashtagMap(posts),
                buildAuthorMap(posts.stream().map(BoardPost::getUserId).toList()),
                buildBoardCodeMap(posts)
        );
    }

    public List<BoardDtos.PostSummaryResponse> searchPosts(String keyword) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어를 입력해주세요.");
        }

        List<BoardPost> posts = boardPostRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(trimmedKeyword, trimmedKeyword);
        return toPostSummaryResponses(
                posts,
                buildImageMap(posts),
                buildHashtagMap(posts),
                buildAuthorMap(posts.stream().map(BoardPost::getUserId).toList()),
                buildBoardCodeMap(posts)
        );
    }

    public List<BoardDtos.PostSummaryResponse> listPostsByHashtag(String hashtag) {
        String normalizedHashtag = normalizeHashtags(List.of(hashtag)).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "해시태그를 입력해주세요."));

        BoardTag tag = boardTagRepository.findByNameIgnoreCase(normalizedHashtag)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해시태그를 찾을 수 없습니다."));

        List<Long> postIds = boardPostTagRepository.findByTagId(tag.getTagId()).stream()
                .map(BoardPostTag::getPostId)
                .distinct()
                .toList();
        if (postIds.isEmpty()) {
            return List.of();
        }

        List<BoardPost> posts = boardPostRepository.findAllById(postIds).stream()
                .sorted(Comparator.comparing(BoardPost::getCreatedAt).reversed())
                .toList();

        return toPostSummaryResponses(
                posts,
                buildImageMap(posts),
                buildHashtagMap(posts),
                buildAuthorMap(posts.stream().map(BoardPost::getUserId).toList()),
                buildBoardCodeMap(posts)
        );
    }

    @Transactional
    public BoardDtos.PostDetailResponse createPost(BoardCode boardCode, Long userId, BoardDtos.CreatePostRequest request) {
        Board board = getBoard(boardCode);
        User user = getUser(userId);
        List<String> imageUrls = normalizeImages(request.imageUrls());
        List<String> hashtags = normalizeHashtags(request.hashtags());

        if (boardCode == BoardCode.AUTHENTICITY && imageUrls.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정품감정 게시판은 이미지가 필수입니다.");
        }
        if (imageUrls.size() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 최대 10장까지 업로드할 수 있습니다.");
        }

        BoardPost post = boardPostRepository.save(BoardPost.builder()
                .boardId(board.getBoardId())
                .userId(user.getUserId())
                .title(request.title())
                .content(request.content())
                .saleOrAuctionLink(request.saleOrAuctionLink())
                .build());

        persistImages(post.getPostId(), imageUrls);
        persistHashtags(post.getPostId(), hashtags);
        return toPostDetail(boardCode, post, user.getNickname());
    }

    public BoardDtos.PostDetailResponse getPost(BoardCode boardCode, Long postId) {
        Board board = getBoard(boardCode);
        BoardPost post = getPost(board.getBoardId(), postId);
        User user = getUser(post.getUserId());
        return toPostDetail(boardCode, post, user.getNickname());
    }

    @Transactional
    public BoardDtos.PostDetailResponse updatePost(BoardCode boardCode, Long postId, Long userId, BoardDtos.UpdatePostRequest request) {
        Board board = getBoard(boardCode);
        BoardPost post = getPost(board.getBoardId(), postId);
        validatePostOwner(post, userId);

        List<String> imageUrls = normalizeImages(request.imageUrls());
        List<String> hashtags = normalizeHashtags(request.hashtags());
        if (boardCode == BoardCode.AUTHENTICITY && imageUrls.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정품감정 게시판은 이미지가 필수입니다.");
        }
        if (imageUrls.size() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 최대 10장까지 업로드할 수 있습니다.");
        }

        post.updatePost(request.title(), request.content(), request.saleOrAuctionLink());
        boardPostRepository.save(post);

        boardPostImageRepository.deleteByPostId(post.getPostId());
        persistImages(post.getPostId(), imageUrls);
        boardPostTagRepository.deleteByPostId(post.getPostId());
        persistHashtags(post.getPostId(), hashtags);
        return toPostDetail(boardCode, post, getUser(post.getUserId()).getNickname());
    }

    @Transactional
    public void deletePost(BoardCode boardCode, Long postId, Long userId) {
        Board board = getBoard(boardCode);
        BoardPost post = getPost(board.getBoardId(), postId);
        validatePostOwner(post, userId);

        boardPostVoteRepository.deleteByPostId(post.getPostId());
        boardPostLikeRepository.deleteByPostId(post.getPostId());
        boardCommentRepository.deleteByPostId(post.getPostId());
        boardPostImageRepository.deleteByPostId(post.getPostId());
        boardPostTagRepository.deleteByPostId(post.getPostId());
        boardPostRepository.delete(post);
    }

    @Transactional
    public BoardDtos.CommentResponse createComment(BoardCode boardCode, Long postId, Long userId, BoardDtos.CreateCommentRequest request) {
        Board board = getBoard(boardCode);
        BoardPost post = getPost(board.getBoardId(), postId);
        User user = getUser(userId);
        validateParentCommentForReply(post.getPostId(), request.parentId());

        BoardComment comment = boardCommentRepository.save(BoardComment.builder()
                .postId(post.getPostId())
                .userId(user.getUserId())
                .parentId(request.parentId())
                .content(request.content())
                .build());

        post.increaseCommentCount();
        boardPostRepository.save(post);

        return new BoardDtos.CommentResponse(
                comment.getCommentId(),
                comment.getPostId(),
                comment.getUserId(),
                user.getNickname(),
                comment.getParentId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getLikeCount(),
                List.of()
        );
    }

    public List<BoardDtos.CommentResponse> listComments(BoardCode boardCode, Long postId) {
        Board board = getBoard(boardCode);
        BoardPost post = getPost(board.getBoardId(), postId);
        return toCommentResponses(post.getPostId());
    }

    @Transactional
    public void deleteComment(BoardCode boardCode, Long commentId, Long userId) {
        Board board = getBoard(boardCode);
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "댓글 작성자만 삭제할 수 있습니다.");
        }

        BoardPost post = getPost(board.getBoardId(), comment.getPostId());
        List<Long> deleteIds = collectDeleteCommentIds(post.getPostId(), comment.getCommentId());
        boardCommentLikeRepository.deleteByCommentIdIn(deleteIds);
        boardCommentRepository.deleteAllById(deleteIds);
        for (int i = 0; i < deleteIds.size(); i++) {
            post.decreaseCommentCount();
        }
        boardPostRepository.save(post);
    }

    @Transactional
    public BoardDtos.PostDetailResponse toggleLike(BoardCode boardCode, Long postId, Long userId) {
        Board board = getBoard(boardCode);
        BoardPost post = getPost(board.getBoardId(), postId);
        getUser(userId);

        Optional<BoardPostLike> likeOpt = boardPostLikeRepository.findByPostIdAndUserId(post.getPostId(), userId);
        if (likeOpt.isPresent()) {
            boardPostLikeRepository.delete(likeOpt.get());
            post.decreaseLikeCount();
        } else {
            boardPostLikeRepository.save(BoardPostLike.builder()
                    .postId(post.getPostId())
                    .userId(userId)
                    .build());
            post.increaseLikeCount();
        }
        boardPostRepository.save(post);

        return toPostDetail(boardCode, post, getUser(post.getUserId()).getNickname());
    }

    @Transactional
    public BoardDtos.CommentLikeToggleResponse toggleCommentLike(BoardCode boardCode, Long commentId, Long userId) {
        Board board = getBoard(boardCode);
        BoardComment comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        getPost(board.getBoardId(), comment.getPostId());
        getUser(userId);

        Optional<BoardCommentLike> likeOpt = boardCommentLikeRepository.findByCommentIdAndUserId(comment.getCommentId(), userId);
        boolean liked;
        if (likeOpt.isPresent()) {
            boardCommentLikeRepository.delete(likeOpt.get());
            comment.decreaseLikeCount();
            liked = false;
        } else {
            boardCommentLikeRepository.save(BoardCommentLike.builder()
                    .commentId(comment.getCommentId())
                    .userId(userId)
                    .build());
            comment.increaseLikeCount();
            liked = true;
        }

        boardCommentRepository.save(comment);
        return new BoardDtos.CommentLikeToggleResponse(comment.getCommentId(), liked, comment.getLikeCount());
    }

    @Transactional
    public BoardDtos.PostDetailResponse vote(BoardCode boardCode, Long postId, Long userId, BoardDtos.VoteRequest request) {
        if (boardCode != BoardCode.AUTHENTICITY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정품감정 게시판에서만 투표할 수 있습니다.");
        }

        Board board = getBoard(boardCode);
        BoardPost post = getPost(board.getBoardId(), postId);
        getUser(userId);

        Optional<BoardPostVote> existingVoteOpt = boardPostVoteRepository.findByPostIdAndUserId(post.getPostId(), userId);

        if (existingVoteOpt.isPresent()) {
            BoardPostVote existingVote = existingVoteOpt.get();
            if (existingVote.getChoice() != request.choice()) {
                post.decreaseVoteCount(existingVote.getChoice());
                existingVote.changeChoice(request.choice());
                boardPostVoteRepository.save(existingVote);
                post.increaseVoteCount(request.choice());
                boardPostRepository.save(post);
            }
        } else {
            boardPostVoteRepository.save(BoardPostVote.builder()
                    .postId(post.getPostId())
                    .userId(userId)
                    .choice(request.choice())
                    .build());
            post.increaseVoteCount(request.choice());
            boardPostRepository.save(post);
        }

        return toPostDetail(boardCode, post, getUser(post.getUserId()).getNickname());
    }

    private Board getBoard(BoardCode boardCode) {
        return boardRepository.findByCode(boardCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다."));
    }

    private BoardPost getPost(Long boardId, Long postId) {
        return boardPostRepository.findByPostIdAndBoardId(postId, boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "작성자를 찾을 수 없습니다."));
    }

    private void validatePostOwner(BoardPost post, Long userId) {
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 수정/삭제할 수 있습니다.");
        }
    }

    private void validateParentCommentForReply(Long postId, Long parentId) {
        if (parentId == null) {
            return;
        }

        BoardComment parent = boardCommentRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."));
        if (!Objects.equals(parent.getPostId(), postId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 게시글의 댓글에만 답글을 작성할 수 있습니다.");
        }
        if (parent.getParentId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대댓글에는 추가 답글을 작성할 수 없습니다.");
        }
    }

    private List<String> normalizeImages(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private List<String> normalizeHashtags(List<String> hashtags) {
        if (hashtags == null) {
            return List.of();
        }
        return hashtags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(tag -> tag.startsWith("#") ? tag.substring(1) : tag)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(tag -> tag.length() > 50 ? tag.substring(0, 50) : tag)
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    private void persistImages(Long postId, List<String> imageUrls) {
        for (int i = 0; i < imageUrls.size(); i++) {
            boardPostImageRepository.save(BoardPostImage.builder()
                    .postId(postId)
                    .imageUrl(imageUrls.get(i))
                    .sortOrder(i)
                    .build());
        }
    }

    private void persistHashtags(Long postId, List<String> hashtags) {
        for (String hashtag : hashtags) {
            BoardTag tag = boardTagRepository.findByNameIgnoreCase(hashtag)
                    .orElseGet(() -> boardTagRepository.save(BoardTag.builder().name(hashtag).build()));
            boardPostTagRepository.save(BoardPostTag.builder()
                    .postId(postId)
                    .tagId(tag.getTagId())
                    .build());
        }
    }

    private BoardDtos.PostDetailResponse toPostDetail(BoardCode boardCode, BoardPost post, String author) {
        List<BoardDtos.CommentResponse> comments = toCommentResponses(post.getPostId());

        List<String> imageUrls = boardPostImageRepository.findByPostIdOrderBySortOrderAscCreatedAtAsc(post.getPostId()).stream()
                .map(BoardPostImage::getImageUrl)
                .toList();
        List<String> hashtags = buildHashtagMap(List.of(post)).getOrDefault(post.getPostId(), List.of());

        return new BoardDtos.PostDetailResponse(
                post.getPostId(),
                boardCode,
                post.getUserId(),
                author,
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getLikeCount(),
                post.getCommentCount(),
                imageUrls,
                hashtags,
                post.getSaleOrAuctionLink(),
                post.getAuthenticVoteCount(),
                post.getFakeVoteCount(),
                comments
        );
    }

    private List<BoardDtos.CommentResponse> toCommentResponses(Long postId) {
        List<BoardComment> commentEntities = boardCommentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        Map<Long, String> commentAuthorMap = buildAuthorMap(commentEntities.stream().map(BoardComment::getUserId).toList());
        List<BoardComment> roots = commentEntities.stream()
                .filter(comment -> comment.getParentId() == null)
                .toList();
        Map<Long, List<BoardComment>> byParent = commentEntities.stream()
                .filter(comment -> comment.getParentId() != null)
                .collect(Collectors.groupingBy(BoardComment::getParentId, LinkedHashMap::new, Collectors.toList()));

        return buildCommentTree(roots, byParent, commentAuthorMap);
    }

    private List<BoardDtos.CommentResponse> buildCommentTree(
            List<BoardComment> roots,
            Map<Long, List<BoardComment>> byParent,
            Map<Long, String> commentAuthorMap
    ) {
        return roots.stream()
                .map(comment -> new BoardDtos.CommentResponse(
                        comment.getCommentId(),
                        comment.getPostId(),
                        comment.getUserId(),
                        commentAuthorMap.getOrDefault(comment.getUserId(), "unknown"),
                        comment.getParentId(),
                        comment.getContent(),
                        comment.getCreatedAt(),
                        comment.getLikeCount(),
                        buildCommentTree(byParent.getOrDefault(comment.getCommentId(), List.of()), byParent, commentAuthorMap)
                ))
                .toList();
    }

    private List<Long> collectDeleteCommentIds(Long postId, Long rootCommentId) {
        List<BoardComment> commentEntities = boardCommentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        Map<Long, List<Long>> childrenMap = commentEntities.stream()
                .filter(comment -> comment.getParentId() != null)
                .collect(Collectors.groupingBy(
                        BoardComment::getParentId,
                        Collectors.mapping(BoardComment::getCommentId, Collectors.toList())
                ));

        List<Long> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootCommentId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            result.add(current);
            for (Long childId : childrenMap.getOrDefault(current, List.of())) {
                queue.add(childId);
            }
        }
        return result;
    }

    private Map<Long, List<String>> buildImageMap(List<BoardPost> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }

        List<Long> postIds = posts.stream().map(BoardPost::getPostId).toList();
        return boardPostImageRepository.findByPostIdInOrderByPostIdAscSortOrderAsc(postIds).stream()
                .collect(Collectors.groupingBy(
                        BoardPostImage::getPostId,
                        Collectors.mapping(BoardPostImage::getImageUrl, Collectors.toList())
                ));
    }

    private Map<Long, String> buildAuthorMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(new HashSet<>(userIds)).stream()
                .collect(Collectors.toMap(User::getUserId, User::getNickname, (left, right) -> left));
    }

    private Map<Long, List<String>> buildHashtagMap(List<BoardPost> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }

        List<Long> postIds = posts.stream().map(BoardPost::getPostId).toList();
        List<BoardPostTag> postTags = boardPostTagRepository.findByPostIdIn(postIds);
        if (postTags.isEmpty()) {
            return Map.of();
        }

        Set<Long> tagIds = postTags.stream().map(BoardPostTag::getTagId).collect(Collectors.toSet());
        Map<Long, String> tagNameMap = boardTagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(BoardTag::getTagId, BoardTag::getName, (left, right) -> left));

        return postTags.stream()
                .collect(Collectors.groupingBy(
                        BoardPostTag::getPostId,
                        Collectors.mapping(postTag -> "#" + tagNameMap.getOrDefault(postTag.getTagId(), ""), Collectors.toList())
                ));
    }

    private Map<Long, BoardCode> buildBoardCodeMap(List<BoardPost> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }

        Set<Long> boardIds = posts.stream().map(BoardPost::getBoardId).collect(Collectors.toSet());
        return boardRepository.findAllById(boardIds).stream()
                .filter(board -> board.getCode() != null)
                .collect(Collectors.toMap(Board::getBoardId, Board::getCode, (left, right) -> left));
    }

    private List<BoardDtos.PostSummaryResponse> toPostSummaryResponses(
            List<BoardPost> posts,
            Map<Long, List<String>> imageMap,
            Map<Long, List<String>> hashtagMap,
            Map<Long, String> authorMap,
            Map<Long, BoardCode> boardCodeMap
    ) {
        return posts.stream()
                .map(post -> new BoardDtos.PostSummaryResponse(
                        post.getPostId(),
                        boardCodeMap.get(post.getBoardId()),
                        post.getUserId(),
                        authorMap.getOrDefault(post.getUserId(), "unknown"),
                        post.getTitle(),
                        post.getContent(),
                        post.getCreatedAt(),
                        post.getLikeCount(),
                        post.getCommentCount(),
                        imageMap.getOrDefault(post.getPostId(), List.of()),
                        hashtagMap.getOrDefault(post.getPostId(), List.of()),
                        post.getSaleOrAuctionLink(),
                        post.getAuthenticVoteCount(),
                        post.getFakeVoteCount()
                ))
                .toList();
    }
}
