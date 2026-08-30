package com.haru.haruverse.admin;

import com.haru.haruverse.admin.entity.AdminAuditLog;
import com.haru.haruverse.admin.entity.AuditAction;
import com.haru.haruverse.admin.repository.AdminAuditLogRepository;
import com.haru.haruverse.admin.service.AdminCommunityService;
import com.haru.haruverse.admin.service.AdminMemberService;
import com.haru.haruverse.community.entity.Comment;
import com.haru.haruverse.community.entity.Post;
import com.haru.haruverse.community.repository.CommentRepository;
import com.haru.haruverse.community.repository.PostRepository;
import com.haru.haruverse.member.entity.Member;
import com.haru.haruverse.member.entity.MemberRole;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 관리자 감사 로그.
 *
 * <p>이 프로젝트의 삭제는 되돌릴 수 없다. 나중에 "이 글이 왜 없지"를 물었을 때
 * 답할 수 있는 유일한 흔적이 이 표다. 그래서 고정하는 건 세 가지다.
 * <ol>
 *   <li>지운 <b>대상의 정보가 남는가</b> — 원본이 사라진 뒤에도 읽을 수 있어야 한다</li>
 *   <li><b>누가</b> 했는지 남는가</li>
 *   <li><b>실패한 일이 기록되지 않는가</b> — 로그가 거짓말을 하면 안 된다</li>
 * </ol>
 */
@SpringBootTest
@Transactional
class AuditLogTest {

    @Autowired AdminCommunityService communityService;
    @Autowired AdminMemberService memberService;
    @Autowired AdminAuditLogRepository auditRepository;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired WorkRepository workRepository;

    private static final String ADMIN = "audit-admin@haru.test";

    private Post post;
    private Member target;

    @BeforeEach
    void setUp() {
        Member author = memberRepository.save(new Member("audit-author@haru.test", "x", "감사글쓴이"));
        target = memberRepository.save(new Member("audit-target@haru.test", "x", "감사대상"));

        Work work = workRepository.save(new Work("감사테스트작품", WorkType.ANIME, WorkSource.JIKAN));
        post = postRepository.save(new Post(work, author, "감사 테스트 글", "본문"));
        commentRepository.save(new Comment(post, author, "감사 테스트 댓글"));
    }

    private List<AdminAuditLog> logs() {
        return auditRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50)).getContent();
    }

    @Test
    @DisplayName("★글을 지우면 제목·작성자가 기록에 남는다★ (원본은 이미 없다)")
    void postDeletionKeepsDetails() {
        communityService.deletePost(post.getId(), ADMIN);

        AdminAuditLog log = logs().stream()
                .filter(l -> l.getAction() == AuditAction.DELETE_POST)
                .filter(l -> l.getTargetId().equals(post.getId()))
                .findFirst().orElseThrow();

        assertThat(log.getActorEmail()).isEqualTo(ADMIN);
        // 원본은 지워졌지만 무엇이었는지 알 수 있어야 한다
        assertThat(log.getSummary()).contains("감사 테스트 글");
        assertThat(log.getSummary()).contains("감사글쓴이");
        assertThat(log.getSummary()).contains("감사테스트작품");
        // 함께 사라진 댓글 수까지
        assertThat(log.getSummary()).contains("1개");

        assertThat(postRepository.findById(post.getId())).isEmpty();
    }

    @Test
    @DisplayName("댓글 삭제도 내용과 원글이 남는다")
    void commentDeletionKeepsDetails() {
        Comment comment = commentRepository.findByPostIdWithMember(post.getId()).get(0);

        communityService.deleteComment(comment.getId(), ADMIN);

        AdminAuditLog log = logs().stream()
                .filter(l -> l.getAction() == AuditAction.DELETE_COMMENT)
                .findFirst().orElseThrow();

        assertThat(log.getSummary()).contains("감사 테스트 댓글");
        assertThat(log.getSummary()).contains("감사 테스트 글");
    }

    @Test
    @DisplayName("권한 변경은 바뀌기 전후가 함께 남는다")
    void roleChangeKeepsBeforeAndAfter() {
        memberService.changeRole(target.getId(), MemberRole.ADMIN, ADMIN);

        AdminAuditLog log = logs().stream()
                .filter(l -> l.getAction() == AuditAction.CHANGE_ROLE)
                .findFirst().orElseThrow();

        assertThat(log.getActorEmail()).isEqualTo(ADMIN);
        assertThat(log.getSummary()).contains("감사대상");
        assertThat(log.getSummary()).contains("USER");
        assertThat(log.getSummary()).contains("ADMIN");
    }

    @Test
    @DisplayName("★막힌 요청은 기록되지 않는다★ — 일어나지 않은 일이 남으면 안 된다")
    void blockedActionLeavesNoTrace() {
        int before = logs().size();

        // 자물쇠 ① 자기 자신의 권한은 바꿀 수 없다
        assertThatThrownBy(() ->
                memberService.changeRole(target.getId(), MemberRole.ADMIN, target.getEmail()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(logs()).hasSize(before);
    }

    @Test
    @DisplayName("★기록은 지우기 '전'에 만들어야 한다★ (순서가 뒤바뀌면 빈 요약이 남는다)")
    void summaryIsBuiltBeforeDeletion() {
        Long postId = post.getId();
        String title = post.getTitle();

        communityService.deletePost(postId, ADMIN);

        // 삭제 후에 요약을 만들었다면 제목을 읽을 수 없어 비어 있었을 것이다
        AdminAuditLog log = logs().get(0);
        assertThat(log.getSummary()).isNotBlank();
        assertThat(log.getSummary()).contains(title);
    }
}
