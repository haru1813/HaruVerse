package com.haru.haruverse.external.rawg;

import com.haru.haruverse.external.rawg.dto.RawgGame;
import com.haru.haruverse.genre.entity.Genre;
import com.haru.haruverse.genre.service.GenreService;
import com.haru.haruverse.studio.entity.Studio;
import com.haru.haruverse.studio.service.StudioService;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.search.event.WorkSavedEvent;
import com.haru.haruverse.work.repository.WorkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * RAWG 게임 1건을 work 테이블에 쓰는 역할(변환 + upsert).
 *
 * <p>JikanWorkWriter와 같은 이유로 수집 서비스에서 분리했다
 * (자기호출이면 @Transactional이 적용되지 않음).
 */
@Service
public class RawgWorkWriter {

    private static final Logger log = LoggerFactory.getLogger(RawgWorkWriter.class);

    private final WorkRepository workRepository;
    private final StudioService studioService;
    private final GenreService genreService;
    /** 색인 반영은 이벤트로 넘긴다 — 수집기가 검색을 몰라도 되게 */
    private final ApplicationEventPublisher events;

    public RawgWorkWriter(WorkRepository workRepository,
                          StudioService studioService,
                          GenreService genreService,
                          ApplicationEventPublisher events) {
        this.workRepository = workRepository;
        this.studioService = studioService;
        this.genreService = genreService;
        this.events = events;
    }

    /**
     * external_id 기준 upsert.
     *
     * @return 새로 만들었으면 true, 기존 것을 갱신했으면 false
     */
    @Transactional
    public boolean upsert(RawgGame game) {
        String externalId = WorkSource.RAWG.externalId(game.id());
        LocalDate released = toLocalDate(game.released());
        BigDecimal rating = toRating(game.rating10());

        Studio studio = resolveStudio(game);
        Set<Genre> genres = resolveGenres(game);
        // 목록·상세 응답 모두에 있는 필드라 재수집이 값을 지우지 않는다
        Set<String> platforms = new LinkedHashSet<>(game.platformNames());

        return workRepository.findByExternalId(externalId)
                .map(existing -> {
                    // ⚠️ 목록 수집 시 synopsis가 null로 온다(상세에만 있는 필드).
                    //    이미 저장된 줄거리를 null로 덮어쓰지 않도록 기존 값을 살린다.
                    String synopsis = game.descriptionRaw() != null
                            ? game.descriptionRaw()
                            : existing.getSynopsis();

                    existing.updateFromExternal(game.name(), synopsis, released,
                            null, rating, game.backgroundImage());
                    if (studio != null) existing.assignStudio(studio);
                    if (!genres.isEmpty()) existing.replaceGenres(genres);
                    if (!platforms.isEmpty()) existing.replacePlatforms(platforms);
                    // 수정 분기도 알린다 (더티 체킹이라 save() 호출이 없다)
                    events.publishEvent(new WorkSavedEvent(existing.getId()));
                    return false;
                })
                .orElseGet(() -> {
                    // 게임에는 애니의 '분기(season)' 개념이 없어 null
                    Work work = new Work(game.name(), WorkType.GAME, WorkSource.RAWG)
                            .withDetails(game.descriptionRaw(), released, null,
                                    rating, game.backgroundImage(), externalId);
                    work.assignStudio(studio);
                    work.replaceGenres(genres);
                    work.replacePlatforms(platforms);
                    workRepository.save(work); // id는 여기서 부여된다
                    events.publishEvent(new WorkSavedEvent(work.getId()));
                    return true;
                });
    }

    /* ── 연관 해석 ───────────────────────────────────── */

    private Studio resolveStudio(RawgGame game) {
        String name = game.studioName();
        return name == null ? null : studioService.findOrCreate(name.trim());
    }

    private Set<Genre> resolveGenres(RawgGame game) {
        Set<Genre> result = new LinkedHashSet<>();
        if (game.genres() == null) return result;

        for (RawgGame.Named g : game.genres()) {
            if (g.name() == null || g.name().isBlank()) continue;
            result.add(genreService.findOrCreate(g.name().trim()));
        }
        return result;
    }

    /* ── 값 변환 ─────────────────────────────────────── */

    /** "2013-09-17" → LocalDate. 미정(TBA)이면 null */
    private LocalDate toLocalDate(String released) {
        if (released == null || released.isBlank()) return null;
        try {
            return LocalDate.parse(released);
        } catch (DateTimeParseException e) {
            log.debug("출시일 파싱 실패: {}", released);
            return null;
        }
    }

    /** 10점 만점 평점 → DECIMAL(3,1) */
    private BigDecimal toRating(Double score) {
        if (score == null) return null;
        return BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP);
    }
}
