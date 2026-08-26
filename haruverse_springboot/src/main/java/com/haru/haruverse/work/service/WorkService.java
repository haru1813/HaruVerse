package com.haru.haruverse.work.service;

import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.work.dto.WorkDetailResponse;
import com.haru.haruverse.work.dto.WorkResponse;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkType;
import com.haru.haruverse.work.repository.WorkRepository;
import com.haru.haruverse.work.repository.WorkSpecs;
import org.springframework.data.domain.Page;
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

    public WorkService(WorkRepository workRepository) {
        this.workRepository = workRepository;
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
        Page<Work> works = workRepository.findAll(
                WorkSpecs.filter(type, season, genre, keyword, studio), pageable);
        return PageResponse.of(works, WorkResponse::from); // 트랜잭션 안에서 변환
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
