package com.haru.haruverse.work.dto;

import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;

import com.haru.haruverse.genre.entity.Genre;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 작품 '상세'용 응답 — 목록 필드 + 줄거리·출처 등.
 *
 * <p>TODO(하루): 캐릭터·장르·제작사 연관을 붙이면 여기에 리스트 필드를 추가한다.
 */
public record WorkDetailResponse(
        Long id,
        String title,
        WorkType type,
        String synopsis,
        String season,
        BigDecimal rating,
        String imageUrl,
        LocalDate releaseDate,
        WorkSource source,
        String externalId,
        String studio,          // 제작사 이름 (없으면 null)
        List<String> genres,
        // 게임에만 값이 있다(애니는 빈 배열)
        List<String> platforms,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WorkDetailResponse from(Work work) {
        return new WorkDetailResponse(
                work.getId(),
                work.getTitle(),
                work.getType(),
                work.getSynopsis(),
                work.getSeason(),
                work.getRating(),
                work.getImageUrl(),
                work.getReleaseDate(),
                work.getSource(),
                work.getExternalId(),
                work.getStudio() == null ? null : work.getStudio().getName(),
                work.getGenres().stream().map(Genre::getName).toList(),
                List.copyOf(work.getPlatforms()),
                work.getCreatedAt(),
                work.getUpdatedAt()
        );
    }
}
