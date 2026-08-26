package com.haru.haruverse.community;

import com.haru.haruverse.community.dto.ChannelResponse;
import com.haru.haruverse.community.dto.PostRequest;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostLikeRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.community.service.PostService;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.repository.MemberRepository;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkSource;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 커뮤니티 첫 화면 — 채널 목록.
 *
 * <p>채널은 작품이고, <b>글이 하나라도 있는 작품만</b> 카드가 된다.
 */
@SpringBootTest
class ChannelListTest {

    @Autowired PostService postService;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired PostLikeRepository postLikeRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired WorkRepository workRepository;

    private static final String EMAIL = "channel-test@haru.test";

    private Long workA, workB, workEmpty;

    @BeforeEach
    void setUp() {
        cleanUp();
        memberRepository.findByEmail(EMAIL).orElseGet(
                () -> memberRepository.save(new Member(EMAIL, "encoded", "채널테스터")));

        workA = workRepository.save(new Work("채널테스트 작품A", WorkType.ANIME, WorkSource.MANUAL)).getId();
        workB = workRepository.save(new Work("채널테스트 작품B", WorkType.ANIME, WorkSource.MANUAL)).getId();
        // 글을 하나도 쓰지 않을 작품 — 목록에 나오면 안 된다
        workEmpty = workRepository.save(new Work("채널테스트 빈작품", WorkType.ANIME, WorkSource.MANUAL)).getId();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    /** ★삭제 순서★ 추천·댓글 → 글 → 작품 */
    private void cleanUp() {
        workRepository.findAll().stream()
                .filter(w -> w.getTitle().startsWith("채널테스트"))
                .forEach(w -> {
                    postRepository.findSummariesByWorkId(w.getId(), PageRequest.of(0, 500))
                            .forEach(p -> {
                                postLikeRepository.deleteByPostId(p.id());
                                commentRepository.deleteByPostId(p.id());
                                postRepository.deleteById(p.id());
                            });
                    workRepository.delete(w);
                });
    }

    private List<ChannelResponse> mine() {
        return postService.getChannels(PageRequest.of(0, 100)).content().stream()
                .filter(c -> c != null && c.workTitle().startsWith("채널테스트"))
                .toList();
    }

    @Test
    @DisplayName("★글이 있는 채널만 나온다★ (빈 작품은 카드가 없다)")
    void onlyChannelsWithPosts() {
        postService.createPost(workA, EMAIL, new PostRequest("A의 글", "내용"));

        assertThat(mine()).extracting(ChannelResponse::workTitle)
                .containsExactly("채널테스트 작품A")
                .doesNotContain("채널테스트 빈작품");
    }

    @Test
    @DisplayName("카드에 가장 최근 글이 담긴다")
    void cardCarriesLatestPost() {
        postService.createPost(workA, EMAIL, new PostRequest("예전 글", "내용"));
        postService.createPost(workA, EMAIL, new PostRequest("최근 글", "내용"));

        ChannelResponse card = mine().get(0);
        assertThat(card.latestPostTitle()).isEqualTo("최근 글");
        assertThat(card.latestPostAuthor()).isEqualTo("채널테스터");
        assertThat(card.latestPostId()).isNotNull();
    }

    @Test
    @DisplayName("글 수가 맞다")
    void postCount() {
        postService.createPost(workA, EMAIL, new PostRequest("1", "내용"));
        postService.createPost(workA, EMAIL, new PostRequest("2", "내용"));
        postService.createPost(workB, EMAIL, new PostRequest("3", "내용"));

        assertThat(mine()).extracting(ChannelResponse::workTitle, ChannelResponse::postCount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("채널테스트 작품A", 2L),
                        org.assertj.core.groups.Tuple.tuple("채널테스트 작품B", 1L));
    }

    @Test
    @DisplayName("최근 글이 있는 채널이 앞에 온다")
    void orderedByLatest() {
        postService.createPost(workA, EMAIL, new PostRequest("A 글", "내용"));
        postService.createPost(workB, EMAIL, new PostRequest("B 글", "내용")); // 더 최근

        assertThat(mine()).extracting(ChannelResponse::workTitle)
                .containsExactly("채널테스트 작품B", "채널테스트 작품A");
    }

    @Test
    @DisplayName("★같은 채널이 두 번 나오지 않는다★ (글이 여러 개여도 카드는 하나)")
    void noDuplicateChannel() {
        for (int i = 0; i < 5; i++) {
            postService.createPost(workA, EMAIL, new PostRequest("글" + i, "내용"));
        }
        assertThat(mine()).hasSize(1);
    }

    @Test
    @DisplayName("글이 하나도 없으면 빈 목록 (in () 문법 오류가 나지 않는다)")
    void emptyIsSafe() {
        // 이 테스트가 만든 글이 없는 상태 — 전체가 비어 있을 수도 있다
        assertThat(mine()).isEmpty();
    }
}
