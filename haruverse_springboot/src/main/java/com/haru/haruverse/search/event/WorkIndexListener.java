package com.haru.haruverse.search.event;

import com.haru.haruverse.search.service.WorkIndexService;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.repository.WorkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 작품이 저장되면 색인에 반영한다.
 *
 * <p><b>★왜 저장 코드에서 직접 색인하지 않고 이벤트로 받는가★</b>
 * 세 가지 이유가 있다.
 *
 * <p><b>① 커밋된 것만 색인한다.</b>
 * 저장 직후 색인하면 그 트랜잭션이 나중에 롤백됐을 때 <b>DB에 없는 작품이 검색에 남는다.</b>
 * {@code AFTER_COMMIT} 은 커밋이 끝난 뒤에만 실행되므로 그런 유령이 생기지 않는다.
 *
 * <p><b>② 색인 실패가 저장을 되돌리지 못한다.</b>
 * 커밋이 이미 끝난 뒤라, 여기서 무슨 일이 나든 수집 결과는 그대로 남는다.
 * ES가 죽어 있어도 데이터 수집은 계속돼야 한다는 요구를 구조로 보장한다.
 *
 * <p><b>③ 수집기(Jikan·RAWG·StarRail)가 검색을 몰라도 된다.</b>
 * 수집기 세 곳에 색인 호출을 각각 심으면, 네 번째 수집기를 만들 때 또 잊어버린다.
 * 저장하는 쪽은 "저장했다"고 알리기만 하고, 색인은 이쪽이 책임진다.
 *
 * <p><b>주의</b>: AFTER_COMMIT 시점에는 원래 트랜잭션이 끝나 있어서 지연 로딩이 안 된다.
 * 그래서 엔티티를 그대로 들고 오지 않고 <b>id만</b> 이벤트에 담아, 여기서 새 트랜잭션으로
 * 다시 읽는다({@code REQUIRES_NEW}). 엔티티를 들고 오면 genres 를 읽는 순간 터진다.
 */
@Component
public class WorkIndexListener {

    private static final Logger log = LoggerFactory.getLogger(WorkIndexListener.class);

    private final WorkIndexService indexService;
    private final WorkRepository workRepository;

    public WorkIndexListener(WorkIndexService indexService, WorkRepository workRepository) {
        this.indexService = indexService;
        this.workRepository = workRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onWorkSaved(WorkSavedEvent event) {
        try {
            Work work = workRepository.findById(event.workId()).orElse(null);
            if (work == null) {
                // 저장 직후 지워진 경우 — 색인에서도 빼준다
                indexService.remove(event.workId());
                return;
            }
            indexService.index(work); // 지연 로딩은 이 트랜잭션 안에서 일어난다
        } catch (Exception e) {
            // 여기서 터져도 수집 결과는 이미 커밋됐다. 재색인으로 복구된다.
            log.warn("색인 반영 실패 (workId={}): {}", event.workId(), e.toString());
        }
    }
}
