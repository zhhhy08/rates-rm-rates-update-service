package com.hertz.api.repository;

import static java.lang.String.format;

import com.hertz.api.models.Director;
import java.util.Arrays;
import org.springframework.stereotype.Component;

@Component
public class DirectorRepositoryImpl implements DirectorRepository {

  public Director getDirector(String id) {
    Director director = new Director();
    director.setName("David Lynch");
    director.setType("director");
    director.setId(id);
    director.setAwards(Arrays.asList(format("metadata-%s", id)));
    return director;
  }
}

