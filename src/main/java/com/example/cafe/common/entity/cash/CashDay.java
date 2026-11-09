package com.example.cafe.common.entity.cash;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Entity
@Table(name = "cash_days")
public class CashDay {
  @Id @Column(name = "business_date")
  private LocalDate businessDate;
  @Column(name = "opening_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal openingAmount = new BigDecimal("500000");
  @Column(nullable = false)
  private long revision;
}
