package com.haru.haruverse.work.dto;

import com.haru.haruverse.search.document.WorkDocument;
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
    /**
     * 검색 문서 → DTO.
     *
     * <p><b>★ES 결과로 DB를 다시 읽지 않는 이유★</b>
     * 화면에 필요한 값을 WorkDocument 에 전부 비정규화해 담아뒀다.
     * id 목록으로 DB를 다시 조회하면 요청이 한 번 더 나가고, ES가 매긴 관련도 순서를
     * 손으로 다시 맞춰야 한다(DB는 그 순서를 모른다). 비정규화를 해둔 이유가 이것이다.
     *
     * <p>대신 색인이 밀리면 결과가 옛날 값일 수 있다 — 재색인 API로 복구한다.
     */
    public static WorkResponse fromDocument(WorkDocument doc) {
        return new WorkResponse(
                doc.getId(),
                doc.getTitle(),
                doc.workType(),
                doc.getSeason(),
                doc.getRating() == null ? null : BigDecimal.valueOf(doc.getRating()),
                doc.getImageUrl(),
                doc.getReleaseDate() == null ? null : LocalDate.parse(doc.getReleaseDate()),
                doc.getGenres(),
                doc.getPlatforms()
        );
    }

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
