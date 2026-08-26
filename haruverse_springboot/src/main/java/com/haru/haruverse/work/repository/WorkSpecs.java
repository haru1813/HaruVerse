package com.haru.haruverse.work.repository;

import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.entity.WorkType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 작품 목록 필터 조건.
 *
 * <p><b>왜 파생 쿼리(findByTypeAndSeason…) 대신 Specification인가</b>
 * 조건이 type·season·genre·keyword·studio 다섯 개다.
 * 파생 메서드로 다루려면 조합마다 하나씩 만들어야 하는데, 그러면 32가지가 필요하다.
 * 실제로는 다 만들 수 없으니 if 분기로 "가장 그럴듯한 하나"를 골라 쓰게 되고,
 * 그 결과 <b>나머지 조건이 조용히 무시된다.</b>
 *
 * <p>실제로 그런 버그가 있었다 — 검색어가 있으면 type을 무시해서
 * <b>게임 탭에서 검색하면 애니가 나왔다.</b> 조건을 조합으로 다루면 이 문제가 사라진다.
 */
public final class WorkSpecs {

    private WorkSpecs() {}

    public static Specification<Work> filter(WorkType type, String season, String genre,
                                             String keyword, String studio) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (hasText(season)) {
                predicates.add(cb.equal(root.get("season"), season.trim()));
            }
            if (hasText(keyword)) {
                // 대소문자를 가리지 않는 부분 일치. DB의 LIKE라 앞뒤 %가 붙으면 인덱스를 못 탄다.
                // 데이터가 커지면 이 부분이 먼저 느려진다 → 그때 Elasticsearch로 옮긴다.
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (hasText(studio)) {
                // studio는 @ManyToOne — get()을 이어 쓰면 inner join이 된다.
                // 제작사가 없는 작품은 자연히 제외된다(이 조건을 준 이상 맞는 동작).
                predicates.add(cb.equal(cb.lower(root.get("studio").get("name")), studio.trim().toLowerCase()));
            }
            if (hasText(genre)) {
                // ★genres는 @ManyToMany★ join하면 장르 수만큼 같은 작품이 중복해서 나온다.
                // distinct를 걸지 않으면 목록에 같은 카드가 여러 번 뜨고 총 건수도 부풀려진다.
                Join<Object, Object> g = root.join("genres");
                predicates.add(cb.equal(cb.lower(g.get("name")), genre.trim().toLowerCase()));
                if (query != null) {
                    query.distinct(true);
                }
            }

            // 조건이 하나도 없으면 전체 조회
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
