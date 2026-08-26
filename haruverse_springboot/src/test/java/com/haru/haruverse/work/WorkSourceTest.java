package com.haru.haruverse.work;

import com.haru.haruverse.work.entity.WorkSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * externalId 조합·해석 규칙.
 *
 * <p>이 규칙을 몰라서 캐릭터 수집이 86건 전부 건너뛰어진 적이 있다
 * ("jikan-52991"을 그대로 Long.parseLong에 넘겼다).
 * 조합과 해석이 짝을 이루는지 여기서 못 박는다.
 */
class WorkSourceTest {

    @Test
    @DisplayName("externalId는 접두사가 붙은 문자열이다 (순수 숫자가 아니다)")
    void format() {
        assertThat(WorkSource.JIKAN.externalId(52991)).isEqualTo("jikan-52991");
        assertThat(WorkSource.RAWG.externalId(3498)).isEqualTo("rawg-3498");
    }

    @Test
    @DisplayName("조합한 값을 그대로 되돌릴 수 있다 (왕복)")
    void roundTrip() {
        for (long id : new long[]{1L, 52991L, 999_999L}) {
            assertThat(WorkSource.JIKAN.extractExternalKey(WorkSource.JIKAN.externalId(id))).isEqualTo(id);
            assertThat(WorkSource.RAWG.extractExternalKey(WorkSource.RAWG.externalId(id))).isEqualTo(id);
        }
    }

    @Test
    @DisplayName("다른 출처의 식별자를 넘기면 null (rawg- 를 JIKAN으로 해석하지 않는다)")
    void wrongSource() {
        assertThat(WorkSource.JIKAN.extractExternalKey("rawg-3498")).isNull();
        assertThat(WorkSource.RAWG.extractExternalKey("jikan-52991")).isNull();
    }

    @Test
    @DisplayName("형식이 깨져도 예외가 아니라 null (수집이 한 건 때문에 멈추지 않게)")
    void malformed() {
        assertThat(WorkSource.JIKAN.extractExternalKey(null)).isNull();
        assertThat(WorkSource.JIKAN.extractExternalKey("")).isNull();
        assertThat(WorkSource.JIKAN.extractExternalKey("52991")).isNull();     // 접두사 없음
        assertThat(WorkSource.JIKAN.extractExternalKey("jikan-abc")).isNull(); // 숫자 아님
        assertThat(WorkSource.JIKAN.extractExternalKey("jikan-")).isNull();
    }
}
