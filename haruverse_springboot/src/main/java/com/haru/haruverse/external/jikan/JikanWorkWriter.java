package com.haru.haruverse.external.jikan;

import com.haru.haruverse.external.jikan.dto.JikanAnime;
import com.haru.haruverse.genre.entity.Genre;
import com.haru.haruverse.genre.service.GenreService;
import com.haru.haruverse.studio.entity.Studio;
import com.haru.haruverse.studio.service.StudioService;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Jikan 애니 1건을 work 테이블에 쓰는 역할(변환 + upsert).
 *
 * <p><b>왜 JikanCollectService에서 분리했나 — Spring 자기호출(self-invocation) 문제</b>
 * <pre>
 *   @Service class A {
 *       void collect() { upsert(x); }        // ← 자기 자신을 직접 호출
 *       @Transactional void upsert(..) {}    // ← ★트랜잭션이 안 걸린다★
 *   }
 * </pre>
 * @Transactional은 스프링이 만든 <b>프록시 객체</b>가 앞뒤로 트랜잭션을 열고 닫아준다.
 * 그런데 같은 클래스 안에서 메서드를 부르면 프록시를 거치지 않고 <b>실제 객체의 메서드</b>가
 * 바로 호출되므로 어노테이션이 무시된다. (컴파일 에러도, 경고도 안 난다 — 그래서 위험하다)
 *
 * <p>→ 다른 빈으로 분리하면 주입받은 것이 프록시라서 트랜잭션이 정상 적용된다.
 */
@Service
public class JikanWorkWriter {

    private static final Logger log = LoggerFactory.getLogger(JikanWorkWriter.class);

    private final WorkRepository workRepository;
    private final StudioService studioService;
    private final GenreService genreService;

    public JikanWorkWriter(WorkRepository workRepository,
                           StudioService studioService,
                           GenreService genreService) {
        this.workRepository = workRepository;
        this.studioService = studioService;
        this.genreService = genreService;
    }

    /**
     * external_id 기준 upsert — 있으면 갱신, 없으면 생성.
     *
     * @return 새로 만들었으면 true, 기존 것을 갱신했으면 false
     */
    @Transactional
    public boolean upsert(JikanAnime anime) {
        String externalId = WorkSource.JIKAN.externalId(anime.malId());
        String title = resolveTitle(anime);
        LocalDate releaseDate = toLocalDate(anime.aired());
        BigDecimal rating = toRating(anime.score());

        // 제작사·장르는 이름 기준으로 찾거나 새로 만든다 (중복 행 방지)
        Studio studio = resolveStudio(anime);
        Set<Genre> genres = resolveGenres(anime);

        return workRepository.findByExternalId(externalId)
                .map(existing -> {
                    // 이미 있는 작품 — 바뀔 수 있는 값만 갱신 (평점·줄거리 등).
                    // 영속 상태이므로 setter 호출만으로 커밋 시점에 UPDATE가 나간다 (더티 체킹)
                    existing.updateFromExternal(title, anime.synopsis(), releaseDate,
                            anime.seasonKey(), rating, anime.posterUrl());
                    existing.assignStudio(studio);
                    existing.replaceGenres(genres);
                    return false;
                })
                .orElseGet(() -> {
                    Work work = new Work(title, WorkType.ANIME, WorkSource.JIKAN)
                            .withDetails(anime.synopsis(), releaseDate, anime.seasonKey(),
                                    rating, anime.posterUrl(), externalId);
                    work.assignStudio(studio);
                    work.replaceGenres(genres);
                    workRepository.save(work);
                    return true;
                });
    }

    /* ── 연관 해석 ───────────────────────────────────── */

    /** 제작사 — Jikan은 배열로 주지만 ERD가 N:1이라 첫 번째만 쓴다 */
    private Studio resolveStudio(JikanAnime anime) {
        List<JikanAnime.Named> studios = anime.studios();
        if (studios == null || studios.isEmpty()) return null;

        String name = studios.get(0).name();
        if (name == null || name.isBlank()) return null;
        return studioService.findOrCreate(name.trim());
    }

    /** 장르 — 순서를 보존하려고 LinkedHashSet 사용 */
    private Set<Genre> resolveGenres(JikanAnime anime) {
        Set<Genre> result = new LinkedHashSet<>();
        if (anime.genres() == null) return result;

        for (JikanAnime.Named g : anime.genres()) {
            if (g.name() == null || g.name().isBlank()) continue;
            result.add(genreService.findOrCreate(g.name().trim()));
        }
        return result;
    }

    /* ── 값 변환 ─────────────────────────────────────── */

    /** 제목 — 영문 제목이 있으면 그쪽이 읽기 편하므로 우선 사용 */
    private String resolveTitle(JikanAnime anime) {
        if (anime.titleEnglish() != null && !anime.titleEnglish().isBlank()) {
            return anime.titleEnglish();
        }
        return anime.title();
    }

    /** "2023-09-29T00:00:00+00:00" → LocalDate. 형식이 깨졌으면 null (수집은 계속) */
    private LocalDate toLocalDate(JikanAnime.Aired aired) {
        if (aired == null || aired.from() == null) return null;
        try {
            return OffsetDateTime.parse(aired.from()).toLocalDate();
        } catch (DateTimeParseException e) {
            log.debug("방영일 파싱 실패: {}", aired.from());
            return null;
        }
    }

    /** double 평점 → DECIMAL(3,1). 소수 첫째 자리까지 (ERD 스펙) */
    private BigDecimal toRating(Double score) {
        if (score == null) return null;
        return BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP);
    }
}
