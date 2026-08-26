package com.haru.haruverse.community;

import com.haru.haruverse.community.dto.ChannelResponse;
import com.haru.haruverse.community.dto.PostRequest;
import com.haru.haruverse.community.dto.PostSummaryResponse;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostLikeRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.community.repository.SubscriptionRepository;
import com.haru.haruverse.community.service.PostService;
import com.haru.haruverse.community.service.SubscriptionService;
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
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 채널 구독.
 *
 * <p>@Transactional을 붙이지 않는다 — 붙이면 테스트가 한 트랜잭션으로 묶여
 * 서비스의 트랜잭션 경계와 실제 커밋에서만 드러나는 문제를 놓친다.
 * (구독 해제의 파생 delete 쿼리가 @Transactional 없이 터지는 문제도 여기서만 잡힌다)
 */
@SpringBootTest
class SubscriptionServiceTest {

    @Autowired SubscriptionService subscriptionService;
    @Autowired PostService postService;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired PostLikeRepository postLikeRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired WorkRepository workRepository;

    private static final String ME = "sub-me@haru.test";
    private static final String OTHER = "sub-other@haru.test";
    private static final String PREFIX = "구독테스트";

    private Long workA;
    private Long workB;

    @BeforeEach
    void setUp() {
        cleanUp();

        memberRepository.findByEmail(ME).orElseGet(
                () -> memberRepository.save(new Member(ME, "encoded", "구독자")));
        memberRepository.findByEmail(OTHER).orElseGet(
                () -> memberRepository.save(new Member(OTHER, "encoded", "다른사람")));

        workA = workRepository.save(new Work(PREFIX + " 작품A", WorkType.ANIME, WorkSource.MANUAL)).getId();
        workB = workRepository.save(new Work(PREFIX + " 작품B", WorkType.GAME, WorkSource.MANUAL)).getId();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    /** ★삭제 순서★ 구독·추천·댓글 → 글 → 작품 (참조하는 쪽부터) */
    private void cleanUp() {
        workRepository.findAll().stream()
                .filter(w -> w.getTitle().startsWith(PREFIX))
                .forEach(w -> {
                    subscriptionRepository.findAll().stream()
                            .filter(s -> s.getWork().getId().equals(w.getId()))
                            .forEach(subscriptionRepository::delete);
                    postRepository.findSummariesByWorkId(w.getId(), PageRequest.of(0, 500))
                            .forEach(p -> {
                                postLikeRepository.deleteByPostId(p.id());
                                commentRepository.deleteByPostId(p.id());
                                postRepository.deleteById(p.id());
                            });
                    workRepository.delete(w);
                });
    }

    private List<ChannelResponse> myChannels() {
        return subscriptionService.getMyChannels(ME).stream()
                .filter(c -> c.workTitle().startsWith(PREFIX))
                .toList();
    }

    /* ── 구독·해제 ─────────────────────────────────────── */

    @Test
    @DisplayName("구독하면 내 구독 목록에 나온다")
    void subscribeAndList() {
        subscriptionService.subscribe(ME, workA);

        assertThat(myChannels()).extracting(ChannelResponse::workId).containsExactly(workA);
    }

    @Test
    @DisplayName("★구독은 멱등★ 두 번 눌러도 하나만 남는다")
    void subscribeIsIdempotent() {
        assertThat(subscriptionService.subscribe(ME, workA)).isTrue();  // 새로 구독
        assertThat(subscriptionService.subscribe(ME, workA)).isFalse(); // 이미 구독 중

        assertThat(myChannels()).hasSize(1);
    }

    @Test
    @DisplayName("★해제도 멱등★ 원래 구독이 아니어도 성공한다")
    void unsubscribeIsIdempotent() {
        subscriptionService.subscribe(ME, workA);

        assertThat(subscriptionService.unsubscribe(ME, workA)).isTrue();
        assertThat(subscriptionService.unsubscribe(ME, workA)).isFalse(); // 이미 없음

        assertThat(myChannels()).isEmpty();
    }

    @Test
    @DisplayName("없는 작품은 구독할 수 없다")
    void subscribeMissingWork() {
        assertThatThrownBy(() -> subscriptionService.subscribe(ME, 99_999_999L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("★남의 구독은 내 목록에 섞이지 않는다★")
    void otherMembersSubscriptionIsolated() {
        subscriptionService.subscribe(OTHER, workA);
        subscriptionService.subscribe(ME, workB);

        assertThat(myChannels()).extracting(ChannelResponse::workId).containsExactly(workB);
    }

    @Test
    @DisplayName("구독한 작품 id 목록을 준다 (버튼 상태용)")
    void myWorkIds() {
        subscriptionService.subscribe(ME, workA);

        assertThat(subscriptionService.getMyWorkIds(ME)).contains(workA).doesNotContain(workB);
    }

    /* ── 채널 카드 ─────────────────────────────────────── */

    @Test
    @DisplayName("★글이 하나도 없는 채널도 구독 목록에 나온다★ (글 수 0, 최신 글 null)")
    void emptyChannelStillListed() {
        subscriptionService.subscribe(ME, workA); // 글을 쓰지 않는다

        List<ChannelResponse> channels = myChannels();

        assertThat(channels).hasSize(1);
        assertThat(channels.get(0).postCount()).isZero();
        // 커뮤니티 첫 화면(findChannelStats)은 글 있는 채널만 보여주지만,
        // 구독 목록은 "내가 구독한 것"이 기준이라 빈 채널도 나와야 한다
        assertThat(channels.get(0).latestPostId()).isNull();
        assertThat(channels.get(0).latestPostTitle()).isNull();
        assertThat(channels.get(0).latestPostAuthor()).isNull();
    }

    @Test
    @DisplayName("채널 카드에 글 수와 가장 최근 글이 담긴다")
    void channelCarriesLatestPost() {
        subscriptionService.subscribe(ME, workA);
        postService.createPost(workA, ME, new PostRequest("옛날 글", "내용"));
        postService.createPost(workA, ME, new PostRequest("최근 글", "내용"));

        ChannelResponse channel = myChannels().get(0);

        assertThat(channel.postCount()).isEqualTo(2);
        assertThat(channel.latestPostTitle()).isEqualTo("최근 글");
        assertThat(channel.latestPostAuthor()).isEqualTo("구독자");
    }

    @Test
    @DisplayName("★글이 있는 채널이 먼저, 빈 채널은 뒤로★")
    void channelsWithPostsComeFirst() {
        // 빈 채널을 나중에 구독한다 — 구독 순서대로면 이게 위로 온다
        subscriptionService.subscribe(ME, workA);
        postService.createPost(workA, ME, new PostRequest("글 있음", "내용"));
        subscriptionService.subscribe(ME, workB);

        assertThat(myChannels()).extracting(ChannelResponse::workId)
                .containsExactly(workA, workB);
    }

    @Test
    @DisplayName("최근 글이 올라온 채널이 위로 온다")
    void recentlyActiveChannelFirst() {
        subscriptionService.subscribe(ME, workA);
        subscriptionService.subscribe(ME, workB);
        postService.createPost(workA, ME, new PostRequest("A 글", "내용"));
        postService.createPost(workB, ME, new PostRequest("B 글", "내용")); // 더 최근

        assertThat(myChannels()).extracting(ChannelResponse::workId)
                .containsExactly(workB, workA);
    }

    @Test
    @DisplayName("구독이 하나도 없으면 빈 목록 (in () 문법 오류가 나지 않는다)")
    void noSubscriptions() {
        // ★빈 목록을 in 절에 넘기면 DB에 따라 "in ()" 문법 오류★ → 서비스가 미리 끊는다
        assertThat(subscriptionService.getMyChannels(ME)).isEmpty();
    }

    /* ── 구독과 글은 별개 ──────────────────────────────── */

    @Test
    @DisplayName("구독하지 않은 채널에도 글은 쓸 수 있다 (구독은 읽기 편의일 뿐)")
    void canPostWithoutSubscribing() {
        postService.createPost(workA, ME, new PostRequest("구독 안 해도 씀", "내용"));

        List<PostSummaryResponse> posts = postService.getPosts(workA, PageRequest.of(0, 20)).content();
        assertThat(posts).extracting(PostSummaryResponse::title).containsExactly("구독 안 해도 씀");
        assertThat(myChannels()).isEmpty();
    }
}
