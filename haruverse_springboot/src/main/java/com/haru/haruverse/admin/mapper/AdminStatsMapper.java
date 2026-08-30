package com.haru.haruverse.admin.mapper;

import com.haru.haruverse.admin.dto.StatsCounts;
import org.apache.ibatis.annotations.Mapper;

/**
 * 관리자 대시보드 통계 — <b>MyBatis</b>.
 *
 * <p><b>★열두 번을 한 번으로★</b>
 * 예전에는 리포지토리의 {@code count()} 를 <b>12번</b> 불렀다.
 * 화면 한 장을 그리려고 DB 를 열두 번 왕복한 셈이다.
 * 스칼라 서브쿼리로 묶으면 한 번에 끝난다.
 *
 * <p>JPA 로도 못 할 건 없지만(네이티브 쿼리) 그러면 결국 SQL 을 문자열로 쓰게 된다.
 * 이 프로젝트의 원칙대로 <b>어려운 읽기는 MyBatis</b> 에 맡긴다.
 *
 * <p>통계는 <b>순수 조회</b>다. 엔티티를 만들지도, 상태를 바꾸지도 않는다 —
 * JPA 가 잘하는 일이 하나도 필요 없는 자리다.
 */
@Mapper
public interface AdminStatsMapper {

    /** 대시보드에 필요한 건수 전부 — 쿼리 한 번 */
    StatsCounts countAll();
}
