package com.haru.haruverse.voiceactor.service;

import com.haru.haruverse.character.entity.AnimeCharacter;
import com.haru.haruverse.character.repository.AnimeCharacterRepository;
import com.haru.haruverse.voiceactor.entity.VoiceActor;
import com.haru.haruverse.voiceactor.repository.VoiceActorRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 기존 캐릭터에 문자열로 붙어 있던 성우 이름을 {@link VoiceActor} 엔티티로 옮긴다.
 *
 * <p><b>왜 이런 게 필요한가</b>
 * 캐릭터를 처음 수집할 때 성우는 이름만 저장했다(식별자·이미지를 버렸다).
 * 성우를 축으로 탐색하려면 엔티티가 필요한데, 다시 수집해서 채우려 해도
 * MyAnimeList가 죽어 있으면(504) 방법이 없다.
 * → 이미 가진 이름만으로 먼저 엔티티를 만들어 두고,
 *   나중에 재수집될 때 식별자·이미지가 채워지게 한다.
 *
 * <p><b>왜 네이티브 쿼리인가</b>
 * AnimeCharacter에서 문자열 필드를 관계로 교체했기 때문에, 엔티티로는
 * 예전 {@code voice_actor} 컬럼을 더 이상 읽을 수 없다.
 * 그 컬럼은 DB에 그대로 남아 있으므로(ddl-auto=update는 컬럼을 지우지 않는다)
 * SQL로 직접 읽는다. 이관이 끝나면 이 코드는 역할을 다한다.
 */
@Service
public class VoiceActorMigrationService {

    private static final Logger log = LoggerFactory.getLogger(VoiceActorMigrationService.class);

    private final EntityManager em;
    private final VoiceActorRepository voiceActorRepository;
    private final AnimeCharacterRepository characterRepository;

    public VoiceActorMigrationService(EntityManager em,
                                      VoiceActorRepository voiceActorRepository,
                                      AnimeCharacterRepository characterRepository) {
        this.em = em;
        this.voiceActorRepository = voiceActorRepository;
        this.characterRepository = characterRepository;
    }

    /**
     * @param scanned  예전 컬럼에 이름이 있던 캐릭터 수
     * @param created  새로 만든 성우 수
     * @param linked   성우를 붙인 캐릭터 수
     */
    public record MigrationResult(int scanned, int created, int linked) {}

    @Transactional
    public MigrationResult migrate() {
        // 아직 성우가 연결되지 않았고, 예전 컬럼에 이름이 남아 있는 캐릭터
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                select id, voice_actor
                from anime_character
                where voice_actor is not null
                  and voice_actor <> ''
                  and voice_actor_id is null
                """).getResultList();

        if (rows.isEmpty()) {
            log.info("성우 이관: 대상 없음 (이미 끝났거나 예전 데이터가 없다)");
            return new MigrationResult(0, 0, 0);
        }

        // 캐릭터 id → 성우 이름
        Map<Long, String> nameByCharacterId = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Long characterId = ((Number) row[0]).longValue();
            String name = ((String) row[1]).trim();
            if (!name.isEmpty()) nameByCharacterId.put(characterId, name);
        }

        // 이름을 모아 한 번에 조회하고, 없는 것만 만든다
        Set<String> names = new LinkedHashSet<>(nameByCharacterId.values());
        Map<String, VoiceActor> byName = new HashMap<>();
        for (VoiceActor v : voiceActorRepository.findByNameIn(names)) {
            byName.put(v.getName(), v);
        }

        int created = 0;
        List<VoiceActor> newOnes = new ArrayList<>();
        for (String name : names) {
            if (!byName.containsKey(name)) {
                VoiceActor v = new VoiceActor(name); // malId·imageUrl은 재수집 때 채워진다
                newOnes.add(v);
                byName.put(name, v);
                created++;
            }
        }
        voiceActorRepository.saveAll(newOnes); // 여기서 id가 붙는다

        int linked = 0;
        // 트랜잭션 안에서 영속 상태인 엔티티를 고치면 커밋 때 자동 반영된다(dirty checking).
        // saveAll을 부를 필요가 없다.
        List<AnimeCharacter> characters = characterRepository.findAllById(nameByCharacterId.keySet());
        for (AnimeCharacter c : characters) {
            VoiceActor v = byName.get(nameByCharacterId.get(c.getId()));
            if (v != null) {
                c.assignVoiceActor(v);
                linked++;
            }
        }

        log.info("성우 이관 완료: 캐릭터 {}건 / 성우 신규 {}명 / 연결 {}건",
                nameByCharacterId.size(), created, linked);
        return new MigrationResult(nameByCharacterId.size(), created, linked);
    }
}
