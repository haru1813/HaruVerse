package com.haru.haruverse.favorite.service;

import com.haru.haruverse.favorite.entity.Favorite;
import com.haru.haruverse.favorite.repository.FavoriteRepository;
import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import com.haru.haruverse.work.dto.WorkResponse;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.repository.WorkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 찜 서비스.
 *
 * <p>인증 필터가 SecurityContext에 넣어두는 principal은 <b>이메일 문자열</b>이다.
 * (JwtAuthenticationFilter 참고) 그래서 서비스는 email을 받아 회원을 찾는다.
 */
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final WorkRepository workRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           MemberRepository memberRepository,
                           WorkRepository workRepository) {
        this.favoriteRepository = favoriteRepository;
        this.memberRepository = memberRepository;
        this.workRepository = workRepository;
    }

    /**
     * 찜하기. <b>멱등</b> — 이미 찜한 상태로 다시 호출해도 그대로 성공이다.
     *
     * <p>★여기서 DataIntegrityViolationException을 try-catch로 삼키면 안 된다★
     * exists 검사와 저장 사이의 틈으로 동시 요청이 들어오면 유니크 제약에 걸리는데,
     * 그 예외를 잡아서 "이미 있으니 성공"으로 처리하고 싶어진다. 하지만 JPA는
     * INSERT가 터진 시점에 영속성 컨텍스트가 오염되고 트랜잭션이 rollback-only로
     * 마킹된다. 예외를 잡아도 <b>커밋 시점에 UnexpectedRollbackException으로 다시 터진다.</b>
     * → 그냥 밖으로 내보내고 핸들러가 409로 응답한다. 프론트는 낙관적 UI라
     *   이미 하트가 채워져 있으므로 사용자 눈에는 아무 문제가 없다.
     *
     * @return 이번 호출로 새로 찜했으면 true, 원래 찜 상태였으면 false
     */
    @Transactional
    public boolean add(String email, Long workId) {
        Member member = findMember(email);
        if (favoriteRepository.existsByMemberIdAndWorkId(member.getId(), workId)) {
            return false; // 이미 찜함 — 아무것도 하지 않는다
        }
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new NoSuchElementException("작품을 찾을 수 없습니다. id=" + workId));

        favoriteRepository.save(new Favorite(member, work));
        return true;
    }

    /**
     * 찜 해제. 이것도 멱등 — 원래 없던 것을 지워도 성공이다.
     *
     * @return 실제로 지웠으면 true
     */
    @Transactional
    public boolean remove(String email, Long workId) {
        Member member = findMember(email);
        return favoriteRepository.deleteByMemberIdAndWorkId(member.getId(), workId) > 0;
    }

    /** 내가 찜한 작품 목록(최신 찜 순). 홈 그리드와 같은 WorkResponse를 쓴다. */
    @Transactional(readOnly = true)
    public PageResponse<WorkResponse> getMyFavorites(String email, Pageable pageable) {
        Member member = findMember(email);
        Page<Work> works = favoriteRepository.findWorksByMemberId(member.getId(), pageable);
        return PageResponse.of(works, WorkResponse::from); // 지연 로딩은 트랜잭션 안에서
    }

    /** 내가 찜한 작품 id 전체 — 프론트가 Set으로 들고 하트를 칠한다. */
    @Transactional(readOnly = true)
    public List<Long> getMyFavoriteWorkIds(String email) {
        return favoriteRepository.findWorkIdsByMemberId(findMember(email).getId());
    }

    private Member findMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
    }
}
