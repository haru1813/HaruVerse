package com.haru.haruverse.community.repository;

import com.haru.haruverse.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * ★파생 delete 쿼리에는 @Transactional 이 필요하다★
 * {@code deleteById} 같은 기본 메서드는 SimpleJpaRepository 에 트랜잭션이 걸려 있지만,
 * 직접 선언한 {@code deleteBy...} 에는 없다. 트랜잭션 밖에서 부르면
 * "No EntityManager with actual transaction available" 로 터진다.
 */
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    /** 삭제된 행 수를 돌려준다 → 0이면 원래 추천하지 않은 상태 */
    @Transactional
    long deleteByPostIdAndMemberId(Long postId, Long memberId);

    long countByPostId(Long postId);

    /** 글을 지울 때 먼저 호출 (댓글과 같은 이유) */
    @Transactional
    void deleteByPostId(Long postId);
}
