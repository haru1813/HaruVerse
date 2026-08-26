package com.haru.haruverse.studio.entity;

import jakarta.persistence.*;

/**
 * 제작사 — 여러 작품이 한 제작사에 속한다 (Work N : 1 Studio).
 *
 * <p>이름이 UNIQUE라 같은 제작사가 중복 저장되지 않는다.
 * 외부 API에서 "Madhouse"가 100번 와도 행은 하나만 생긴다.
 */
@Entity
@Table(name = "studio", uniqueConstraints = @UniqueConstraint(name = "uk_studio_name", columnNames = "name"))
public class Studio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    protected Studio() {}

    public Studio(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
