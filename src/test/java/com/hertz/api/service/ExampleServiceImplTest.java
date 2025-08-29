package com.hertz.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hertz.api.models.Movie;
import com.hertz.api.repository.MovieRepository;
import java.math.BigInteger;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExampleServiceImplTest {

  @Mock
  private MovieRepository movieRepository;

  @InjectMocks
  private ExampleServiceImpl exampleService;

  private Movie movie;

  @BeforeEach
  void setup() {

    this.movie = Movie.builder().name("attribute name")
        .type("model-type").id("1").awards(Arrays.asList("none")).build();

  }


  @Test
  public void shouldGetExpectedValue() {

    when(movieRepository.getMovie(anyString())).thenReturn(this.movie);

    Movie response = exampleService.getMovie("1");

    verify(movieRepository, times(BigInteger.ONE.intValue())).getMovie(anyString());
    assertNotNull(response);
    assertEquals("1", response.getId());
  }

}