package ru.yandex.practicum.filmorate.dao.likes;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface LikesDao {

    void addLike(Long userId, Long filmId);

    void removeLike(Long userId, Long filmId);

    List<Film> getPopular(Long genreId, Integer year, int count);

    boolean isLikeExist(Long userId, Long filmId);
}
