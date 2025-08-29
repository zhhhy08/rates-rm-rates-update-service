package com.hertz.api.controller;

import com.hertz.api.models.Director;
import com.hertz.api.models.DirectorResponse;
import com.hertz.api.models.Movie;
import com.hertz.api.service.ExampleService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller should implement the generated interface that is build from
 * the openapi spec for the service.
 *
 */
@AllArgsConstructor
@RestController
@Slf4j
public class ExampleController {

  private ExampleService exampleService;

  // controller methods should be annotated with the correct SecurityExpression method to authorize access
  @PreAuthorize("isHtzdAuthorized()")
  DirectorResponse getDirector(String id) {
    log.debug("request with id = {}", id);

    Movie movie1 = exampleService.getMovie("1");
    Movie movie2 = exampleService.getMovie("2");

    Director director = exampleService.getDirector("1");

    DirectorResponse directorResponse = new DirectorResponse();
    directorResponse.setData(director);

    return directorResponse;
  }
}