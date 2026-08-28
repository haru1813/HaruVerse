package com.haru.haruverse.external.tmdb;

import com.haru.haruverse.search.event.WorkSavedEvent;
import com.haru.haruverse.work.repository.WorkRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 한국어 제목을 DB에 쓰는 역할.
 *
 * <p><b>★수집 서비스와 분리한 이유 — 자기 호출 함정★</b>
 * {@code @Transactional} 은 스프링이 감싼 프록시를 통해 호출될 때만 동작한다.
 * 같은 클래스 안에서 {@code this.save(...)} 로 부르면 프록시를 안 거쳐서
 * <b>트랜잭션이 조용히 적용되지 않는다</b>(에러도 안 난다 — 그래서 더 위험하다).
 * 별도 빈으로 빼면 주입된 프록시를 통해 불리므로 정상 동작한다.
 *
 * <p>작품 하나당 짧게 커밋한다. 수집 전체를 한 트랜잭션으로 묶으면
 * 외부 API 응답을 기다리는 동안 DB 커넥션을 붙잡고 있게 된다.
 */
@Service
public class TmdbTitleWriter {

    private final WorkRepository workRepository;
    private final ApplicationEventPublisher events;

    public TmdbTitleWriter(WorkRepository workRepository, ApplicationEventPublisher events) {
        this.workRepository = workRepository;
        this.events = events;
    }

    /**
     * 한국어 제목 저장.
     *
     * <p>색인 반영은 이벤트로 넘긴다 — 수집기가 검색을 몰라도 되게.
     * (AFTER_COMMIT 에 처리되므로 롤백된 값이 색인에 남지 않는다)
     *
     * @return 실제로 저장했으면 true (작품이 그새 지워졌으면 false)
     */
    @Transactional
    public boolean save(Long workId, String koTitle) {
        return workRepository.findById(workId).map(work -> {
            work.assignTitleKo(koTitle);
            events.publishEvent(new WorkSavedEvent(workId));
            return true;
        }).orElse(false);
    }
}
