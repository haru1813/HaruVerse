package com.haru.haruverse.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.haru.haruverse.search.document.WorkDocument;
import com.haru.haruverse.work.entity.WorkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 작품 검색 — Elasticsearch.
 *
 * <p><b>★ES가 죽으면 null 을 돌려준다★</b>
 * 예외를 던지지 않고 "검색을 못 했다"는 뜻으로 null 을 준다.
 * 부르는 쪽(WorkService)이 그때 기존 DB 검색으로 넘어간다.
 * 검색은 부가 기능이라, ES 장애가 목록 조회 자체를 막으면 안 된다.
 */
@Service
public class WorkSearchService {

    private static final Logger log = LoggerFactory.getLogger(WorkSearchService.class);

    /**
     * ★차단기(circuit breaker)★ — ES가 죽었을 때 매 요청이 타임아웃을 기다리지 않게 한다.
     *
     * <p>이게 없으면 ES 장애 중 <b>모든 검색이 연결 타임아웃(3초)만큼 느려진다.</b>
     * 실제로 재보니 요청당 3.4초였다. 사용자 입장에서는 "검색이 고장났다"와 구별되지 않는다.
     * 한 번 실패하면 잠시 ES를 건너뛰고 곧장 DB로 간다 — 느린 성공보다 빠른 폴백이 낫다.
     *
     * <p>라이브러리(Resilience4j)를 쓸 수도 있지만, 필요한 게 "실패하면 잠깐 쉰다" 하나뿐이라
     * 의존성을 늘리는 대신 필드 하나로 둔다.
     */
    private static final long COOLDOWN_MS = 30_000;
    private final AtomicLong skipUntil = new AtomicLong(0);

    private final ElasticsearchOperations operations;

    /**
     * 검색 엔진 사용 여부 — 끄면 항상 DB 검색으로 간다.
     *
     * <p><b>★테스트에서 반드시 꺼야 한다★</b>
     * 켜두면 테스트가 <b>개발 PC에 떠 있는 실제 색인</b>을 조회한다.
     * H2에 넣은 테스트 데이터가 아니라 운영 데이터가 돌아와서, 통과해야 할 테스트가 깨지고
     * (더 나쁘게는) 깨져야 할 테스트가 통과한다. 실제로 이 스위치가 없어서 5개가 깨졌다.
     *
     * <p>운영에서도 쓸모가 있다 — ES에 문제가 생겼을 때 재배포 없이 끌 수 있는 스위치다.
     */
    private final boolean enabled;

    public WorkSearchService(ElasticsearchOperations operations,
                             @Value("${search.elasticsearch.enabled:true}") boolean enabled) {
        this.operations = operations;
        this.enabled = enabled;
    }

