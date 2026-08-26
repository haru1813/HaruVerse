package com.haru.haruverse.work.entity;

/**
 * 작품 종류. HaruVerse는 애니메이션과 게임을 한 테이블(work)에서 함께 다룬다.
 *
 * <p>DB에는 문자열("ANIME")로 저장한다(@Enumerated(EnumType.STRING)).
 * ORDINAL(0,1,...)로 저장하면 enum 상수 순서를 바꾸는 순간 기존 데이터의 의미가
 * 통째로 뒤바뀌므로 절대 쓰지 않는다.
 */
public enum WorkType {
    ANIME,
    GAME
}
