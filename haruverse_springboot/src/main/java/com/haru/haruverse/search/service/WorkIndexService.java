package com.haru.haruverse.search.service;

import com.haru.haruverse.search.document.WorkDocument;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.repository.WorkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 작품 색인 — DB의 작품을 Elasticsearch 문서로 밀어 넣는다.
 *
 * <p><b>★이 클래스의 제1 원칙: 색인 실패가 서비스 실패가 되면 안 된다★</b>
 * 검색은 부가 기능이다. ES가 죽어 있어도 작품 수집·조회·커뮤니티는 그대로 돌아가야 한다.
 * 그래서 모든 색인 작업은 예외를 밖으로 내보내지 않고 <b>로그만 남기고 삼킨다.</b>
 * (예외를 삼키는 건 보통 나쁜 습관이지만, 여기서는 그게 요구사항이다.
 *  대신 무엇이 실패했는지 반드시 로그로 남긴다 — 조용히 사라지면 그게 더 나쁘다)
 *
 * <p>색인이 밀리면 {@link #reindexAll()} 로 통째로 다시 만든다.
 */
@Service
public class WorkIndexService {

    private static final Logger log = LoggerFactory.getLogger(WorkIndexService.class);

    private final ElasticsearchOperations operations;
    private final WorkRepository workRepository;

    public WorkIndexService(ElasticsearchOperations operations, WorkRepository workRepository) {
        this.operations = operations;
        this.workRepository = workRepository;
    }

    /**
     * 색인이 없으면 만든다(매핑·분석기 포함).
     *
     * <p>{@code @Document(createIndex = false)} 로 두었으므로 앱 기동 때 자동으로 만들지 않는다.
     * ES가 죽어 있어도 앱은 떠야 하기 때문이다. 대신 색인을 쓰기 직전에 여기서 보장한다.
     *
     * @return 준비되었으면 true, ES에 못 붙었으면 false
     */
    public boolean ensureIndex() {
        try {
            IndexOperations indexOps = operations.indexOps(WorkDocument.class);
            if (!indexOps.exists()) {
                // 설정(work-settings.json)과 매핑(@Field)을 함께 적용해 만든다
                indexOps.createWithMapping();
                log.info("작품 색인을 생성했습니다.");
            }
            return true;
        } catch (Exception e) {
            log.warn("색인을 준비하지 못했습니다 — 검색이 비활성화됩니다: {}", e.toString());
            return false;
        }
    }

    /**
     * 작품 하나를 색인.
     *
     * <p>★트랜잭션 안에서 불러야 한다★ genres·platforms 가 지연 로딩이라
     * 트랜잭션 밖이면 LazyInitializationException 이 난다(open-in-view 를 껐다).
     */
    public void index(Work work) {
        try {
            if (!ensureIndex()) return;
            operations.save(WorkDocument.from(work));
        } catch (Exception e) {
            // 저장은 이미 커밋됐다. 색인만 밀린 상태이므로 재색인으로 복구된다.
            log.warn("작품 색인 실패 (id={}): {}", work.getId(), e.toString());
        }
    }

    /** 색인에서 제거 — 작품이 지워졌을 때 */
    public void remove(Long workId) {
        try {
            if (!ensureIndex()) return;
            operations.delete(String.valueOf(workId), WorkDocument.class);
        } catch (Exception e) {
            log.warn("작품 색인 삭제 실패 (id={}): {}", workId, e.toString());
        }
    }

    /**
     * 전량 재색인 — 색인이 밀렸거나 매핑을 바꿨을 때의 복구 수단.
     *
     * <p>★readOnly 트랜잭션 안에서 도는 이유★ 작품마다 genres·platforms 를 지연 로딩한다.
     * (Work 엔티티에 {@code @BatchSize(100)} 이 붙어 있어 N+1 은 나지 않는다)
     *
     * <p>작품이 187편 수준이라 한 번에 처리한다. 수만 건이 되면 페이지 단위로 끊어야 한다.
     *
     * @return 색인한 문서 수. ES에 못 붙었으면 -1
     */
    @Transactional(readOnly = true)
    public int reindexAll() {
        if (!ensureIndex()) return -1;

        try {
            List<Work> works = workRepository.findAll();
            if (works.isEmpty()) return 0;

            List<WorkDocument> docs = works.stream().map(WorkDocument::from).toList();
            operations.save(docs); // 벌크 — 한 건씩 보내면 187번 왕복한다
            operations.indexOps(WorkDocument.class).refresh(); // 바로 검색되도록

            log.info("전량 재색인 완료: {}건", docs.size());
            return docs.size();
        } catch (Exception e) {
            log.warn("전량 재색인 실패: {}", e.toString());
            return -1;
        }
    }

    /** 색인을 지우고 다시 만든다 — 매핑을 바꿨을 때 (매핑은 나중에 못 바꾸는 필드가 있다) */
    public boolean recreateIndex() {
        try {
            IndexOperations indexOps = operations.indexOps(WorkDocument.class);
            if (indexOps.exists()) indexOps.delete();
            indexOps.createWithMapping();
            log.info("작품 색인을 다시 만들었습니다.");
            return true;
        } catch (Exception e) {
            log.warn("색인 재생성 실패: {}", e.toString());
            return false;
        }
    }
}
