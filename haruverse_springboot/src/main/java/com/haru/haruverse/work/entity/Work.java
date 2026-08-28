package com.haru.haruverse.work.entity;

import com.haru.haruverse.genre.entity.Genre;
import com.haru.haruverse.global.common.BaseTimeEntity;
import com.haru.haruverse.studio.entity.Studio;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 작품 엔티티 — 애니메이션·게임을 함께 담는 핵심 테이블.
 *
 * <p>설계문서 ⑥ ERD의 work 테이블에 대응.
 * studio·genre·character 연관은 다음 단계에서 붙인다(지금은 Work 단독).
 */
@Entity
@Table(
        name = "work",
        // 인덱스 — 목록 필터·정렬에 쓰는 컬럼 (ERD 4장)
        indexes = {
                @Index(name = "idx_work_title", columnList = "title"),
                @Index(name = "idx_work_release_date", columnList = "release_date"),
                @Index(name = "idx_work_season", columnList = "season"),
                @Index(name = "idx_work_type", columnList = "type")
        },
        // 외부 API에서 같은 작품을 두 번 수집하지 않도록 (upsert 판단 키)
        uniqueConstraints = @UniqueConstraint(name = "uk_work_external_id", columnNames = "external_id")
)
public class Work extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 한국어 제목 — TMDB 에서 가져온다.
     *
     * <p><b>★nullable 이어야 한다★</b> 게임은 TMDB 에 없고, 애니도 매칭에 실패하면 못 채운다.
     * 확신이 없을 때 억지로 채우면 <b>틀린 제목이 붙는다</b> — 그건 비어 있는 것보다 나쁘다.
     * (TmdbTitleMatcher 가 연도·제목 두 관문을 통과한 것만 채택하는 이유)
     */
    @Column(name = "title_ko", length = 255)
    private String titleKo;

    // ANIME / GAME — 문자열로 저장 (위 WorkType 주석 참고)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkType type;

    // 줄거리 — 길어질 수 있으므로 TEXT
    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    // 분기 (예: "2026-spring")
    @Column(length = 20)
    private String season;

    // 평점 — 소수점 오차가 없어야 하므로 double이 아닌 BigDecimal(DECIMAL(3,1))
    @Column(precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // 외부 API의 작품 ID (Jikan의 mal_id, RAWG의 id 등)
    @Column(name = "external_id", length = 50)
    private String externalId;

    // 수집 출처 — JIKAN / RAWG
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkSource source;

    /**
     * 제작사 (N:1).
     *
     * <p>★fetch = LAZY★ — @ManyToOne의 기본값은 EAGER라서, 작품을 조회할 때마다
     * 제작사까지 무조건 조인/추가 쿼리가 나간다. 목록 20건이면 쓸데없는 쿼리가 20번.
     * 필요할 때만 가져오도록 LAZY로 명시한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_id", foreignKey = @ForeignKey(name = "fk_work_studio"))
    private Studio studio;

    /**
     * 장르 (N:M) — work_genre 조인 테이블로 연결.
     *
     * <p>★@BatchSize★ — 목록 20건을 뿌릴 때 각 작품의 장르를 하나씩 조회하면
     * 쿼리가 1 + 20번 나간다(N+1 문제). BatchSize를 주면 Hibernate가
     * `where work_id in (?, ?, ... )` 형태로 묶어서 한 번에 가져온다.
     *
     * <p>목록 조회에 fetch join을 쓰지 않은 이유: 컬렉션을 조인하면 행이 뻥튀기되어
     * 페이징이 깨진다(Hibernate가 메모리에서 페이징하며 경고를 띄운다).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "work_genre",
            joinColumns = @JoinColumn(name = "work_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @BatchSize(size = 100)
    private Set<Genre> genres = new LinkedHashSet<>();

    /**
     * 플랫폼 — <b>게임에만</b> 값이 있다 (애니는 빈 집합).
     *
     * <p><b>★왜 Genre처럼 엔티티가 아니라 @ElementCollection인가★</b>
     * 장르는 애니·게임 양쪽에서 들어와 이름 정규화가 필요하고(GenreService.findOrCreate),
     * 장르별 필터·목록 화면이 있어서 <b>독립된 신분</b>이 필요했다.
     * 플랫폼은 RAWG가 주는 고정된 낱말 몇 개(PC·PlayStation·Xbox·Nintendo·SEGA…)를
     * 그대로 보여주기만 한다. 마스터 테이블을 두면 관리 대상만 하나 늘어난다.
     * → 나중에 "플랫폼으로 거르기"가 필요해지면 그때 Genre처럼 승격시킨다.
     *
     * <p>RAWG의 platforms(기종별: PS4·PS5·Xbox One…)가 아니라
     * <b>parent_platforms</b>(묶음: PlayStation·Xbox…)를 담는다. 기종별로 담으면
     * 카드 한 장에 칩이 열 개씩 붙는다.
     *
     * <p>genres와 같은 이유로 @BatchSize — 없으면 목록 24건에 조회가 24번 더 나간다.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "work_platform",
            joinColumns = @JoinColumn(name = "work_id", foreignKey = @ForeignKey(name = "fk_work_platform_work"))
    )
    @Column(name = "platform", nullable = false, length = 50)
    @BatchSize(size = 100)
    private Set<String> platforms = new LinkedHashSet<>();

    protected Work() {} // JPA용 기본 생성자 (직접 호출 금지)

    // 필수값만 받는 생성자. 나머지는 아래 with~ 메서드로 채운다.
    public Work(String title, WorkType type, WorkSource source) {
        this.title = title;
        this.type = type;
        this.source = source;
    }

    /**
     * 선택 필드를 한 번에 채우는 메서드.
     * 생성자 파라미터를 10개 늘어놓으면 순서를 헷갈리기 쉬워서 분리했다.
     * (나중에 필드가 더 늘면 빌더 패턴으로 바꾸는 게 정석)
     */
    public Work withDetails(String synopsis, LocalDate releaseDate, String season,
                            BigDecimal rating, String imageUrl, String externalId) {
        this.synopsis = synopsis;
        this.releaseDate = releaseDate;
        this.season = season;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.externalId = externalId;
        return this;
    }

    /**
     * 외부 API 재수집 시 변경된 값만 갱신 (제목·평점 등은 바뀔 수 있음).
     * external_id가 UNIQUE이므로 "있으면 update, 없으면 insert"의 update 쪽을 담당.
     */
    public void updateFromExternal(String title, String synopsis, LocalDate releaseDate,
                                   String season, BigDecimal rating, String imageUrl) {
        this.title = title;
        this.synopsis = synopsis;
        this.releaseDate = releaseDate;
        this.season = season;
        this.rating = rating;
        this.imageUrl = imageUrl;
    }

    /** 제작사 지정 (외부 수집 시 호출) */
    public void assignStudio(Studio studio) {
        this.studio = studio;
    }

    /** 장르 교체 — 재수집 시 외부 API의 최신 목록으로 통째로 갈아끼운다 */
    public void replaceGenres(Set<Genre> newGenres) {
        this.genres.clear();
        this.genres.addAll(newGenres);
    }

    /** 한국어 제목 지정 — TMDB 수집에서만 부른다 */
    public void assignTitleKo(String titleKo) {
        this.titleKo = titleKo;
    }

    /** 플랫폼 통째 교체 — 재수집 때 최신 목록으로 갈아끼운다 */
    public void replacePlatforms(Set<String> newPlatforms) {
        this.platforms.clear();
        this.platforms.addAll(newPlatforms);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getTitleKo() { return titleKo; }
    public Studio getStudio() { return studio; }
    public Set<Genre> getGenres() { return genres; }
    public Set<String> getPlatforms() { return platforms; }
    public WorkType getType() { return type; }
    public String getSynopsis() { return synopsis; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public String getSeason() { return season; }
    public BigDecimal getRating() { return rating; }
    public String getImageUrl() { return imageUrl; }
    public String getExternalId() { return externalId; }
    public WorkSource getSource() { return source; }
}
