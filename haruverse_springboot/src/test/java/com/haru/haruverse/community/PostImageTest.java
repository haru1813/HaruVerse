package com.haru.haruverse.community;

import com.haru.haruverse.community.entity.Post;
import com.haru.haruverse.community.repository.PostImageRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.community.service.PostImageService;
import com.haru.haruverse.global.exception.ForbiddenException;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 게시글 첨부 이미지.
 *
 * <p>여기서 고정하는 건 대부분 <b>보안</b>이다. 파일 업로드는 사용자가 서버 디스크에
 * 무언가를 쓰게 하는 일이라, 막을 곳을 하나만 빠뜨려도 통로가 된다.
 * <ol>
 *   <li><b>확장자·Content-Type 을 믿지 않는다</b> — 실제 내용을 본다</li>
 *   <li><b>파일명은 서버가 짓는다</b> — 경로 탐색과 덮어쓰기를 원천 차단</li>
 *   <li><b>남의 글에는 못 붙인다</b></li>
 *   <li><b>장수 제한</b></li>
 * </ol>
 */
@SpringBootTest
@Transactional
class PostImageTest {

    @Autowired PostImageService imageService;
    @Autowired PostImageRepository imageRepository;
    @Autowired PostRepository postRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired WorkRepository workRepository;

    private static final String OWNER = "img-owner@haru.test";
    private static final String OTHER = "img-other@haru.test";

    private Post post;

    /** 진짜 PNG — 매직 넘버 89 50 4E 47 로 시작한다 */
    private static MockMultipartFile png(String filename) {
        byte[] content = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01
        };
        return new MockMultipartFile("file", filename, "image/png", content);
    }

    @BeforeEach
    void setUp() {
        Member owner = memberRepository.save(new Member(OWNER, "x", "이미지주인"));
        memberRepository.save(new Member(OTHER, "x", "남"));

        Work work = workRepository.save(new Work("이미지테스트", WorkType.ANIME, WorkSource.JIKAN));
        post = postRepository.save(new Post(work, owner, "이미지 글", "본문"));
    }

    @Test
    @DisplayName("이미지를 붙이면 목록에 나온다")
    void attaches() {
        var saved = imageService.attach(post.getId(), OWNER, png("사진.png"));

        assertThat(saved.originalName()).isEqualTo("사진.png");
        assertThat(saved.url()).startsWith("/api/images/");
        assertThat(imageService.getImages(post.getId())).hasSize(1);
    }

    @Test
    @DisplayName("★파일명을 서버가 새로 짓는다★ — 원본 이름은 표시용으로만 남는다")
    void generatesOwnFilename() {
        var saved = imageService.attach(post.getId(), OWNER, png("내 사진 (1).png"));

        String storedName = imageRepository.findAll().stream()
                .filter(i -> i.getId().equals(saved.id()))
                .findFirst().orElseThrow()
                .getStoredName();

        assertThat(storedName).doesNotContain("내 사진");
        assertThat(storedName).doesNotContain(" ");
        assertThat(storedName).endsWith(".png");
        assertThat(storedName).hasSize(36); // 32(hex) + ".png"
    }

    @Test
    @DisplayName("★경로 탐색 시도가 이름에 남지 않는다★")
    void pathTraversalIsNeutralized() {
        var saved = imageService.attach(post.getId(), OWNER, png("../../../etc/passwd.png"));

        assertThat(saved.originalName()).doesNotContain("..");
        assertThat(saved.originalName()).doesNotContain("/");

        String storedName = imageRepository.findAll().get(0).getStoredName();
        assertThat(storedName).doesNotContain("..");
        assertThat(storedName).doesNotContain("/");
    }

    @Test
    @DisplayName("★확장자만 이미지인 파일은 거부한다★ (내용을 직접 본다)")
    void rejectsFakeImage() {
        var fake = new MockMultipartFile(
                "file", "evil.png", "image/png",
                "not an image at all, just text".getBytes());

        assertThatThrownBy(() -> imageService.attach(post.getId(), OWNER, fake))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("올릴 수 있습니다");

        assertThat(imageService.getImages(post.getId())).isEmpty();
    }

    @Test
    @DisplayName("빈 파일은 거부한다")
    void rejectsEmpty() {
        var empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> imageService.attach(post.getId(), OWNER, empty))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("★남의 글에는 붙일 수 없다★")
    void onlyAuthorCanAttach() {
        assertThatThrownBy(() -> imageService.attach(post.getId(), OTHER, png("a.png")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("★장수 제한을 넘으면 거부한다★ (설정값 5장)")
    void enforcesLimit() {
        for (int i = 0; i < 5; i++) {
            imageService.attach(post.getId(), OWNER, png("p" + i + ".png"));
        }

        assertThatThrownBy(() -> imageService.attach(post.getId(), OWNER, png("여섯번째.png")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("5장");
    }

    @Test
    @DisplayName("남의 글 이미지는 지울 수 없다")
    void onlyAuthorCanDelete() {
        var saved = imageService.attach(post.getId(), OWNER, png("a.png"));

        assertThatThrownBy(() -> imageService.detach(saved.id(), OTHER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("★글을 지우면 이미지도 함께 사라진다★")
    void deletingPostRemovesImages() {
        imageService.attach(post.getId(), OWNER, png("a.png"));
        imageService.attach(post.getId(), OWNER, png("b.png"));
        assertThat(imageRepository.countByPostId(post.getId())).isEqualTo(2);

        imageService.detachAll(post.getId());

        assertThat(imageRepository.countByPostId(post.getId())).isZero();
    }
}
