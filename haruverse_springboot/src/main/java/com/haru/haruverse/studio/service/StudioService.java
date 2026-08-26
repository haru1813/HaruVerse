package com.haru.haruverse.studio.service;

import com.haru.haruverse.studio.entity.Studio;
import com.haru.haruverse.studio.repository.StudioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import com.haru.haruverse.global.response.PageResponse;
import com.haru.haruverse.studio.dto.StudioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudioService {

    private final StudioRepository studioRepository;

    public StudioService(StudioRepository studioRepository) {
        this.studioRepository = studioRepository;
    }

    /**
     * 이름으로 찾고 없으면 만든다 (find-or-create).
     *
     * <p>외부 API에서 "Madhouse"가 수백 번 와도 studio 행은 하나만 있어야 한다.
     *
     * <p>⚠️ 동시에 두 스레드가 같은 이름을 만들면 UNIQUE 제약에 걸린다.
     * 지금 수집은 단일 스레드지만, 나중에 병렬 수집으로 바꿔도 깨지지 않도록
     * 제약 위반을 잡아 재조회한다(낙관적 처리).
     */
    @Transactional
    public Studio findOrCreate(String name) {
        return studioRepository.findByName(name)
                .orElseGet(() -> {
                    try {
                        return studioRepository.save(new Studio(name));
                    } catch (DataIntegrityViolationException e) {
                        // 그 사이 다른 스레드가 만들었다 → 그걸 쓴다
                        return studioRepository.findByName(name).orElseThrow(() -> e);
                    }
                });
    }

    /**
     * 제작사 목록 — 작품이 많은 순.
     *
     * <p>집계 결과를 그대로 DTO로 받으므로 엔티티 지연 로딩이 끼어들지 않는다.
     */
    @Transactional(readOnly = true)
    public PageResponse<StudioResponse> getStudios(String keyword, Pageable pageable) {
        String k = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<StudioResponse> page = studioRepository.findAllWithWorkCount(k, pageable);
        // 이미 DTO라 변환이 필요 없다
        return PageResponse.of(page, r -> r);
    }
}
