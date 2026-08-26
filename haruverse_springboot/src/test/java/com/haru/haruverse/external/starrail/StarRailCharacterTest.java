package com.haru.haruverse.external.starrail;

import com.haru.haruverse.external.starrail.dto.StarRailCharacter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스타레일 캐릭터 이름 처리.
 *
 * <p>실제 데이터(2026-08-25 확인)에서 주인공 10명(id 8001~8010)의 이름이
 * {@code "{NICKNAME}"} 자리표시자로 들어온다. 그대로 저장하면 도감이 망가진다.
 */
class StarRailCharacterTest {

    private StarRailCharacter of(String id, String name, String element) {
        return new StarRailCharacter(id, name, 5, "Warrior", element, null, null, null);
    }

    @Test
    @DisplayName("보통 캐릭터는 이름을 그대로 쓴다")
    void normalName() {
        assertThat(of("1001", "March 7th", "Ice").displayName()).isEqualTo("March 7th");
    }

    @Test
    @DisplayName("★주인공은 '{NICKNAME}'으로 온다★ → 속성을 붙여 구분한다")
    void trailblazerPlaceholder() {
        assertThat(of("8001", "{NICKNAME}", "Physical").displayName()).isEqualTo("Trailblazer (Physical)");
        assertThat(of("8003", "{NICKNAME}", "Fire").displayName()).isEqualTo("Trailblazer (Fire)");
    }

    @Test
    @DisplayName("속성이 없으면 통칭만")
    void trailblazerWithoutElement() {
        assertThat(of("8001", "{NICKNAME}", null).displayName()).isEqualTo("Trailblazer");
        assertThat(of("8001", "{NICKNAME}", "").displayName()).isEqualTo("Trailblazer");
    }

    @Test
    @DisplayName("이름이 비어 있으면 저장하지 않는다")
    void invalid() {
        assertThat(of("1001", null, "Ice").isValid()).isFalse();
        assertThat(of("1001", "  ", "Ice").isValid()).isFalse();
        assertThat(of(null, "March 7th", "Ice").isValid()).isFalse();
        // 자리표시자여도 저장은 된다 (이름을 만들어 주므로)
        assertThat(of("8001", "{NICKNAME}", "Physical").isValid()).isTrue();
    }
}
