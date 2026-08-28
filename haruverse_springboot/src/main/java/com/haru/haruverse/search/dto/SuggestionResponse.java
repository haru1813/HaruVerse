package com.haru.haruverse.search.dto;

import com.haru.haruverse.search.document.WorkDocument;
import com.haru.haruverse.work.entity.WorkType;

/**
 * 자동완성 후보 한 건.
 *
 * <p>검색창 아래 뜨는 목록이라 <b>화면에 보일 최소한만</b> 담는다.
 * 줄거리·장르까지 실으면 타이핑 한 글자마다 그만큼이 오간다.
 */
public record SuggestionResponse(
        Long id,
        String title,
        WorkType type,
        String imageUrl
) {
    public static SuggestionResponse from(WorkDocument doc) {
        return new SuggestionResponse(doc.getId(), doc.getTitle(), doc.workType(), doc.getImageUrl());
    }
}
