package com.example.cafe.feature.order.service;

import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.enums.DrinkSize;
import com.example.cafe.common.enums.DrinkOptionLevel;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.catalog.service.ProductSizePricing;
import com.example.cafe.feature.order.dto.request.GuestOrderRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuestOrderLines {
  private GuestOrderLines() {}
  private record Key(Long productId, DrinkSize size, DrinkOptionLevel iceLevel, DrinkOptionLevel sweetnessLevel) {}
  public record Line(Product product, DrinkSize size, int quantity, BigDecimal price, DrinkOptionLevel iceLevel, DrinkOptionLevel sweetnessLevel) {}

  private static DrinkOptionLevel resolveOption(Product product, DrinkOptionLevel requested) {
    if (!ProductSizePricing.supportsSizes(product)) {
      if (requested != null && requested != DrinkOptionLevel.DEFAULT) {
        throw new AppException(ErrorCode.INVALID_DRINK_CUSTOMIZATION);
      }
      return null;
    }
    return requested == null ? DrinkOptionLevel.DEFAULT : requested;
  }

  public static List<Line> collect(GuestOrderRequest request, Map<Long, Product> products) {
    Map<Key, Integer> quantities = new LinkedHashMap<>();
    try {
      for (var item : request.getItems()) {
        Product product = products.get(item.getProductId());
        DrinkSize size = ProductSizePricing.resolveSize(product, item.getSize());
        DrinkOptionLevel ice = resolveOption(product, item.getIceLevel());
        DrinkOptionLevel sweetness = resolveOption(product, item.getSweetnessLevel());
        quantities.merge(new Key(product.getId(), size, ice, sweetness), item.getQuantity(), Math::addExact);
      }
    } catch (ArithmeticException exception) {
      throw new AppException(ErrorCode.INVALID_ORDER_QUANTITY);
    }
    return quantities.entrySet().stream().map(entry -> {
      Product product = products.get(entry.getKey().productId());
      DrinkSize size = entry.getKey().size();
      return new Line(product, size, entry.getValue(), ProductSizePricing.price(product, size), entry.getKey().iceLevel(), entry.getKey().sweetnessLevel());
    }).toList();
  }
}