    /**
     * 키워드 + 필터 검색.
     *
     * <p><b>왜 DB를 다시 읽지 않는가</b>
     * WorkDocument 에 화면이 필요한 값(제목·평점·이미지·장르·플랫폼)을 전부 비정규화해 담았다.
     * ES가 준 id로 DB를 다시 읽으면 페이지당 조회가 한 번 더 나가고, 정렬 순서도 다시 맞춰야 한다.
     * <b>비정규화를 해둔 이유가 이것</b>이므로 문서를 그대로 쓴다.
     *
     * @return 검색 결과. <b>ES에 못 붙었으면 null</b> (호출자가 DB 검색으로 폴백)
     */
    public Page<WorkDocument> search(String keyword, WorkType type, String season,
                                     String genre, String studio, Pageable pageable) {
        if (!enabled) return null; // 꺼져 있으면 곧장 DB 검색으로

        // 최근에 실패했으면 시도조차 하지 않는다 (타임아웃 대기를 건너뛴다)
        if (System.currentTimeMillis() < skipUntil.get()) return null;

        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> {
                        // ── 점수에 반영되는 부분: 키워드 ──
                        b.must(keywordQuery(keyword));

                        // ── 점수에 반영되지 않는 부분: 필터 ──
                        // ★filter 를 쓰는 이유★ must 에 넣으면 "장르가 Action이다" 같은
                        //   참/거짓 조건이 점수 계산에 끼어들어 순위를 흔든다.
                        //   filter 는 점수를 계산하지 않아 더 빠르고, 캐시도 된다.
                        for (Query f : filters(type, season, genre, studio)) {
                            b.filter(f);
                        }
                        return b;
                    }))
                    .withPageable(pageable)
                    .build();

            SearchHits<WorkDocument> hits = operations.search(query, WorkDocument.class);

            List<WorkDocument> content = hits.getSearchHits().stream()
                    .map(hit -> hit.getContent())
                    .toList();

            skipUntil.set(0); // 살아났다 — 차단 해제
            return new PageImpl<>(content, pageable, hits.getTotalHits());

        } catch (Exception e) {
            long until = System.currentTimeMillis() + COOLDOWN_MS;
            // 이미 차단 중이면 로그를 반복하지 않는다 (장애 때 로그가 폭발한다)
            if (skipUntil.getAndSet(until) < System.currentTimeMillis()) {
                log.warn("ES 검색 실패 — {}초간 DB 검색으로 넘어갑니다: {}",
                        COOLDOWN_MS / 1000, e.toString());
            }
            return null; // 폴백 신호
        }
    }

    /**
     * 키워드 질의 — 여러 필드를 한 번에 보되 <b>제목에 가중치</b>를 준다.
     *
     * <p>가중치가 없으면 줄거리에 단어가 여러 번 나온 작품이 제목이 정확히 일치하는 작품을
     * 밀어내고 위로 올라온다. 검색하는 사람이 기대하는 순서가 아니다.
     *
     * <p><b>fuzziness = AUTO</b> — 오타 한두 글자를 허용한다. 짧은 단어는 엄격하게,
     * 긴 단어는 관대하게 자동 조절된다(2글자 이하는 오타 허용 안 함 — 안 그러면 아무거나 걸린다).
     */
    private Query keywordQuery(String keyword) {
        return Query.of(q -> q.bool(b -> b
                // ── 제목 계열: 오타를 허용한다 ──
                //   사람이 제목을 틀리게 치는 건 흔하다. 여기는 관대하게.
                .should(sh -> sh.multiMatch(m -> m
                        .query(keyword)
                        .fields("title^5", "titleKo^5", "aliases^3")
                        .fuzziness("AUTO")
                        .minimumShouldMatch("75%")))

                // ── 줄거리·제작사: 오타를 허용하지 않는다 ──
                //   ★처음엔 여기도 fuzziness 를 걸었다가 검색이 망가졌다★
                //   줄거리는 수백 단어라 오타 허용을 켜면 아무 단어나 얼추 걸리고,
                //   "elden rong" 에 무관한 애니 29편이 나왔다.
                //   긴 본문에서는 오타 허용이 도움이 아니라 소음이다.
                .should(sh -> sh.multiMatch(m -> m
                        .query(keyword)
                        .fields("studio^2", "synopsis")
                        .minimumShouldMatch("75%")))

                // 둘 중 하나만 맞아도 결과에 포함 (제목 쪽이 가중치가 높아 위로 온다)
                .minimumShouldMatch("1")));
    }

    /** 필터 — 값이 있는 것만 AND 로 걸린다 */
    private List<Query> filters(WorkType type, String season, String genre, String studio) {
        List<Query> list = new ArrayList<>();
        addTerm(list, "type", type == null ? null : type.name());
        addTerm(list, "season", season);
        addTerm(list, "genres", genre);   // 배열 필드도 term 으로 걸린다 (원소 중 하나라도 일치)
        addTerm(list, "studio", studio);
        return list;
    }

    private void addTerm(List<Query> list, String field, String value) {
        if (value == null || value.isBlank()) return;
        list.add(Query.of(q -> q.term(t -> t.field(field).value(value))));
    }
}
