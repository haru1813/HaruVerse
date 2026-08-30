package com.haru.haruverse.community.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 첨부 이미지의 <b>파일</b>을 다룬다 — DB 는 모른다.
 *
 * <p><b>★확장자를 믿지 않는다★</b>
 * {@code .jpg} 로 이름 붙인 실행 파일을 올릴 수 있다. 브라우저가 보내는
 * {@code Content-Type} 도 클라이언트가 정하는 값이라 마찬가지다.
 * 그래서 <b>파일 앞부분의 매직 넘버</b>를 직접 읽어 실제 형식을 확인한다.
 *
 * <p><b>★파일명은 서버가 만든다★</b>
 * 올린 이름을 그대로 쓰면 {@code ../../etc/passwd} 같은 경로 탐색이 열리고,
 * 같은 이름끼리 덮어쓴다. UUID 로 새로 짓고, 확장자도 <b>검증된 형식</b>에서 뽑는다.
 *
 * <p><b>★디렉터리를 벗어나지 못하게 한 번 더 막는다★</b>
 * UUID 만 쓰므로 이론상 안전하지만, 읽을 때 경로를 정규화해
 * 업로드 디렉터리 밖을 가리키면 거부한다. 방어는 겹쳐야 의미가 있다.
 */
@Component
public class ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

    /**
     * 허용 형식과 그 매직 넘버.
     *
     * <p>SVG 는 <b>일부러 뺐다</b> — 텍스트 파일이라 안에 스크립트를 넣을 수 있고,
     * 그대로 서빙하면 XSS 가 된다. 첨부 목록에 SVG 가 필요할 일도 없다.
     */
    private enum ImageType {
        JPEG("image/jpeg", ".jpg", new int[]{0xFF, 0xD8, 0xFF}),
        PNG("image/png", ".png", new int[]{0x89, 0x50, 0x4E, 0x47}),
        GIF("image/gif", ".gif", new int[]{0x47, 0x49, 0x46, 0x38}),
        // WebP 는 "RIFF" 뒤 4바이트(크기)를 건너뛰고 "WEBP" 가 온다 → 앞 4바이트만 본다
        WEBP("image/webp", ".webp", new int[]{0x52, 0x49, 0x46, 0x46});

        final String contentType;
        final String extension;
        final int[] magic;

        ImageType(String contentType, String extension, int[] magic) {
            this.contentType = contentType;
            this.extension = extension;
            this.magic = magic;
        }
    }

    private final Path root;

    public ImageStorage(@Value("${upload.image-dir}") String imageDir) {
        this.root = Paths.get(imageDir).toAbsolutePath().normalize();
    }

    /**
     * 파일을 저장하고 서버가 지은 이름을 돌려준다.
     *
     * @throws IllegalStateException 비었거나, 이미지가 아니거나, 형식이 허용 목록 밖일 때
     */
    public Stored save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("빈 파일은 올릴 수 없습니다.");
        }

        ImageType type = detect(file);
        String storedName = UUID.randomUUID().toString().replace("-", "") + type.extension;

        try {
            Files.createDirectories(root);
            Path target = root.resolve(storedName);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return new Stored(storedName, safeOriginalName(file), type.contentType, file.getSize());
        } catch (IOException e) {
            log.warn("이미지 저장 실패: {}", e.toString());
            throw new IllegalStateException("이미지를 저장하지 못했습니다.");
        }
    }

    /** 저장된 파일을 읽는다 — 이름으로만 접근한다 */
    public Resource load(String storedName) {
        Path target = root.resolve(storedName).normalize();

        // ★업로드 디렉터리 밖이면 거부★ (겹겹의 방어)
        if (!target.startsWith(root) || !Files.exists(target)) {
            throw new NoSuchElementException("이미지를 찾을 수 없습니다.");
        }
        return new FileSystemResource(target);
    }

    /**
     * 파일을 지운다. <b>실패해도 예외를 던지지 않는다.</b>
     *
     * <p>DB 행을 먼저 지우고 이 메서드를 부른다. 파일 삭제가 실패하면
     * 아무도 참조하지 않는 고아 파일이 남는데, 그게 <b>깨진 링크보다 낫다</b> —
     * 디스크만 조금 차지할 뿐 화면은 멀쩡하다.
     * 반대 순서라면 파일은 없는데 DB 에 남아 이미지가 깨져 보인다.
     */
    public void deleteQuietly(String storedName) {
        try {
            Path target = root.resolve(storedName).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            // 지우지 못한 파일이 무엇인지는 남긴다 — 나중에 정리할 수 있어야 한다
            log.warn("이미지 파일 삭제 실패 (고아 파일로 남음): {} / {}", storedName, e.toString());
        }
    }

    /**
     * 실제 내용을 읽어 형식을 판별한다.
     *
     * <p>클라이언트가 보낸 {@code Content-Type} 이나 확장자는 참고만 하고 믿지 않는다.
     */
    private ImageType detect(MultipartFile file) {
        byte[] head = new byte[8];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(head);
            if (read < 4) {
                throw new IllegalStateException("이미지 파일이 아닙니다.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("파일을 읽지 못했습니다.");
        }

        return Arrays.stream(ImageType.values())
                .filter(t -> matches(head, t.magic))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "JPG · PNG · GIF · WebP 만 올릴 수 있습니다."));
    }

    private boolean matches(byte[] head, int[] magic) {
        for (int i = 0; i < magic.length; i++) {
            if ((head[i] & 0xFF) != magic[i]) return false;
        }
        return true;
    }

    /**
     * 원본 이름에서 경로를 떼어낸다.
     *
     * <p>이 값은 화면 표시에만 쓰지만, 그래도 {@code ../} 같은 조각을 남겨 둘 이유가 없다.
     * 일부 브라우저는 전체 경로를 보내기도 한다.
     */
    private String safeOriginalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) return "image";

        // 윈도우·유닉스 구분자를 모두 자른다
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replace("..", "").trim();

        if (name.isBlank()) return "image";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    /** 허용 형식 목록 — 화면 안내에 쓴다 */
    public static List<String> allowedTypes() {
        return Arrays.stream(ImageType.values())
                .map(t -> t.extension.substring(1).toUpperCase(Locale.ROOT))
                .toList();
    }

    /** 저장 결과 */
    public record Stored(String storedName, String originalName, String contentType, long size) {}
}
