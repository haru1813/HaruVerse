package com.haru.haruverse.community.mapper;

import com.haru.haruverse.community.dto.ChannelResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 커뮤니티 채널 목록 — <b>MyBatis</b>.
 *
 * <p><b>★왜 이것만 MyBatis 인가★</b>
 * 이 프로젝트의 원칙은 <b>쓰기는 JPA, 어려운 읽기는 MyBatis</b> 다.
 * 엔티티 생명주기(저장·수정·삭제·더티 체킹)는 JPA 가 훨씬 잘하고,
 * 여러 테이블을 엮어 집계하는 조회는 SQL 을 직접 쓰는 편이 짧고 빠르다.
 *
 * <p>채널 목록이 그 경계에 정확히 걸린다. 필요한 건 <b>"작품별 최신 글 한 건 + 글 수"</b>인데,
 * JPQL 로는 한 번에 못 가져온다(서브쿼리에 limit 을 못 쓴다).
 * 그래서 예전에는 ① 작품별 글 수·최신 글 id 를 집계하고 ② 그 id 들을 다시 읽는
 * <b>두 단계</b>로 우회했다. 쿼리가 두 번 나가고, 두 결과를 코드에서 다시 엮어야 했다.
 *
 * <p>윈도우 함수({@code ROW_NUMBER}, {@code COUNT OVER})를 쓰면 <b>한 번</b>에 끝난다.
 * MariaDB 10.2+ 와 H2 모두 지원한다.
 *
 * <p><b>★정렬 기준은 created_at 이 아니라 id 다★</b>
 * 같은 시각에 쓰인 글이 둘이면 시각으로는 최신 한 건을 특정할 수 없어 카드가 중복된다.
 * id 는 auto increment 라 최신 글이 곧 최대값이다. (JPA 버전도 {@code max(p.id)} 를 썼다)
 */
@Mapper
public interface ChannelMapper {

    /**
     * 글이 있는 작품만, 각 채널의 최신 글과 함께.
     *
     * @param limit  한 페이지 크기
     * @param offset 건너뛸 개수
     */
    List<ChannelResponse> findChannels(@Param("limit") int limit, @Param("offset") int offset);

    /** 전체 채널 수 — 페이징 계산용 */
    long countChannels();
}
