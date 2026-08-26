package com.haru.haruverse.genre.service;

import com.haru.haruverse.genre.entity.Genre;
import com.haru.haruverse.genre.repository.GenreRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    /** 이름으로 찾고 없으면 만든다 (StudioService.findOrCreate와 동일한 이유·전략) */
    @Transactional
    public Genre findOrCreate(String name) {
        return genreRepository.findByName(name)
                .orElseGet(() -> {
                    try {
                        return genreRepository.save(new Genre(name));
                    } catch (DataIntegrityViolationException e) {
                        return genreRepository.findByName(name).orElseThrow(() -> e);
                    }
                });
    }
}
