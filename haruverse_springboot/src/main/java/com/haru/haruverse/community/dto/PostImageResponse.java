package com.haru.haruverse.community.dto;

import com.haru.haruverse.community.entity.PostImage;

/** 첨부 이미지 한 장 */
public record PostImageResponse(
        Long id,
        /** 화면이 그대로 src 에 넣는 주소 */
        String url,
        String originalName,
        long byteSize
) {
    public static PostImageResponse from(PostImage image) {
        return new PostImageResponse(
                image.getId(),
                "/api/images/" + image.getStoredName(),
                image.getOriginalName(),
                image.getByteSize());
    }
}
