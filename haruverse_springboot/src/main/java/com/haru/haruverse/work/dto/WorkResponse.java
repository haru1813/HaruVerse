package com.haru.haruverse.work.dto;

import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkType;

import com.haru.haruverse.genre.entity.Genre;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 작품 '목록'용 응답 — 카드 한 장에 필요한 최소 정보만.
 *
 * <p>줄거리(synopsis)처럼 긴 필드는 뺐다.
 * 20개씩 내려주는 목록에 TEXT가 섞이면 응답 크기가 불필요하게 커진다.
 */
public record WorkResponse(
        Long id,
        String title,
        WorkType type,
        String season,
        BigDecimal rating,
        String imageUrl,
        LocalDate releaseDate,
        // 카드에 "액션 · 판타지"처럼 보여줄 장르 이름들.
        // 엔티티(Genre)를 그대로 내보내지 않고 이름만 뽑는다 — 응답 크기·결합도 최소화
        List<String> genres,
        // 게임에만 값이 있다(애니는 빈 배열) — "PC · PlayStation" 처럼 보여준다
        List<String> platforms
) {
    // 엔티티 → DTO 변환 (정적 팩터리). PageResponse.of(page, WorkResponse::from) 형태로 쓴다.
    public static WorkResponse from(Work work) {
        return new WorkResponse(
                work.getId(),
                work.getTitle(),
                work.getType(),
                work.getSeason(),
                work.getRating(),
                work.getImageUrl(),
                work.getReleaseDate(),
                work.getGenres().stream().map(Genre::getName).toList(),
                List.copyOf(work.getPlatforms())
        );
    }
}
