package com.haru.haruverse.search.document;

import com.haru.haruverse.genre.entity.Genre;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkType;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * 검색용 작품 문서 — Elasticsearch 색인 단위.
 *
 * <p><b>★JPA 엔티티(Work)를 그대로 쓰지 않는 이유★</b>
 * 하나의 클래스에 {@code @Entity} 와 {@code @Document} 를 같이 붙이면 편해 보이지만,
 * 두 저장소의 요구가 정면으로 충돌한다.
 * <ul>
 *   <li>DB는 정규화를 원한다 — 장르는 별도 표, 조인해서 읽는다</li>
 *   <li>ES는 <b>비정규화</b>를 원한다 — 장르를 문서 안에 박아야 조인 없이 한 번에 검색된다</li>
 * </ul>
 * 게다가 검색에 필요 없는 필드(createdAt·externalId·source)까지 색인에 들어가고,
 * 매핑을 바꾸려면 엔티티를 건드려야 해서 두 관심사가 엉킨다. 그래서 분리한다.
 *
 * <p><b>★createIndex = false★</b>
 * 기본값(true)이면 앱이 뜰 때 색인을 만들려고 ES에 접속한다.
 * 검색은 부가 기능인데 ES가 죽어 있다고 앱이 못 뜨면 안 된다.
 * 색인 생성은 재색인 API가 명시적으로 한다.
 */
@Document(indexName = "works", createIndex = false)
@Setting(settingPath = "elasticsearch/work-settings.json")
public class WorkDocument {

    @Id
    private Long id;

    /**
     * 원제(대개 영문). 검색의 주 대상.
     *
     * <p>{@code text} 로 분석해 부분 일치·오타 교정이 되게 하고,
     * 하위 필드 {@code keyword} 로 정확 일치·정렬도 함께 쓸 수 있게 둔다.
     */
    @Field(type = FieldType.Text, analyzer = "work_analyzer")
    private String title;

    /**
     * 한글 제목 — <b>지금은 전부 비어 있다.</b>
     *
     * <p>Jikan·RAWG 가 주는 제목이 전부 영문이라 저장된 한글 제목이 한 건도 없다.
     * 즉 "프리렌"으로 검색해서 {@code Frieren} 을 찾는 건 <b>검색 엔진이 아니라 데이터 문제</b>다.
     * 나중에 한글 제목을 주는 출처(TMDB 등)를 붙이면 이 필드만 채우면 되도록 <b>미리 열어둔다.</b>
     * 그때 매핑을 다시 만들지 않아도 되고, 색인 구조도 그대로다.
     */
    @Field(type = FieldType.Text, analyzer = "work_analyzer")
    private String titleKo;

    /** 별칭·다른 표기 — 위와 같은 이유로 미리 열어둔다 (약칭·시리즈명 등) */
    @Field(type = FieldType.Text, analyzer = "work_analyzer")
    private List<String> aliases = new ArrayList<>();

    /** 줄거리 — 제목보다 가중치를 낮춰 쓴다 (제목 일치가 항상 우선이어야 한다) */
    @Field(type = FieldType.Text, analyzer = "work_analyzer")
    private String synopsis;

    /* ── 필터·정렬용 (분석하지 않는다) ─────────────────── */

    @Field(type = FieldType.Keyword)
    private String type;              // ANIME · GAME

    @Field(type = FieldType.Keyword)
    private String season;            // "2023 가을" (게임은 null)

    @Field(type = FieldType.Keyword)
    private String studio;

    @Field(type = FieldType.Keyword)
    private List<String> genres = new ArrayList<>();

    @Field(type = FieldType.Keyword)
    private List<String> platforms = new ArrayList<>();

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd")
    private String releaseDate;

    /** 화면에 바로 뿌리기 위한 값 — 검색 대상은 아니다 */
    @Field(type = FieldType.Keyword, index = false)
    private String imageUrl;

    public WorkDocument() {}

    /**
     * 엔티티 → 문서.
     *
     * <p>★지연 로딩 주의★ genres·platforms 를 건드리므로 <b>트랜잭션 안에서</b> 불러야 한다.
     * 밖에서 부르면 LazyInitializationException 이 난다(open-in-view 를 껐다).
     */
    public static WorkDocument from(Work work) {
        WorkDocument doc = new WorkDocument();
        doc.id = work.getId();
        doc.title = work.getTitle();
        doc.synopsis = work.getSynopsis();
        doc.type = work.getType() == null ? null : work.getType().name();
        doc.season = work.getSeason();
        doc.studio = work.getStudio() == null ? null : work.getStudio().getName();
        doc.genres = work.getGenres().stream().map(Genre::getName).toList();
        doc.platforms = List.copyOf(work.getPlatforms());
        doc.rating = work.getRating() == null ? null : work.getRating().doubleValue();
        doc.releaseDate = work.getReleaseDate() == null ? null : work.getReleaseDate().toString();
        doc.imageUrl = work.getImageUrl();
        // titleKo·aliases 는 아직 출처가 없다 (위 주석 참고)
        return doc;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getTitleKo() { return titleKo; }
    public List<String> getAliases() { return aliases; }
    public String getSynopsis() { return synopsis; }
    public String getType() { return type; }
    public String getSeason() { return season; }
    public String getStudio() { return studio; }
    public List<String> getGenres() { return genres; }
    public List<String> getPlatforms() { return platforms; }
    public Double getRating() { return rating; }
    public String getReleaseDate() { return releaseDate; }
    public String getImageUrl() { return imageUrl; }

    /** WorkType 으로 되돌린다 — 검색 결과를 기존 응답 DTO에 맞출 때 쓴다 */
    public WorkType workType() {
        return type == null ? null : WorkType.valueOf(type);
    }
}
