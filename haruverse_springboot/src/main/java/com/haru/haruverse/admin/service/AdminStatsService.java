package com.haru.haruverse.admin.service;

import com.haru.haruverse.admin.dto.AdminStats;
import com.haru.haruverse.admin.dto.StatsCounts;
import com.haru.haruverse.admin.mapper.AdminStatsMapper;
import com.haru.haruverse.search.service.WorkIndexService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드 통계 집계.
 *
 * <p><b>★쿼리 12번을 1번으로★</b>
 * 예전에는 리포지토리 여섯 개를 주입받아 {@code count()} 를 12번 불렀다.
 * 화면 한 장을 그리려고 DB 를 열두 번 왕복한 셈이다.
 * 스칼라 서브쿼리로 묶은 MyBatis 매퍼 하나가 그 일을 한 번에 한다.
 *
 * <p>통계는 순수 조회다 — 엔티티를 만들지도, 상태를 바꾸지도 않는다.
 * JPA 가 잘하는 일(생명주기·더티 체킹)이 하나도 필요 없는 자리라
 * <b>어려운 읽기는 MyBatis</b> 라는 이 프로젝트의 경계에 정확히 들어맞는다.
 *
 * <p>캐싱은 넣지 않는다. 작품이 200편 수준이라 매번 세도 부담이 없고,
 * 캐시가 있으면 "고쳤는데 화면이 안 바뀐다"가 생긴다 — 운영 도구에서는 그게 더 나쁘다.
 */
@Service
public class AdminStatsService {

    private final AdminStatsMapper statsMapper;
    private final WorkIndexService indexService;

    public AdminStatsService(AdminStatsMapper statsMapper, WorkIndexService indexService) {
        this.statsMapper = statsMapper;
        this.indexService = indexService;
    }

    @Transactional(readOnly = true)
    public AdminStats collect() {
        StatsCounts db = statsMapper.countAll();

        // ★색인 조회는 실패할 수 있다★ ES가 죽어 있어도 나머지 통계는 그대로 나와야 한다.
        //   WorkIndexService 는 실패를 -1 로 알린다 — 그걸 null 로 바꿔 내보낸다.
        //   0("연결됐지만 비어 있음")과 null("물어볼 수 없었음")은 뜻이 다르므로 구분한다.
        long rawIndexed = indexService.countIndexed();
        Long indexed = rawIndexed < 0 ? null : rawIndexed;

        return new AdminStats(
                db.works(),
                db.anime(),
                db.games(),
                db.titleKoFilled(),
                db.characters(),
                db.animeWithCharacters(),
                db.voiceActors(),
                db.studios(),
                db.members(),
                db.admins(),
                db.posts(),
                db.comments(),
                indexed,
                // ES에 못 붙었으면 어긋났는지 '알 수 없는' 것이지 어긋난 게 아니다
                indexed != null && indexed != db.works());
    }
}
