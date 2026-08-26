package com.haru.haruverse.voiceactor.entity;

import com.haru.haruverse.global.common.BaseTimeEntity;
import jakarta.persistence.*;

/**
 * 성우.
 *
 * <p><b>왜 문자열이 아니라 엔티티인가</b>
 * 처음에는 캐릭터에 성우 <i>이름만</i> 문자열로 붙여뒀다. 그러면
 * "이 성우가 맡은 다른 캐릭터"를 찾을 수가 없다 — 이름으로 LIKE 검색을 돌려야 하고,
 * 표기가 조금만 달라도 다른 사람이 된다. 성우를 축으로 탐색하려면 엔티티여야 한다.
 *
 * <p><b>malId·imageUrl이 nullable인 이유</b>
 * 기존에 저장된 179명분은 이름만 있다(수집 당시 성우의 식별자·이미지를 버렸다).
 * MyAnimeList가 복구되어 재수집하면 그때 채워진다.
 * 그때까지도 성우 도감은 이름과 담당 캐릭터로 동작해야 하므로 필수로 두지 않는다.
 *
 * <p>식별 기준은 <b>이름</b>이다. malId가 있으면 그쪽이 더 정확하지만,
 * 없는 데이터가 섞여 있어 이름을 유니크 키로 삼는다.
 * (MAL 표기가 "성, 이름" 형식이라 동명이인 충돌 가능성은 낮다)
 */
@Entity
@Table(
        name = "voice_actor",
        uniqueConstraints = @UniqueConstraint(name = "uk_voice_actor_name", columnNames = "name"),
        indexes = @Index(name = "idx_voice_actor_mal_id", columnList = "mal_id")
)
public class VoiceActor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** MyAnimeList의 인물 번호. 이름만 이관한 데이터에는 없다 */
    @Column(name = "mal_id")
    private Long malId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    protected VoiceActor() {} // JPA용

    public VoiceActor(String name) {
        this.name = name;
    }

    /**
     * 재수집 때 비어 있던 값을 채운다.
     *
     * <p>이미 있는 값을 null로 덮지 않는다 — 이름만 있는 상태로 이관된 데이터에
     * 식별자·이미지를 붙이는 게 목적이지, 지우는 게 아니다.
     */
    public VoiceActor fillIfAbsent(Long malId, String imageUrl) {
        if (this.malId == null && malId != null) this.malId = malId;
        if (this.imageUrl == null && imageUrl != null && !imageUrl.isBlank()) this.imageUrl = imageUrl;
        return this;
    }

    public Long getId() { return id; }
    public Long getMalId() { return malId; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
}
