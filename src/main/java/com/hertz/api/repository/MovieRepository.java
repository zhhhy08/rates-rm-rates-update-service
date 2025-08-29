package com.hertz.api.repository;

import com.hertz.api.models.Movie;

public interface MovieRepository {

  Movie getMovie(String id);
}
