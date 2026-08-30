package com.haru.haruverse.member.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// 회원 엔티티 — 'member' 테이블과 매핑.
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password; // 반드시 '인코딩된' 값만 저장 (평문 금지)

    @Column(nullable = false, length = 30)
    private String nickname;

    /**
     * 권한. 가입하면 USER, 승격은 DB에서 직접 한다(관리자 화면이 아직 없다).
     *
     * <p>★columnDefinition 에 default 를 넣은 이유★
     * ddl-auto=update 는 이미 행이 있는 표에 <b>NOT NULL 컬럼을 그냥 못 붙인다</b>.
     * 기존 행을 뭘로 채울지 모르기 때문인데, Hibernate 는 이때 <b>조용히 건너뛴다</b>
     * (에러도 안 난다 — 컬럼이 없는 채로 앱이 뜨고 나중에 엉뚱한 곳에서 터진다).
     * DB 기본값을 함께 주면 기존 행이 'USER' 로 채워지면서 추가에 성공한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) not null default 'USER'")
    private MemberRole role = MemberRole.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Member() {} // JPA용 기본 생성자 (직접 호출 금지)

    public Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 권한을 바꾼다 — 관리자 콘솔의 회원 관리에서만 부른다.
     *
     * <p><b>★여기에는 아무 검사도 없다★</b>
     * "자기 자신은 못 바꾼다", "마지막 관리자는 못 내린다" 같은 규칙은
     * {@code AdminMemberService} 에 있다. 엔티티는 <b>누가 부르는지</b>를 모르기 때문이다
     * (로그인한 사람이 누군지, 관리자가 몇 명인지 알 수 없다).
     * 그래서 이 메서드는 절대 컨트롤러에서 바로 부르지 않는다.
     */
    public void changeRole(MemberRole role) {
        this.role = role;
    }

    /**
     * 비밀번호를 바꾼다.
     *
     * <p><b>★반드시 인코딩된 값만 넘긴다★</b>
     * 이 메서드는 받은 문자열을 그대로 저장한다 — 평문이 들어오면 평문이 저장된다.
     * 인코딩은 {@code PasswordEncoder} 를 가진 서비스가 하고,
     * 현재 비밀번호 확인도 거기서 한다(엔티티는 인코더를 모른다).
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // TODO(하루): 필요 시 프로필 이미지 등 필드 추가

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
    public MemberRole getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
