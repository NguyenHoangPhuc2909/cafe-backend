package com.example.cafe.feature.table.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
public class CafeTableRequest {

  @NotBlank(message = "Tên bàn không được để trống")
  @Size(max = 10, message = "Tên bàn tối đa 10 ký tự")
  private String name;
}
