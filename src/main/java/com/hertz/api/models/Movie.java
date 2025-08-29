package com.hertz.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Movie {

  private String id;

  private String type;

  private List<String> awards;

  private String name;

}



