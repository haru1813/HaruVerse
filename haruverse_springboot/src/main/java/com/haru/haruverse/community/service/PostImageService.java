package com.haru.haruverse.community.service;

import com.haru.haruverse.community.dto.PostImageResponse;
import com.haru.haruverse.community.entity.Post;
import com.haru.haruverse.community.entity.PostImage;
import com.haru.haruverse.community.repository.PostImageRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.global.exception.ForbiddenException;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 게시글 첨부 이미지 — DB 와 파일을 함께 다룬다.
 *
 * <p>파일 자체는 {@link ImageStorage} 가 맡고, 여기서는 <b>누가 무엇에 붙일 수 있는지</b>와
 * <b>지우는 순서</b>를 정한다.
 */
@Service
public class PostImageService {

    private final PostImageRepository imageRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ImageStorage storage;
    private final int maxPerPost;

    public PostImageService(PostImageRepository imageRepository,
                            PostRepository postRepository,
                            MemberRepository memberRepository,
                            ImageStorage storage,
                            @Value("${upload.max-images-per-post}") int maxPerPost) {
        this.imageRepository = imageRepository;
        this.postRepository = postRepository;
        this.memberRepository = memberRepository;
        this.storage = storage;
        this.maxPerPost = maxPerPost;
    }

    @Transactional(readOnly = true)
    public List<PostImageResponse> getImages(Long postId) {
        return imageRepository.findByPostIdOrderByIdAsc(postId).stream()
                .map(PostImageResponse::from)
                .toList();
    }

    /**
     * 이미지를 글에 붙인다 — <b>글쓴이만</b>.
     *
     * <p>★파일을 먼저 저장하지 않는다★ 권한과 장수를 먼저 확인한다.
     * 순서가 반대면 거부당할 요청도 일단 디스크에 쓰게 되고,
     * 그 파일은 아무도 참조하지 않는 채로 남는다.
     */
    @Transactional
    public PostImageResponse attach(Long postId, String email, MultipartFile file) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다. id=" + postId));

        Member me = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        if (!post.isWrittenBy(me)) {
            throw new ForbiddenException("본인이 쓴 글에만 이미지를 붙일 수 있습니다.");
        }
        if (imageRepository.countByPostId(postId) >= maxPerPost) {
            throw new IllegalStateException("이미지는 글 하나에 %d장까지 붙일 수 있습니다.".formatted(maxPerPost));
        }

        ImageStorage.Stored stored = storage.save(file);
        PostImage image = imageRepository.save(new PostImage(
                post, stored.storedName(), stored.originalName(),
                stored.contentType(), stored.size()));

        return PostImageResponse.from(image);
    }

    /** 이미지 한 장 삭제 — 글쓴이만 */
    @Transactional
    public void detach(Long imageId, String email) {
        PostImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("이미지를 찾을 수 없습니다. id=" + imageId));

        Member me = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        if (!image.getPost().isWrittenBy(me)) {
            throw new ForbiddenException("본인이 쓴 글의 이미지만 지울 수 있습니다.");
        }

        String storedName = image.getStoredName();
        imageRepository.delete(image);
        // ★DB 를 먼저, 파일은 나중★ 파일 삭제가 실패하면 고아 파일이 남지만,
        //   반대 순서라면 파일은 없는데 DB 에 남아 이미지가 깨져 보인다.
        storage.deleteQuietly(storedName);
    }

    /** 글에 붙은 이미지를 전부 지운다 — 글 삭제 흐름에서 부른다 */
    @Transactional
    public void detachAll(Long postId) {
        List<String> names = imageRepository.findByPostIdOrderByIdAsc(postId).stream()
                .map(PostImage::getStoredName)
                .toList();

        imageRepository.deleteByPostId(postId);
        names.forEach(storage::deleteQuietly);
    }

    /** 파일 서빙 — 이름으로만 찾는다 */
    @Transactional(readOnly = true)
    public Served serve(String storedName) {
        PostImage image = imageRepository.findByStoredName(storedName)
                .orElseThrow(() -> new NoSuchElementException("이미지를 찾을 수 없습니다."));

        return new Served(storage.load(storedName), image.getContentType());
    }

    public record Served(Resource resource, String contentType) {}
}
