package com.haru.haruverse.community.service;

import com.haru.haruverse.community.dto.ChannelResponse;
import com.haru.haruverse.community.entity.Post;
import com.haru.haruverse.community.entity.Subscription;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.community.repository.SubscriptionRepository;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.repository.WorkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 채널 구독 서비스.
 *
 * <p>인증 필터가 SecurityContext에 넣는 principal은 <b>이메일 문자열</b>이다.
 * 구독은 전부 로그인 기능이라 email이 null로 들어올 일이 없다(경로가 authenticated).
 */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final WorkRepository workRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               PostRepository postRepository,
                               MemberRepository memberRepository,
                               WorkRepository workRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
        this.workRepository = workRepository;
    }

    /**
     * 구독. <b>멱등</b> — 이미 구독한 상태로 다시 호출해도 그대로 성공이다.
     *
     * <p>★DataIntegrityViolationException을 try-catch로 삼키지 않는다★
     * exists 검사와 저장 사이의 틈으로 동시 요청이 들어오면 유니크 제약에 걸리는데,
     * JPA는 INSERT가 터진 시점에 트랜잭션을 rollback-only로 마킹한다.
     * 예외를 잡아도 <b>커밋 때 UnexpectedRollbackException으로 다시 터진다.</b>
     * → 밖으로 내보내고 핸들러가 409로 응답한다. 프론트는 낙관적 UI라 사용자 눈엔 문제없다.
     * (찜의 FavoriteService.add 와 같은 이유)
     *
     * @return 이번 호출로 새로 구독했으면 true
     */
    @Transactional
    public boolean subscribe(String email, Long workId) {
        Member member = findMember(email);
        if (subscriptionRepository.existsByMemberIdAndWorkId(member.getId(), workId)) {
            return false;
        }
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new NoSuchElementException("작품을 찾을 수 없습니다. id=" + workId));

        subscriptionRepository.save(new Subscription(member, work));
        return true;
    }

    /** 구독 해제. 이것도 멱등 — 원래 없던 것을 지워도 성공이다. */
    @Transactional
    public boolean unsubscribe(String email, Long workId) {
        Member member = findMember(email);
        return subscriptionRepository.deleteByMemberIdAndWorkId(member.getId(), workId) > 0;
    }

    /** 내가 구독한 작품 id 전체 — 프론트가 Set으로 들고 버튼 상태를 칠한다. */
    @Transactional(readOnly = true)
    public List<Long> getMyWorkIds(String email) {
        return subscriptionRepository.findWorkIdsByMemberId(findMember(email).getId());
    }

    /**
     * 내 구독 채널 목록 — 커뮤니티 첫 화면 상단.
     *
     * <p><b>쿼리는 항상 네 번</b>이다(채널 수와 무관, N+1 아님).
     * ① 구독한 작품 id ② 그 작품들의 글 수·최신 글 id ③ 최신 글 본문·작성자 ④ 작품 정보
     * ③에 작품이 fetch join으로 딸려오지만 <b>글이 0개인 채널은 ③에 없어서</b>
     * 작품을 따로 읽어야 한다.
     *
     * <p><b>정렬</b>: 최신 글이 있는 채널이 먼저(글 id 내림차순), 글이 없는 채널은 뒤로.
     * 구독한 순서로 두면 새 글이 올라온 채널이 아래로 밀려 구독의 의미가 없다.
     *
     * <p>페이징을 두지 않은 이유: 구독은 본인이 하나씩 눌러 만든 목록이라 수가 제한적이고,
     * 첫 화면 상단 섹션이라 페이지네이션 UI가 오히려 방해가 된다.
     */
    @Transactional(readOnly = true)
    public List<ChannelResponse> getMyChannels(String email) {
        Member member = findMember(email);
        List<Long> workIds = subscriptionRepository.findWorkIdsByMemberIdOrderByRecent(member.getId());
        // 비었으면 여기서 끝낸다 — 아래 in 절에 빈 목록을 넘기면 "in ()" 이 되어 문법 오류다
        if (workIds.isEmpty()) {
            return List.of();
        }

        // [workId, count, latestPostId] — ★글이 있는 채널만 잡힌다★
        Map<Long, Long> countByWorkId = new HashMap<>();
        Map<Long, Long> latestIdByWorkId = new HashMap<>();
        for (Object[] row : postRepository.findChannelStatsByWorkIds(workIds)) {
            Long workId = ((Number) row[0]).longValue();
            countByWorkId.put(workId, ((Number) row[1]).longValue());
            latestIdByWorkId.put(workId, ((Number) row[2]).longValue());
        }

        Map<Long, Post> latestByWorkId = new HashMap<>();
        if (!latestIdByWorkId.isEmpty()) {
            for (Post p : postRepository.findByIdInWithWorkAndMember(latestIdByWorkId.values())) {
                latestByWorkId.put(p.getWork().getId(), p);
            }
        }

        List<ChannelResponse> channels = new ArrayList<>();
        for (Work work : workRepository.findAllById(workIds)) {
            Post latest = latestByWorkId.get(work.getId());
            channels.add(new ChannelResponse(
                    work.getId(), work.getTitle(), work.getImageUrl(),
                    countByWorkId.getOrDefault(work.getId(), 0L),
                    latest == null ? null : latest.getId(),
                    latest == null ? null : latest.getTitle(),
                    latest == null ? null : latest.getMember().getNickname(),
                    latest == null ? null : latest.getCreatedAt()));
        }

        // ★findAllById 는 순서를 보장하지 않는다★ → 여기서 명시적으로 정렬한다
        channels.sort(Comparator
                .comparing(ChannelResponse::latestPostId,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ChannelResponse::workTitle));
        return channels;
    }

    private Member findMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
    }
}
