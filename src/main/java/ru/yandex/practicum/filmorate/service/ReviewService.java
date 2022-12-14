package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.event.EventDao;
import ru.yandex.practicum.filmorate.dao.review.ReviewDao;
import ru.yandex.practicum.filmorate.exceptions.LikeDoesntExistException;
import ru.yandex.practicum.filmorate.exceptions.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

@Service
public class ReviewService {

    private static final String REVIEW_WITH_ID_NOT_FOUND_DEBUG = "Review with id {} not found";
    public static final String REVIEW_NOT_FOUND = "Review %s doesn't exist";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ReviewDao reviewDao;
    private final UserService userService;
    private final FilmService filmService;
    private final EventDao eventDao;

    public ReviewService(ReviewDao reviewDao, UserService userService, FilmService filmService, EventDao eventDao) {
        this.reviewDao = reviewDao;
        this.userService = userService;
        this.filmService = filmService;
        this.eventDao = eventDao;
    }

    public List<Review> findByFilmId(Long filmId, Integer count) {
        return reviewDao.findByFilmId(filmId, count);
    }

    public Review findById(Long id) {
        return reviewDao.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format(REVIEW_NOT_FOUND, id)));
    }

    public Review create(Review review) {
        final Long userId = review.getUserId();
        validateUserExisting(userId);
        validateFilmExisting(review.getFilmId());
        Review savedReview = reviewDao.createReview(review);
        eventDao.addEvent(new Event(userId, Event.EventType.REVIEW, Event.Operation.ADD, savedReview.getReviewId()));
        log.debug("{} has been added.", savedReview);
        return savedReview;
    }

    public Review update(Long id, Review review) {
        Review previous = findById(id);
        Long userId = previous.getUserId();
        review.setUserId(userId);
        review.setFilmId(previous.getFilmId());
        reviewDao.updateReview(id, review);
        eventDao.addEvent(new Event(userId, Event.EventType.REVIEW, Event.Operation.UPDATE, review.getReviewId()));
        log.debug("Review updated. Before: {}, after: {}", previous, review);
        return review;
    }

    public void removeReview(Long id) {
        Review review = findById(id);
        reviewDao.deleteById(id);
        eventDao.addEvent(new Event(review.getUserId(), Event.EventType.REVIEW,
                Event.Operation.REMOVE, review.getReviewId()));
        log.debug("Review {} removed", id);
    }

    public void addLike(Long reviewId, Long userId, Boolean isLike) {
        validateExisting(reviewId, userId);
        if (reviewDao.isLikeExist(reviewId, userId, isLike)) {
            log.debug("User with ID {} has already liked review with ID {}", userId, reviewId);
            throw new LikeDoesntExistException(
                    String.format("User with ID %s has already liked review with ID %s", userId, reviewId)
            );
        }
        reviewDao.addLike(reviewId, userId, isLike);
        log.debug("User {} liked review {}", userId, reviewId);
    }


    public void removeLike(Long reviewId, Long userId, Boolean isLike) {
        validateExisting(reviewId, userId);
        if (!reviewDao.isLikeExist(reviewId, userId, isLike)) {
            log.debug("User with ID {} has not liked review with ID {}", userId, reviewId);
            throw new LikeDoesntExistException(
                    String.format("User with ID %s has not liked review with ID %s", userId, reviewId)
            );
        }
        reviewDao.removeLike(reviewId, userId);
        log.debug("User {} removed like from review {}", userId, reviewId);
    }

    private void validateExisting(Long reviewId, Long userId) {
        validateReviewExisting(reviewId);
        validateUserExisting(userId);
    }

    private void validateReviewExisting(Long reviewId) {
        if (!reviewDao.existsById(reviewId)) {
            log.debug(REVIEW_WITH_ID_NOT_FOUND_DEBUG, reviewId);
            throw new NotFoundException(String.format(REVIEW_NOT_FOUND, reviewId));
        }
    }

    private void validateFilmExisting(Long filmId) {
        if (!filmService.existsById(filmId)) {
            log.debug(FilmService.FILM_WITH_ID_NOT_FOUND_DEBUG, filmId);
            throw new NotFoundException(String.format(FilmService.FILM_NOT_FOUND, filmId));
        }
    }

    private void validateUserExisting(Long userId) {
        if (!userService.existById(userId)) {
            log.debug(UserService.USER_WITH_ID_NOT_FOUND_DEBUG, userId);
            throw new NotFoundException(String.format(UserService.USER_NOT_FOUND, userId));
        }
    }
}
