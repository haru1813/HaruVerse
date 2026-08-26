package com.haru.haruverse.community.repository;

import com.haru.haruverse.community.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByMemberIdAndWorkId(Long memberId, Long workId);

    /**
     * 삭제된 행 수를 돌려준다 → 0이면 "원래 구독이 아니었다".
     *
     * <p>★@Transactional 필수★ 직접 선언한 delete 파생 쿼리에는 트랜잭션이 딸려오지 않는다.
     * (JpaRepository가 물려주는 deleteById 와 달리 SimpleJpaRepository의 @Transactional을 못 받는다)
     * 빠뜨리면 "No EntityManager with actual transaction available"로 터진다.
     */
    @Transactional
    long deleteByMemberIdAndWorkId(Long memberId, Long workId);

    /**
     * 내가 구독한 작품 id 전체 — 구독 버튼 상태 표시용.
     *
     * <p>찜의 findWorkIdsByMemberId 와 같은 이유다: 채널 카드마다 조회하면 요청이 카드 수만큼
     * 나가고, ChannelResponse에 subscribed 플래그를 넣으면 커뮤니티 조회 서비스가
     * "누가 보고 있는지"를 알아야 해서 비로그인/로그인 분기가 도메인으로 파고든다.
     */
    @Query("select s.work.id from Subscription s where s.member.id = :memberId")
    List<Long> findWorkIdsByMemberId(@Param("memberId") Long memberId);

    /** 내 구독 목록 — 최근에 구독한 채널이 먼저 */
    @Query("select s.work.id from Subscription s where s.member.id = :memberId order by s.id desc")
    List<Long> findWorkIdsByMemberIdOrderByRecent(@Param("memberId") Long memberId);
}
