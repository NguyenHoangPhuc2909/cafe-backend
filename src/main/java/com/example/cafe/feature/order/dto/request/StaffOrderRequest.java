package com.example.cafe.feature.order.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffOrderRequest extends CreateOrderRequest {
  @Min(1)
  private Long sessionId;
}