package com.haru.haruverse.character.entity;

import com.haru.haruverse.global.common.BaseTimeEntity;
import com.haru.haruverse.voiceactor.entity.VoiceActor;
import jakarta.persistence.*;

/**
 * 캐릭터.
 *
 * <p><b>클래스명이 왜 AnimeCharacter인가</b>
 * {@code Character}로 두면 {@link java.lang.Character}와 이름이 겹쳐,
 * 다른 패키지에서 쓸 때 어느 쪽인지 헷갈리고 import를 빠뜨리면 엉뚱하게 컴파일된다.
 * 테이블명 {@code character}도 SQL 예약어(CHARACTER 타입)라 그대로 쓸 수 없다.
 * 캐릭터 정보를 주는 외부 API가 Jikan(애니)뿐이라 이름과 실제 범위도 맞다.
 *
 * <p><b>여러 작품에 같은 캐릭터가 나올 수 있다</b> (시즌 1·2, 극장판).
 * 그래서 Work와 직접 묶지 않고 {@link WorkCharacter}로 연결한다.
 */
@Entity
@Table(
        name = "anime_character",
        uniqueConstraints = @UniqueConstraint(name = "uk_character_external_id", columnNames = "external_id"),
        indexes = {
                // 캐릭터 도감의 기본 정렬이 인기순이라 favorites에 인덱스를 둔다
                @Index(name = "idx_character_favorites", columnList = "favorites"),
                @Index(name = "idx_character_name", columnList = "name")
        }
)
public class AnimeCharacter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 출처를 포함한 식별자 — 재수집 시 같은 캐릭터를 알아보는 기준.
     *
     * <p>★MAL 번호를 그대로 쓰지 않는 이유★
     * 캐릭터 출처가 Jikan 하나였을 때는 mal_id로 충분했다.
     * 붕괴: 스타레일처럼 MAL에 없는 게임 캐릭터가 들어오면서
     * "번호가 없는 캐릭터"가 생겼고, 출처가 다르면 번호가 겹칠 수도 있다.
     * → 작품(Work.externalId)과 같은 방식으로 접두사를 붙여 구분한다.
     * 예) {@code "jikan-188175"}, {@code "hsr-1001"}
     */
    @Column(name = "external_id", nullable = false, length = 50)
    private String externalId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** MAL 즐겨찾기 수 — 캐릭터 인기 순위의 근거 */
    @Column(nullable = false)
    private Integer favorites = 0;

    /**
     * 일본어 성우.
     *
     * <p>응답에는 10개 언어의 성우가 함께 오지만 전부 저장하면 데이터가 10배가 된다.
     * 화면에도 일본어 성우만 쓰므로 그것만 남긴다.
     *
     * <p>★문자열이 아니라 관계인 이유★ 이름만 들고 있으면
     * "이 성우가 맡은 다른 캐릭터"를 찾을 수 없다. 성우를 축으로 탐색하려면 연결해야 한다.
     * (예전 voice_actor 문자열 컬럼은 DB에 남아 있고, 마이그레이션의 원본으로만 쓰인다)
     *
     * <p>⚠️ 한계: 리메이크처럼 작품마다 성우가 다른 경우, 처음 수집된 값이 남는다.
     * 작품별로 정확히 관리하려면 WorkCharacter로 옮겨야 한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voice_actor_id", foreignKey = @ForeignKey(name = "fk_character_voice_actor"))
    private VoiceActor voiceActor;

    protected AnimeCharacter() {} // JPA용

    public AnimeCharacter(String externalId, String name) {
        this.externalId = externalId;
        this.name = name;
    }

    /** 수집·재수집 때 변할 수 있는 값들을 한 번에 갱신 */
    public AnimeCharacter update(String name, String imageUrl, Integer favorites, VoiceActor voiceActor) {
        if (name != null && !name.isBlank()) this.name = name;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (favorites != null) this.favorites = favorites;
        // 성우는 이미 있으면 덮어쓰지 않는다 (응답에 성우가 빠졌을 때 지워지는 것을 막음)
        if (voiceActor != null) this.voiceActor = voiceActor;
        return this;
    }

    /** 마이그레이션 전용 — 기존 문자열 컬럼에서 만든 성우를 붙인다 */
    public void assignVoiceActor(VoiceActor voiceActor) {
        this.voiceActor = voiceActor;
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public Integer getFavorites() { return favorites; }
    public VoiceActor getVoiceActor() { return voiceActor; }

    /** 화면·DTO용 편의 — 성우가 없으면 null */
    public String getVoiceActorName() { return voiceActor == null ? null : voiceActor.getName(); }
}
