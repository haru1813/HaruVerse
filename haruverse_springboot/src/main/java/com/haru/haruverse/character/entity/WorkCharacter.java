package com.haru.haruverse.character.entity;

import com.haru.haruverse.work.entity.Work;
import jakarta.persistence.*;

/**
 * 작품 ↔ 캐릭터 연결.
 *
 * <p><b>왜 @ManyToMany가 아니라 연결 엔티티인가</b>
 * 연결 자체에 <b>비중(role)</b>이라는 속성이 있기 때문이다.
 * 같은 캐릭터라도 어떤 작품에서는 주역, 다른 작품에서는 조역일 수 있어
 * role은 캐릭터가 아니라 '이 작품에서의 관계'에 속한다.
 */
@Entity
@Table(
        name = "work_character",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_work_character",
                columnNames = {"work_id", "character_id"}),
        indexes = @Index(name = "idx_work_character_work", columnList = "work_id")
)
public class WorkCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne의 기본값은 EAGER — 명시하지 않으면 연결 하나 읽을 때마다 양쪽을 다 끌고 온다
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", foreignKey = @ForeignKey(name = "fk_work_character_work"))
    private Work work;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", foreignKey = @ForeignKey(name = "fk_work_character_character"))
    private AnimeCharacter character;

    @Enumerated(EnumType.STRING) // ORDINAL은 enum 순서가 바뀌면 기존 데이터가 뒤틀린다
    @Column(nullable = false, length = 20)
    private CharacterRole role;

    protected WorkCharacter() {} // JPA용

    public WorkCharacter(Work work, AnimeCharacter character, CharacterRole role) {
        this.work = work;
        this.character = character;
        this.role = role;
    }

    public void changeRole(CharacterRole role) { this.role = role; }

    public Long getId() { return id; }
    public Work getWork() { return work; }
    public AnimeCharacter getCharacter() { return character; }
    public CharacterRole getRole() { return role; }
}
