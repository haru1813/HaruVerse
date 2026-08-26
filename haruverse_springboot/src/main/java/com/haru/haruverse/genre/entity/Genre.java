package com.haru.haruverse.genre.entity;

import jakarta.persistence.*;

/**
 * 장르 — 한 작품이 여러 장르를 갖고, 한 장르에 여러 작품이 속한다 (N : M).
 * 연결은 work_genre 조인 테이블이 담당한다 (Work 쪽에서 @JoinTable로 정의).
 */
@Entity
@Table(name = "genre", uniqueConstraints = @UniqueConstraint(name = "uk_genre_name", columnNames = "name"))
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    protected Genre() {}

    public Genre(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
