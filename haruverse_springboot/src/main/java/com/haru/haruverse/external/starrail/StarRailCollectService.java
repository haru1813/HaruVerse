package com.haru.haruverse.external.starrail;

import com.haru.haruverse.character.entity.AnimeCharacter;
import com.haru.haruverse.character.entity.CharacterRole;
import com.haru.haruverse.character.entity.CharacterSource;
import com.haru.haruverse.character.entity.WorkCharacter;
import com.haru.haruverse.character.repository.AnimeCharacterRepository;
import com.haru.haruverse.character.repository.WorkCharacterRepository;
import com.haru.haruverse.external.starrail.dto.StarRailCharacter;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.work.repository.WorkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 붕괴: 스타레일 캐릭터 수집.
 *
 * <p>Jikan 수집과 구조가 다르다 — 작품마다 부르는 게 아니라
 * <b>정적 파일 한 번</b>으로 전체 캐릭터를 받는다. 요청 제한도 없어서 간격을 둘 필요가 없다.
 *
 * <p>여기서는 클래스를 나누지 않고 {@code @Transactional}을 메서드에 붙였다.
 * 외부 호출이 <b>한 번뿐</b>이고 그 뒤에 저장만 하므로,
 * 호출을 트랜잭션 밖으로 빼려고 클래스를 쪼갤 이유가 없다.
 * (Jikan은 작품마다 호출·저장이 반복돼서 나눠야 했다)
 */
@Service
public class StarRailCollectService {

    private static final Logger log = LoggerFactory.getLogger(StarRailCollectService.class);

    private final StarRailClient client;
    private final AnimeCharacterRepository characterRepository;
    private final WorkCharacterRepository workCharacterRepository;
    private final WorkRepository workRepository;

    public StarRailCollectService(StarRailClient client,
                                  AnimeCharacterRepository characterRepository,
                                  WorkCharacterRepository workCharacterRepository,
                                  WorkRepository workRepository) {
        this.client = client;
        this.characterRepository = characterRepository;
        this.workCharacterRepository = workCharacterRepository;
        this.workRepository = workRepository;
    }

    public record CollectResult(int fetched, int created, int updated, int linked, int skipped) {}

    /**
     * @param workId 연결할 작품(붕괴: 스타레일). 그 작품의 등장인물로 묶인다.
     */
    public CollectResult collect(Long workId) {
        // 외부 호출은 트랜잭션 밖에서 (느리고 실패할 수 있다)
        List<StarRailCharacter> characters = client.fetchCharacters();
        return save(workId, characters);
    }

    @Transactional
    protected CollectResult save(Long workId, List<StarRailCharacter> characters) {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new NoSuchElementException("작품을 찾을 수 없습니다. id=" + workId));

        // 같은 이름이 여러 번 오는 경우(주인공 남/녀 등)를 대비해 식별자로 정리
        Map<String, StarRailCharacter> byExternalId = new LinkedHashMap<>();
        int skipped = 0;
        for (StarRailCharacter c : characters) {
            if (!c.isValid()) { skipped++; continue; }
            byExternalId.putIfAbsent(CharacterSource.STAR_RAIL.externalId(c.id()), c);
        }
        if (byExternalId.isEmpty()) return new CollectResult(characters.size(), 0, 0, 0, skipped);

        // 이미 있는 캐릭터를 한 번에 조회 (재수집 시 중복 생성 방지)
        Map<String, AnimeCharacter> found = new HashMap<>();
        for (AnimeCharacter c : characterRepository.findByExternalIdIn(byExternalId.keySet())) {
            found.put(c.getExternalId(), c);
        }

        int created = 0, updated = 0;
        Map<String, AnimeCharacter> resolved = new LinkedHashMap<>();
        for (var entry : byExternalId.entrySet()) {
            String externalId = entry.getKey();
            StarRailCharacter src = entry.getValue();

            AnimeCharacter c = found.get(externalId);
            if (c == null) {
                c = new AnimeCharacter(externalId, src.displayName());
                created++;
            } else {
                updated++;
            }
            // ★portrait이 아니라 preview★ — portrait은 4MB라 목록에 깔면 화면이 멈춘다
            // favorites는 MAL 즐겨찾기 수라 이 출처엔 없다 → 0으로 두면 인기순 뒤쪽에 모인다
            // 성우 정보도 이 데이터에는 없다
            c.update(src.displayName(), client.toImageUrl(src.preview()), null, null);
            resolved.put(externalId, c);
        }
        characterRepository.saveAll(resolved.values());

        Set<Long> alreadyLinked = new HashSet<>(workCharacterRepository.findCharacterIdsByWorkId(workId));
        List<WorkCharacter> newLinks = new ArrayList<>();
        for (AnimeCharacter c : resolved.values()) {
            if (alreadyLinked.contains(c.getId())) continue;
            // 게임에는 주역/조역 구분이 없다 — 전부 MAIN으로 둔다
            newLinks.add(new WorkCharacter(work, c, CharacterRole.MAIN));
        }
        workCharacterRepository.saveAll(newLinks);

        log.info("스타레일 캐릭터 수집: 신규 {} · 갱신 {} · 연결 {} (건너뜀 {})",
                created, updated, newLinks.size(), skipped);
        return new CollectResult(characters.size(), created, updated, newLinks.size(), skipped);
    }
}
