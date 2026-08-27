package com.haru.haruverse.search.event;

/**
 * 작품이 저장(생성·수정)되었다는 알림.
 *
 * <p>★엔티티가 아니라 id만 담는 이유★
 * 이 이벤트는 트랜잭션이 <b>커밋된 뒤</b> 처리된다. 그 시점엔 원래 영속성 컨텍스트가
 * 닫혀 있어서, 엔티티를 그대로 담아 보내면 genres·platforms 를 읽는 순간
 * LazyInitializationException 이 난다. id만 넘기고 받는 쪽에서 새로 읽는다.
 */
public record WorkSavedEvent(Long workId) {}
