package com.haru.haruverse.community;

import com.haru.haruverse.community.entity.Post;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.community.service.PostService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게시글 검색.
 *
 * <p>제목·본문·작성자·작품명 네 곳에서 찾는다.
 * <b>Elasticsearch 를 쓰지 않는다</b> — 사용자가 찾는 건 방금 본 그 글이고,
 * 제목이나 본문에 실제로 들어 있는 말로 찾는다. 그 판단의 근거는
 * {@code PostRepository.findRecentSummaries} 주석에 있다.
 */
@SpringBootTest
@Transactional
class PostSearchTest {

    @Autowired PostService postService;
    @Autowired PostRepository postRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired WorkRepository workRepository;

    @BeforeEach
    void setUp() {
        Member alice = memberRepository.save(new Member("search-a@haru.test", "x", "검색앨리스"));
        Member bob = memberRepository.save(new Member("search-b@haru.test", "x", "검색밥"));

        Work frieren = workRepository.save(
                new Work("검색용프리렌", WorkType.ANIME, WorkSource.JIKAN));
        Work elden = workRepository.save(
                new Work("검색용엘든링", WorkType.GAME, WorkSource.RAWG));

        postRepository.save(new Post(frieren, alice, "마법 연출이 좋다", "빛 표현이 특히 인상적이었다"));
        postRepository.save(new Post(elden, bob, "보스가 어렵다", "패턴을 외우는 게 답이다"));
    }

    private List<String> titles(String keyword) {
        return postService.getRecentPosts(keyword, PageRequest.of(0, 50))
                .content().stream()
                .map(r -> r.title())
                .toList();
    }

    @Test
    @DisplayName("제목으로 찾는다")
    void byTitle() {
        assertThat(titles("마법 연출")).contains("마법 연출이 좋다");
    }

    @Test
    @DisplayName("본문으로도 찾는다 (제목에 없는 말)")
    void byContent() {
        // '빛 표현'은 제목에 없다 — 본문을 뒤지지 않으면 0건이다
        assertThat(titles("빛 표현")).contains("마법 연출이 좋다");
    }

    @Test
    @DisplayName("작성자 닉네임으로 찾는다")
    void byAuthor() {
        assertThat(titles("검색밥")).contains("보스가 어렵다");
    }

    @Test
    @DisplayName("★작품 이름으로도 찾는다★ — '그 게시판의 글'을 찾는 흐름")
    void byWorkTitle() {
        assertThat(titles("검색용엘든링")).contains("보스가 어렵다");
        assertThat(titles("검색용엘든링")).doesNotContain("마법 연출이 좋다");
    }

    @Test
    @DisplayName("대소문자를 가리지 않는다")
    void ignoresCase() {
        Member m = memberRepository.findByEmail("search-a@haru.test").orElseThrow();
        Work w = workRepository.save(new Work("CaseTest", WorkType.ANIME, WorkSource.JIKAN));
        postRepository.save(new Post(w, m, "Elden Ring 후기", "본문"));

        assertThat(titles("elden ring")).contains("Elden Ring 후기");
        assertThat(titles("ELDEN RING")).contains("Elden Ring 후기");
    }

    @Test
    @DisplayName("★검색어가 없으면 전체 목록이다★ (빈 문자열도 마찬가지)")
    void blankReturnsAll() {
        int all = titles(null).size();

        assertThat(all).isGreaterThanOrEqualTo(2);
        assertThat(titles("")).hasSize(all);
        assertThat(titles("   ")).hasSize(all);
    }

    @Test
    @DisplayName("없는 말은 0건")
    void noMatch() {
        assertThat(titles("존재하지않는검색어xyz")).isEmpty();
    }
}
