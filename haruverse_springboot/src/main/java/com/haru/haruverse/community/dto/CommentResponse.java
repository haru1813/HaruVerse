package com.haru.haruverse.community.dto;

import com.haru.haruverse.community.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 한 건과 그에 달린 답글들.
 *
 * <p><b>★평면 목록이 아니라 중첩 구조로 내보낸다★</b>
 * {@code parentId} 만 주고 클라이언트가 엮게 할 수도 있지만, 그러면 화면마다
 * 같은 트리 구성 코드를 다시 쓰게 된다. 깊이가 1단계로 고정이라
 * 중첩이 깊어질 걱정도 없으므로 서버가 엮어서 준다.
 *
 * <p>답글의 {@code replies} 는 언제나 비어 있다 — 답글에는 답글을 달 수 없다.
 */
public record CommentResponse(
        Long id,
        String content,
        String authorNickname,
        Long authorId,
        boolean mine,
        LocalDateTime createdAt,
        List<CommentResponse> replies
) {
    /** 답글이 없는 한 건 (답글 자신이거나, 아직 엮기 전) */
    public static CommentResponse of(Comment comment, boolean mine) {
        return of(comment, mine, List.of());
    }

    public static CommentResponse of(Comment comment, boolean mine, List<CommentResponse> replies) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getMember().getNickname(),
                comment.getMember().getId(),
                mine,
                comment.getCreatedAt(),
                replies);
    }
}
