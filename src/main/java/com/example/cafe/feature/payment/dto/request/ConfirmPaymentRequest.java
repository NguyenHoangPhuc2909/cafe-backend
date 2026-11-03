package com.example.cafe.feature.payment.dto.request;

import com.example.cafe.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmPaymentRequest {

  @NotNull(message = "Vui lòng chọn phương thức thanh toán")
  private PaymentMethod method;
}
