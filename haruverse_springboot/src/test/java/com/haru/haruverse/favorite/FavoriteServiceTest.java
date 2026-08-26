package com.haru.haruverse.favorite;

import com.haru.haruverse.favorite.repository.FavoriteRepository;
import com.haru.haruverse.favorite.service.FavoriteService;
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

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 찜 서비스 통합 테스트.
 *
 * <p>@Transactional을 붙이지 않는다 — 붙이면 테스트가 하나의 트랜잭션 안에서 돌아
 * 서비스의 트랜잭션 경계가 가려지고, 실제 커밋에서만 드러나는 문제를 놓친다.
 * 대신 매 테스트 시작 시 데이터를 정리한다.
 */
@SpringBootTest
class FavoriteServiceTest {

    @Autowired FavoriteService favoriteService;
    @Autowired FavoriteRepository favoriteRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired WorkRepository workRepository;

    private static final String EMAIL = "fav-test@haruverse.dev";
    private static final String OTHER = "fav-other@haruverse.dev";

    private Long workId;
    private Long otherWorkId;

    @BeforeEach
    void setUp() {
        // ★삭제 순서 주의★ 찜이 회원을 참조하므로 찜을 먼저 지운다.
        //   반대로 하면 FK(fk_favorite_member) 위반으로 커밋이 실패한다.
        favoriteRepository.deleteAll();
        memberRepository.findByEmail(EMAIL).ifPresent(memberRepository::delete);
        memberRepository.findByEmail(OTHER).ifPresent(memberRepository::delete);

        memberRepository.save(new Member(EMAIL, "encoded-dummy", "찜테스터"));
        memberRepository.save(new Member(OTHER, "encoded-dummy", "남"));

        workId = workRepository.save(new Work("찜 대상 작품", WorkType.ANIME, WorkSource.JIKAN)).getId();
        otherWorkId = workRepository.save(new Work("다른 작품", WorkType.GAME, WorkSource.RAWG)).getId();
    }

    @Test
    @DisplayName("찜하기 → 내 목록에 나온다")
    void addAndList() {
        assertThat(favoriteService.add(EMAIL, workId)).isTrue();

        var page = favoriteService.getMyFavorites(EMAIL, PageRequest.of(0, 24));
        assertThat(page.content()).extracting("title").containsExactly("찜 대상 작품");
    }

    @Test
    @DisplayName("★멱등★ 같은 작품을 두 번 찜해도 두 번째는 false, 데이터는 1건")
    void addIsIdempotent() {
        assertThat(favoriteService.add(EMAIL, workId)).isTrue();
        assertThat(favoriteService.add(EMAIL, workId)).isFalse(); // 아무 일도 일어나지 않음

        assertThat(favoriteRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("찜 해제 → 목록에서 빠진다")
    void remove() {
        favoriteService.add(EMAIL, workId);
        assertThat(favoriteService.remove(EMAIL, workId)).isTrue();

        assertThat(favoriteService.getMyFavorites(EMAIL, PageRequest.of(0, 24)).content()).isEmpty();
    }

    @Test
    @DisplayName("★멱등★ 찜이 아닌 것을 해제해도 예외가 아니라 false")
    void removeIsIdempotent() {
        assertThat(favoriteService.remove(EMAIL, workId)).isFalse();
    }

    @Test
    @DisplayName("찜한 작품 id 목록을 한 번에 받는다 (그리드 하트 표시용)")
    void favoriteIds() {
        favoriteService.add(EMAIL, workId);
        favoriteService.add(EMAIL, otherWorkId);

        assertThat(favoriteService.getMyFavoriteWorkIds(EMAIL))
                .containsExactlyInAnyOrder(workId, otherWorkId);
    }

    @Test
    @DisplayName("없는 작품을 찜하면 404로 이어지는 NoSuchElementException")
    void addMissingWork() {
        assertThatThrownBy(() -> favoriteService.add(EMAIL, 999_999L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("찜 해제는 남의 찜을 건드리지 않는다 (member_id 조건)")
    void removeOnlyMine() {
        favoriteService.add(EMAIL, workId);
        favoriteService.add(OTHER, workId);   // 같은 작품을 다른 회원도 찜

        favoriteService.remove(EMAIL, workId);

        assertThat(favoriteService.getMyFavoriteWorkIds(EMAIL)).isEmpty();
        assertThat(favoriteService.getMyFavoriteWorkIds(OTHER)).containsExactly(workId);
    }
}
