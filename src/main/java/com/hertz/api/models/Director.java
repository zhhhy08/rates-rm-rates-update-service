package com.hertz.api.models;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "Director resource, description")
@NoArgsConstructor
public class Director {

  @Schema(description = "add field description here")
  private String id;

  private String type;

  private List<String> awards;

  private String name;

}