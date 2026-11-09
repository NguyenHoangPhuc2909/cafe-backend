package com.example.cafe.feature.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.example.cafe.common.enums.OrderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffOrderRequest extends CreateOrderRequest {
  @Min(1)
  private Long sessionId;

  @NotNull
  private OrderType orderType = OrderType.DINE_IN;
}
