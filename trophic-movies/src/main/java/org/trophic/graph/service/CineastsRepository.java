package org.trophic.graph.service;

import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.trophic.graph.domain.Movie;
import org.trophic.graph.domain.Person;
import org.trophic.graph.domain.Rating;
import org.trophic.graph.domain.User;
import org.trophic.graph.repository.MovieRepository;
import org.trophic.graph.repository.PersonRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 04.03.11
 */
@Repository
@Transactional
public class CineastsRepository {

    @Autowired private PersonRepository personRepository;
    @Autowired private MovieRepository movieRepository;


    public Movie getMovie(String id) {
        return movieRepository.findByPropertyValue("id", id);
    }

    public List<Movie> findMovies(String query, int max) {
        if (query.isEmpty()) return Collections.emptyList();
        if (max < 1 || max > 1000) max = 100;

        ClosableIterable<Movie> searchResult = movieRepository.findAllByQuery("search", "title", query);
        List<Movie> result=new ArrayList<Movie>(max);
        for (Movie movie : searchResult) {
            result.add(movie);
            if (--max == 0) break;
        }
        searchResult.close();
        return result;
    }

    public Person getPerson(String id) {
        return personRepository.findByPropertyValue("id",id);
    }

    public Rating rateMovie(Movie movie, User user, int stars, String comment) {
        if (user == null || movie==null) return null;
        return user.rate(movie, stars,comment);
    }

    public Map<Movie,?> recommendMovies(User user, final int ratingDistance) {
        final FriendsMovieRecommendations movieRecommendations = new FriendsMovieRecommendations(ratingDistance);
        return movieRecommendations.getRecommendationsFor(user);
    }
}
