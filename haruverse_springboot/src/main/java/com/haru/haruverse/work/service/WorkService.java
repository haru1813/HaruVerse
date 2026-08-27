package com.haru.haruverse.work.service;

import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.search.document.WorkDocument;
import com.haru.haruverse.search.service.WorkSearchService;
import com.haru.haruverse.work.dto.WorkDetailResponse;
import com.haru.haruverse.work.dto.WorkResponse;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import com.haru.haruverse.work.repository.WorkSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 작품 도메인 서비스 — 목록 조회(필터)와 단건 조회.
 *
 * <p><b>DTO 변환을 서비스 안에서 하는 이유</b>
 * Work의 genres·studio는 지연 로딩(LAZY)이라, 실제로 값을 꺼내는 시점에 추가 쿼리가 나간다.
 * 컨트롤러에서 변환하면 그 시점이 <b>트랜잭션 밖</b>이라
 * LazyInitializationException이 나거나, open-in-view에 의존하게 된다.
 * → 트랜잭션 안에서 DTO로 바꿔서 내보내면 경계가 명확해지고 open-in-view를 끌 수 있다.
 */
@Service
public class WorkService {

    private final WorkRepository workRepository;
    private final WorkSearchService searchService;

    public WorkService(WorkRepository workRepository, WorkSearchService searchService) {
        this.workRepository = workRepository;
        this.searchService = searchService;
    }

    /**
     * 작품 목록 — 다섯 조건을 <b>조합</b>해서 거른다.
     *
     * <p>예전에는 if 분기로 조건 하나를 골라 파생 메서드를 불렀는데,
     * 검색어가 있으면 type·genre가 무시되어 <b>게임 탭에서 검색하면 애니가 나왔다.</b>
     * Specification으로 바꾸면서 모든 조건이 AND로 함께 걸린다.
     */
    @Transactional(readOnly = true)
    public PageResponse<WorkResponse> getWorks(WorkType type, String season, String genre,
                                               String keyword, String studio, Pageable pageable) {
        // ★키워드가 있을 때만 Elasticsearch★
        //   검색어가 없으면 그냥 목록을 훑는 것이고, 그건 DB가 원본이라 더 정확하다.
        //   ES를 항상 거치면 색인이 밀렸을 때 목록 자체가 어긋난다.
        //   ES가 값을 하는 건 "관련도"가 필요한 순간뿐이다.
        if (keyword != null && !keyword.isBlank()) {
            Page<WorkDocument> found =
                    searchService.search(keyword, type, season, genre, studio, pageable);
            // null = ES에 못 붙었다는 신호 → 아래 DB 검색으로 흘러간다
            if (found != null) {
                return PageResponse.of(found, WorkResponse::fromDocument);
            }
        }

        Page<Work> works = workRepository.findAll(
                WorkSpecs.filter(type, season, genre, keyword, studio), withDefaultSort(pageable));
        return PageResponse.of(works, WorkResponse::from); // 트랜잭션 안에서 변환
    }

    /**
     * 목록 기본 정렬 — 최신순.
     *
     * <p>컨트롤러가 아니라 여기서 붙이는 이유: 검색(ES) 경로에는 이 정렬이 가면 안 된다.
     * ES에 정렬을 주면 관련도(_score)를 버리고 그 기준으로 줄을 세운다.
     * 클라이언트가 {@code sort=} 를 명시했으면 그 뜻을 존중해 그대로 둔다.
     */
    private Pageable withDefaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) return pageable;
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "releaseDate"));
    }

    /** 작품 단건. 없으면 404로 이어질 예외를 던진다. */
    @Transactional(readOnly = true)
    public WorkDetailResponse getWork(Long id) {
        Work work = workRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("작품을 찾을 수 없습니다. (id=" + id + ")"));
        return WorkDetailResponse.from(work); // 트랜잭션 안에서 변환 (genres·studio 로딩 포함)
    }

    /** 저장 — 외부 API 수집·테스트에서 사용 */
    @Transactional
    public Work save(Work work) {
        return workRepository.save(work);
    }
}
