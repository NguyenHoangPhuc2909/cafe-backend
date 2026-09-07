package com.example.cafe.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
  private Long id;
  private String name;
  private BigDecimal price;
  private String description;
  private String imageUrl;

  private Boolean isActive;

  private Long categoryId;
  private String categoryName;
}
