package com.haru.haruverse.community.repository;

import com.haru.haruverse.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    /** 글에 붙은 이미지 — 올린 순서대로 */
    List<PostImage> findByPostIdOrderByIdAsc(Long postId);

    long countByPostId(Long postId);

    /** 서빙할 때 파일명으로 찾는다 (경로가 아니라 이름으로만 접근한다) */
    Optional<PostImage> findByStoredName(String storedName);

    /**
     * 글을 지울 때 먼저 호출 — 이미지가 남아 있으면 FK 위반으로 삭제가 막힌다.
     *
     * <p>★디스크 파일은 이 메서드가 지우지 않는다★
     * DB 행만 사라진다. 실제 파일 삭제는 서비스가 별도로 하고,
     * 실패해도 삭제 자체는 진행한다(고아 파일이 남는 게 깨진 링크보다 낫다).
     */
    @Transactional
    void deleteByPostId(Long postId);
}
