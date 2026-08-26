package com.haru.haruverse.external.jikan;

import com.haru.haruverse.character.entity.AnimeCharacter;
import com.haru.haruverse.character.entity.CharacterRole;
import com.haru.haruverse.character.entity.CharacterSource;
import com.haru.haruverse.character.entity.WorkCharacter;
import com.haru.haruverse.character.repository.AnimeCharacterRepository;
import com.haru.haruverse.character.repository.WorkCharacterRepository;
import com.haru.haruverse.external.jikan.dto.JikanCharacterEntry;
import com.haru.haruverse.work.entity.Work;
import com.haru.haruverse.voiceactor.entity.VoiceActor;
import com.haru.haruverse.voiceactor.repository.VoiceActorRepository;
import com.haru.haruverse.work.repository.WorkRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 캐릭터 저장 담당.
 *
 * <p><b>수집 서비스와 클래스를 나눈 이유</b>
 * @Transactional은 스프링이 만든 프록시를 거칠 때만 동작한다.
 * 같은 클래스 안에서 {@code this.upsert(...)}로 부르면 프록시를 지나지 않아
 * <b>트랜잭션이 조용히 적용되지 않는다.</b> (에러도 안 난다 — 그래서 더 위험하다)
 * 외부 API 호출은 트랜잭션 밖에, 저장만 트랜잭션 안에 두려면 클래스를 나눠야 한다.
 */
@Component
public class JikanCharacterWriter {

    private final AnimeCharacterRepository characterRepository;
    private final WorkCharacterRepository workCharacterRepository;
    private final WorkRepository workRepository;
    private final VoiceActorRepository voiceActorRepository;

    public JikanCharacterWriter(AnimeCharacterRepository characterRepository,
                                WorkCharacterRepository workCharacterRepository,
                                WorkRepository workRepository,
                                VoiceActorRepository voiceActorRepository) {
        this.characterRepository = characterRepository;
        this.workCharacterRepository = workCharacterRepository;
        this.workRepository = workRepository;
        this.voiceActorRepository = voiceActorRepository;
    }

    /** @param created 새로 만든 캐릭터 수, @param linked 이 작품에 새로 연결한 수 */
    public record WriteResult(int created, int updated, int linked) {}

    /**
     * 한 작품의 캐릭터를 통째로 저장한다.
     *
     * <p><b>쿼리 수를 줄이는 게 핵심이다.</b> 캐릭터를 하나씩 조회하면
     * 작품당 60여 번, 86개 작품이면 5,000번이 넘는 쿼리가 나간다.
     * → 식별자를 모아 한 번에 조회하고 Map으로 대조한다.
     */
    @Transactional
    public WriteResult upsert(Long workId, List<JikanCharacterEntry> entries) {
        // 같은 응답 안에 같은 캐릭터가 두 번 오는 경우가 있어 먼저 겹치는 것을 없앤다
        // 키는 저장에 쓸 식별자("jikan-188175") — 같은 응답에 같은 캐릭터가 두 번 오는 경우가 있다
        Map<String, JikanCharacterEntry> byExternalId = new LinkedHashMap<>();
        for (JikanCharacterEntry e : entries) {
            if (e.isValid()) {
                byExternalId.putIfAbsent(CharacterSource.JIKAN.externalId(e.malId()), e);
            }
        }
        if (byExternalId.isEmpty()) return new WriteResult(0, 0, 0);

        // ① 성우를 먼저 정리한다 (이름 기준). 캐릭터마다 조회하면 쿼리가 60번 나간다
        Map<String, VoiceActor> voiceActors = resolveVoiceActors(byExternalId.values());

        // ② 이미 있는 캐릭터를 한 번에 조회 (다른 작품에서 이미 수집됐을 수 있다)
        Map<String, AnimeCharacter> found = new HashMap<>();
        for (AnimeCharacter c : characterRepository.findByExternalIdIn(byExternalId.keySet())) {
            found.put(c.getExternalId(), c);
        }

        int created = 0, updated = 0;
        Map<String, AnimeCharacter> resolved = new LinkedHashMap<>();
        for (var entry : byExternalId.entrySet()) {
            String externalId = entry.getKey();
            JikanCharacterEntry e = entry.getValue();

            AnimeCharacter c = found.get(externalId);
            if (c == null) {
                c = new AnimeCharacter(externalId, e.name());
                created++;
            } else {
                updated++;
            }
            c.update(e.name(), e.imageUrl(), e.favorites(),
                    voiceActors.get(e.japaneseVoiceActor()));
            resolved.put(externalId, c);
        }
        characterRepository.saveAll(resolved.values()); // 여기서 새 캐릭터에 id가 붙는다

        // ③ 이 작품에 이미 연결된 캐릭터도 한 번에 조회
        Set<Long> alreadyLinked = new HashSet<>(workCharacterRepository.findCharacterIdsByWorkId(workId));

        // 연결의 FK만 채우면 되므로 프록시로 충분하다 (Work를 실제로 읽지 않는다)
        Work work = workRepository.getReferenceById(workId);

        List<WorkCharacter> newLinks = new ArrayList<>();
        for (var entry : byExternalId.entrySet()) {
            AnimeCharacter c = resolved.get(entry.getKey());
            if (alreadyLinked.contains(c.getId())) continue;
            newLinks.add(new WorkCharacter(work, c, CharacterRole.from(entry.getValue().role())));
        }
        workCharacterRepository.saveAll(newLinks);

        return new WriteResult(created, updated, newLinks.size());
    }

    /**
     * 응답에 나온 일본어 성우들을 한 번에 조회·생성해 이름→엔티티 Map으로 돌려준다.
     *
     * <p>캐릭터마다 findByName을 부르면 작품당 60번, 86작품이면 5,000번이 넘는다.
     * 이름을 모아 한 번에 조회하고 없는 것만 만든다.
     *
     * <p>이미 있는 성우에게는 비어 있던 식별자·이미지를 채워준다
     * (이름만 이관된 데이터에 재수집으로 살을 붙이는 경로).
     */
    private Map<String, VoiceActor> resolveVoiceActors(Collection<JikanCharacterEntry> entries) {
        Map<String, JikanCharacterEntry.VoiceActor.Person> byName = new LinkedHashMap<>();
        for (JikanCharacterEntry e : entries) {
            var person = e.japaneseVoiceActorPerson();
            if (person != null) byName.putIfAbsent(person.name(), person);
        }
        if (byName.isEmpty()) return Map.of();

        Map<String, VoiceActor> resolved = new HashMap<>();
        for (VoiceActor v : voiceActorRepository.findByNameIn(byName.keySet())) {
            resolved.put(v.getName(), v);
        }

        List<VoiceActor> toSave = new ArrayList<>();
        for (var person : byName.values()) {
            VoiceActor v = resolved.get(person.name());
            if (v == null) {
                v = new VoiceActor(person.name());
                resolved.put(person.name(), v);
            }
            v.fillIfAbsent(person.malId(), person.imageUrl());
            toSave.add(v);
        }
        voiceActorRepository.saveAll(toSave);
        return resolved;
    }
}
