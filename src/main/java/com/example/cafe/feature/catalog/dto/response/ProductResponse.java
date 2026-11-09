package com.example.cafe.feature.catalog.dto.response;

import com.example.cafe.common.enums.ProductType;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import com.example.cafe.common.enums.DrinkSize;

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
  private String unit;

  private Boolean isActive;
  private Boolean sizeEnabled;
  private List<SizeOption> sizes;
  private ProductType productType;

  public record SizeOption(DrinkSize size, BigDecimal price) {}

  private Long categoryId;
  private String categoryName;
}
