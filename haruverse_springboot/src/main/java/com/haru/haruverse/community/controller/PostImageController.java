package com.haru.haruverse.community.controller;

import com.haru.haruverse.community.dto.PostImageResponse;
import com.haru.haruverse.community.service.PostImageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

/**
 * 게시글 첨부 이미지 API.
 *
 * <p><b>★파일을 Spring 이 직접 서빙한다★</b>
 * nginx 로 정적 서빙하면 빠르지만 업로드 디렉터리를 두 컨테이너가 공유해야 하고,
 * 그 디렉터리에서 스크립트가 실행되지 않도록 따로 막아야 한다.
 * Spring 이 내보내면 <b>정적 파일로만</b> 나가므로 그 위험이 애초에 없다.
 * 개인 규모의 트래픽에서는 성능 차이도 문제되지 않는다.
 */
@RestController
public class PostImageController {

    private final PostImageService imageService;

    public PostImageController(PostImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/api/posts/{postId}/images")
    public ResponseEntity<List<PostImageResponse>> images(@PathVariable Long postId) {
        return ResponseEntity.ok(imageService.getImages(postId));
    }

    @PostMapping("/api/posts/{postId}/images")
    public ResponseEntity<PostImageResponse> upload(
            @PathVariable Long postId,
            @AuthenticationPrincipal String email,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.status(201).body(imageService.attach(postId, email, file));
    }

    @DeleteMapping("/api/images/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable Long imageId,
                                       @AuthenticationPrincipal String email) {
        imageService.detach(imageId, email);
        return ResponseEntity.noContent().build();
    }

    /**
     * 이미지 파일 — 비로그인도 볼 수 있다(글이 공개이므로).
     *
     * <p>파일명은 UUID 라 오래 캐시해도 안전하다. 내용이 바뀌면 이름도 바뀐다.
     *
     * <p>{@code Content-Disposition: inline} 을 명시해 브라우저가 다운로드로
     * 처리하지 않게 한다. 타입은 <b>저장할 때 검증한 값</b>을 쓴다 —
     * 요청이 준 값을 그대로 돌려주면 그게 곧 XSS 통로가 된다.
     */
    @GetMapping("/api/images/{storedName}")
    public ResponseEntity<?> serve(@PathVariable String storedName) {
        PostImageService.Served served = imageService.serve(storedName);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(served.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .header("Content-Disposition", "inline")
                // 브라우저가 타입을 추측하지 못하게 한다
                .header("X-Content-Type-Options", "nosniff")
                .body(served.resource());
    }
}
