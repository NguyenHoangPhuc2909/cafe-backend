package com.example.cafe.feature.catalog.service;

import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.enums.DrinkSize;
import com.example.cafe.common.enums.ProductType;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import java.math.BigDecimal;

public final class ProductSizePricing {
  private ProductSizePricing() {}
  public static final BigDecimal LARGE_SURCHARGE = new BigDecimal("10000");

  public static boolean supportsSizes(Product product) {
    return Boolean.TRUE.equals(product.getSizeEnabled()) && product.getProductType() == ProductType.MADE_TO_ORDER;
  }

  public static DrinkSize resolveSize(Product product, DrinkSize requested) {
    if (!supportsSizes(product)) {
      if (requested != null) throw new AppException(ErrorCode.INVALID_PRODUCT_SIZE);
      return null;
    }
    return requested == null ? DrinkSize.M : requested;
  }

  public static BigDecimal price(Product product, DrinkSize size) {
    if (product.getPrice() == null || product.getPrice().signum() < 0) {
      throw new AppException(ErrorCode.PRODUCT_CONFIGURATION_INVALID);
    }
    return size == DrinkSize.L ? product.getPrice().add(LARGE_SURCHARGE) : product.getPrice();
  }
}
