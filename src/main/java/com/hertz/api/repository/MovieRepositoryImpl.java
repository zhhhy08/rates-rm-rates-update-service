package com.hertz.api.repository;

import static java.lang.String.format;

import com.hertz.api.models.Movie;
import java.util.Arrays;
import org.springframework.stereotype.Component;

@Component
public class MovieRepositoryImpl implements MovieRepository {

  public Movie getMovie(String id) {

    Movie movie = Movie.builder().name(format("Dune-%s", id)).type("movie")
        .id(id).awards(Arrays.asList(format("metadata-%s", id))).build();

    return movie;
  }
}
