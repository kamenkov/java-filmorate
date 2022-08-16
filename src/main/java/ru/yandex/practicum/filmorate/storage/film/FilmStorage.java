package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {

    List<Film> findAll();

    Optional<Film> findById(Long id);

    Film createFilm(Film film);

    void updateFilm(Long id, Film film);

    boolean existsById(Long id);

    void deleteById(Long id);

}
