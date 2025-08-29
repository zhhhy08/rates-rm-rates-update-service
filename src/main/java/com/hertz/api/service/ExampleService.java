package com.hertz.api.service;

import com.hertz.api.models.Director;
import com.hertz.api.models.Movie;

public interface ExampleService {

  Movie getMovie(String id);

  Director getDirector(String id);
}
