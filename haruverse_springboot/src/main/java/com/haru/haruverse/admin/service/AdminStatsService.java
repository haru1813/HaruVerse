package com.haru.haruverse.admin.service;

import com.haru.haruverse.admin.dto.AdminStats;
import com.haru.haruverse.character.repository.AnimeCharacterRepository;
import com.haru.haruverse.character.repository.WorkCharacterRepository;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.member.entity.MemberRole;
import com.haru.haruverse.member.repository.MemberRepository;
import com.haru.haruverse.search.service.WorkIndexService;
import com.haru.haruverse.studio.repository.StudioRepository;
import com.haru.haruverse.voiceactor.repository.VoiceActorRepository;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드 통계 집계.
 *
 * <p>전부 {@code count} 쿼리다. 작품이 200편 수준이라 매번 세도 부담이 없고,
 * 캐싱을 넣으면 "고쳤는데 화면이 안 바뀐다"가 생긴다 — 운영 도구에서는 그게 더 나쁘다.
 */
@Service
public class AdminStatsService {

    private final WorkRepository workRepository;
    private final AnimeCharacterRepository characterRepository;
    private final WorkCharacterRepository workCharacterRepository;
    private final VoiceActorRepository voiceActorRepository;
    private final StudioRepository studioRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final WorkIndexService indexService;

    public AdminStatsService(WorkRepository workRepository,
                             AnimeCharacterRepository characterRepository,
                             WorkCharacterRepository workCharacterRepository,
                             VoiceActorRepository voiceActorRepository,
                             StudioRepository studioRepository,
                             MemberRepository memberRepository,
                             PostRepository postRepository,
                             CommentRepository commentRepository,
                             WorkIndexService indexService) {
        this.workRepository = workRepository;
        this.characterRepository = characterRepository;
        this.workCharacterRepository = workCharacterRepository;
        this.voiceActorRepository = voiceActorRepository;
        this.studioRepository = studioRepository;
        this.memberRepository = memberRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.indexService = indexService;
    }

    @Transactional(readOnly = true)
    public AdminStats collect() {
        long works = workRepository.count();
        long anime = workRepository.countByType(WorkType.ANIME);
        long games = workRepository.countByType(WorkType.GAME);

        // ★색인 조회는 실패할 수 있다★ ES가 죽어 있어도 나머지 통계는 그대로 나와야 한다.
        //   WorkIndexService 는 실패를 -1 로 알린다 — 그걸 null 로 바꿔 내보낸다.
        //   0("연결됐지만 비어 있음")과 null("물어볼 수 없었음")은 뜻이 다르므로 구분한다.
        long rawIndexed = indexService.countIndexed();
        Long indexed = rawIndexed < 0 ? null : rawIndexed;

        return new AdminStats(
                works,
                anime,
                games,
                workRepository.countByTypeAndTitleKoIsNotNull(WorkType.ANIME),
                characterRepository.count(),
                workCharacterRepository.countDistinctWorksByType(WorkType.ANIME),
                voiceActorRepository.count(),
                studioRepository.count(),
                memberRepository.count(),
                memberRepository.countByRole(MemberRole.ADMIN),
                postRepository.count(),
                commentRepository.count(),
                indexed,
                // ES에 못 붙었으면 어긋났는지 '알 수 없는' 것이지 어긋난 게 아니다
                indexed != null && indexed != works);
    }
}
