package com.example.cafe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
  private boolean success;
  private String message;
  private T data;

  public static <T> ApiResponse<T> ok(T data){
    return new ApiResponse<>(true, "OK", data);
  }

  public static <T> ApiResponse<T> error(String message){
    return new ApiResponse<>(false, message, null);
  }
}
