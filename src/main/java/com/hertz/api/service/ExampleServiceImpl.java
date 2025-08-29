package com.hertz.api.service;

import com.hertz.api.models.Director;
import com.hertz.api.models.Movie;
import com.hertz.api.repository.DirectorRepository;
import com.hertz.api.repository.MovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ExampleServiceImpl implements ExampleService {

  private MovieRepository movieRepository;

  private DirectorRepository directorRepository;

  @Override
  public Movie getMovie(String id) {
    return movieRepository.getMovie(id);
  }

  @Override
  public Director getDirector(String id) {
    return directorRepository.getDirector(id);
  }
}
