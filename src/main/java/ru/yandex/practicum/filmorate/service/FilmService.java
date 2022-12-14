package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.director.DirectorDao;
import ru.yandex.practicum.filmorate.dao.event.EventDao;
import ru.yandex.practicum.filmorate.dao.film.FilmDao;
import ru.yandex.practicum.filmorate.dao.likes.LikesDao;
import ru.yandex.practicum.filmorate.exceptions.LikeDoesntExistException;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FilmService {

    public static final String FILM_WITH_ID_NOT_FOUND_DEBUG = "Film with id {} not found";
    public static final String FILM_NOT_FOUND = "Film %s doesn't exist";
    public static final String USER_NOT_FOUND = "User with id %d doesn't exists";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final FilmDao filmDao;
    private final LikesDao likesDao;
    private final DirectorDao directorDao;
    private final EventDao eventDao;
    private final UserService userService;

    public FilmService(FilmDao filmDao, LikesDao likesDao, DirectorDao directorDao, UserService userService, EventDao eventDao) {
        this.filmDao = filmDao;
        this.likesDao = likesDao;
        this.directorDao = directorDao;
        this.userService = userService;
        this.eventDao = eventDao;
    }

    public List<Film> findAll() {
        return filmDao.findAll();
    }

    public Film findById(Long id) {
        return filmDao.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format(FILM_NOT_FOUND, id)));
    }

    public Film create(Film film) {
        Film savedFilm = filmDao.createFilm(film);
        log.debug("{} has been added.", savedFilm);
        return savedFilm;
    }

    public Film update(Long id, Film film) {
        Film previous = findById(id);
        filmDao.updateFilm(id, film);
        log.debug("Film updated. Before: {}, after: {}", previous, film);
        film.setDirectors(new HashSet<>(directorDao.findByFilmId(film.getId())));
        return film;
    }

    public void removeFilm(Long id) {
        if (!filmDao.existsById(id)) {
            log.debug(FILM_WITH_ID_NOT_FOUND_DEBUG, id);
            throw new NotFoundException(String.format(FILM_NOT_FOUND, id));
        }
        filmDao.deleteById(id);
        log.debug("Film id {} has been removed.", id);
    }

    public void addLike(Long id, Long userId) {
        validateExisting(id, userId);
        if (likesDao.isLikeExist(userId, id)) {
            log.debug("User with ID {} has already liked film with ID {}", userId, id);
            throw new LikeDoesntExistException(
                    String.format("User with ID %s has already liked film with ID %s", userId, id)
            );
        }
        eventDao.addEvent(new Event(userId, Event.EventType.LIKE, Event.Operation.ADD, id));
        likesDao.addLike(userId, id);
        log.debug("User {} liked film {}", userId, id);
    }

    public void removeLike(Long id, Long userId) {
        validateExisting(id, userId);
        if (!likesDao.isLikeExist(userId, id)) {
            log.debug("User with ID {} has not liked film with ID {}", userId, id);
            throw new LikeDoesntExistException(
                    String.format("User with ID %s has not liked film with ID %s", userId, id)
            );
        }
        eventDao.addEvent(new Event(userId, Event.EventType.LIKE, Event.Operation.REMOVE, id));
        likesDao.removeLike(userId, id);
        log.debug("User {} removed like from film {}", userId, id);
    }

    public List<Film> getPopular(Long genreId, Integer year, Integer count) {
        return likesDao.getPopular(genreId, year, count);
    }

    public boolean existsById(Long id) {
        return filmDao.existsById(id);
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        if (!userService.existById(userId)) {
            throw new NotFoundException(String.format(USER_NOT_FOUND, userId));
        }
        if (!userService.existById(friendId)) {
            throw new NotFoundException(String.format(USER_NOT_FOUND, friendId));
        }
        return filmDao.findCommonFilmsByUsersId(userId, friendId);
    }

    public List<Film> findFilmsByDirectorId(Long id, String sort) {
        if (!directorDao.existsById(id)) {
            throw new NotFoundException(String.format("Director with ID = %d not found", id));
        }
        return directorDao.findFilmsIdByDirectorId(id, sort).stream()
                .map(filmDao::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public List<Film> search(String query, List<String> params) {
        boolean canMatchTitle = false;
        boolean canMatchDirector = false;
        for (String s : params) {
            switch (s) {
                case ("director"):
                    canMatchDirector = true;
                    break;
                case ("title"):
                    canMatchTitle = true;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid request parameter :" + s);
            }
        }
        String titleQuery = canMatchTitle ? query.toLowerCase() : query.toUpperCase();
        String directorQuery = canMatchDirector ? query.toLowerCase() : query.toUpperCase();
        return filmDao.findFilms('%' + titleQuery + '%', '%' + directorQuery + '%');
    }

    private void validateExisting(Long filmId, Long userId) {
        if (!filmDao.existsById(filmId)) {
            log.debug(FILM_WITH_ID_NOT_FOUND_DEBUG, filmId);
            throw new NotFoundException(String.format(FILM_NOT_FOUND, filmId));
        }
        if (!userService.existById(userId)) {
            log.debug(UserService.USER_WITH_ID_NOT_FOUND_DEBUG, userId);
            throw new NotFoundException(String.format(UserService.USER_NOT_FOUND, userId));
        }
    }
}
