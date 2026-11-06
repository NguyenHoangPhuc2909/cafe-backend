package com.example.cafe.feature.order;

import com.example.cafe.common.entity.catalog.Product;
import com.example.cafe.common.entity.order.Order;
import com.example.cafe.common.entity.order.OrderItem;
import com.example.cafe.common.enums.DrinkSize;
import com.example.cafe.common.enums.DrinkOptionLevel;
import com.example.cafe.common.enums.ProductType;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.catalog.mapper.ProductMapper;
import com.example.cafe.feature.order.dto.request.GuestOrderRequest;
import com.example.cafe.feature.order.mapper.OrderMapper;
import com.example.cafe.feature.order.service.GuestOrderLines;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuestOrderLinesTest {
  private Product product(boolean sizes, ProductType type) {
    Product product = new Product();
    product.setId(12L);
    product.setName("Cà phê");
    product.setPrice(new BigDecimal("30000"));
    product.setProductType(type);
    product.setSizeEnabled(sizes);
    return product;
  }
  private GuestOrderRequest.ItemRequest item(DrinkSize size, int quantity) {
    var item = new GuestOrderRequest.ItemRequest();
    item.setProductId(12L);
    item.setSize(size);
    item.setQuantity(quantity);
    return item;
  }
  private GuestOrderRequest request(GuestOrderRequest.ItemRequest... items) {
    var request = new GuestOrderRequest();
    request.setItems(List.of(items));
    return request;
  }

  @Test void keepsSizesSeparateAndMergesOnlyMatchingSizes() {
    var product = product(true, ProductType.MADE_TO_ORDER);
    var lines = GuestOrderLines.collect(request(item(null, 1), item(DrinkSize.M, 1), item(DrinkSize.L, 3)), Map.of(12L, product));
    assertEquals(2, lines.size());
    assertEquals(DrinkSize.M, lines.get(0).size());
    assertEquals(2, lines.get(0).quantity());
    assertEquals(new BigDecimal("30000"), lines.get(0).price());
    assertEquals(DrinkSize.L, lines.get(1).size());
    assertEquals(new BigDecimal("40000"), lines.get(1).price());
    assertEquals(new BigDecimal("180000"), lines.stream().map(line -> line.price().multiply(BigDecimal.valueOf(line.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  @Test void rejectsSizeForPackagedOrNonSizedProducts() {
    for (Product product : List.of(product(false, ProductType.MADE_TO_ORDER), product(true, ProductType.PACKAGED))) {
      assertTrue(ProductMapper.toResponse(product).getSizes().isEmpty());
      var error = assertThrows(AppException.class, () -> GuestOrderLines.collect(request(item(DrinkSize.L, 1)), Map.of(12L, product)));
      assertEquals(ErrorCode.INVALID_PRODUCT_SIZE, error.getErrorCode());
      assertNull(GuestOrderLines.collect(request(item(null, 1)), Map.of(12L, product)).getFirst().size());
    }
  }

  @Test void publishesServerPricesAndKeepsHistoricalSizeAndPrice() {
    var product = product(true, ProductType.MADE_TO_ORDER);
    var menu = ProductMapper.toResponse(product);
    assertEquals(List.of(DrinkSize.M, DrinkSize.L), menu.getSizes().stream().map(option -> option.size()).toList());
    assertEquals(new BigDecimal("40000"), menu.getSizes().get(1).price());
    var item = new OrderItem();
    item.setProduct(product);
    item.setQuantity(2);
    item.setSize(DrinkSize.L);
    item.setPrice(new BigDecimal("40000"));
    product.setPrice(new BigDecimal("50000"));
    product.setSizeEnabled(false);
    var response = OrderMapper.toResponse(new Order(), List.of(item)).getItems().getFirst();
    assertEquals(DrinkSize.L, response.getSize());
    assertEquals(new BigDecimal("40000"), response.getPrice());
  }

  @Test void rejectsOverflowAndInvalidCatalogPrice() {
    var product = product(true, ProductType.MADE_TO_ORDER);
    assertThrows(AppException.class, () -> GuestOrderLines.collect(request(item(DrinkSize.L, Integer.MAX_VALUE), item(DrinkSize.L, 1)), Map.of(12L, product)));
    product.setPrice(new BigDecimal("-1"));
    assertThrows(AppException.class, () -> GuestOrderLines.collect(request(item(DrinkSize.M, 1)), Map.of(12L, product)));
  }

  @Test void separatesDrinkPreferencesAndMergesImplicitAndExplicitDefaults() {
    var product = product(true, ProductType.MADE_TO_ORDER);
    var regular = item(DrinkSize.M, 1);
    var explicitDefault = item(DrinkSize.M, 2);
    explicitDefault.setIceLevel(DrinkOptionLevel.DEFAULT);
    explicitDefault.setSweetnessLevel(DrinkOptionLevel.DEFAULT);
    var less = item(DrinkSize.M, 1);
    less.setIceLevel(DrinkOptionLevel.LESS);
    less.setSweetnessLevel(DrinkOptionLevel.NONE);
    var lines = GuestOrderLines.collect(request(regular, explicitDefault, less), Map.of(12L, product));
    assertEquals(2, lines.size());
    assertEquals(3, lines.getFirst().quantity());
    assertEquals(DrinkOptionLevel.DEFAULT, lines.getFirst().iceLevel());
    assertEquals(DrinkOptionLevel.LESS, lines.get(1).iceLevel());
    assertEquals(DrinkOptionLevel.NONE, lines.get(1).sweetnessLevel());
    assertEquals(lines.getFirst().price(), lines.get(1).price());
  }

  @Test void rejectsDrinkPreferencesForPackagedProductsAndPreservesSnapshots() {
    var packaged = product(false, ProductType.PACKAGED);
    var request = item(null, 1);
    request.setIceLevel(DrinkOptionLevel.LESS);
    assertEquals(ErrorCode.INVALID_DRINK_CUSTOMIZATION, assertThrows(AppException.class,
        () -> GuestOrderLines.collect(request(request), Map.of(12L, packaged))).getErrorCode());
    var stored = new OrderItem();
    stored.setProduct(product(true, ProductType.MADE_TO_ORDER));
    stored.setIceLevel(DrinkOptionLevel.NONE);
    stored.setSweetnessLevel(DrinkOptionLevel.LESS);
    stored.setPrice(new BigDecimal("30000"));
    stored.setQuantity(1);
    var response = OrderMapper.toResponse(new Order(), List.of(stored)).getItems().getFirst();
    assertEquals(DrinkOptionLevel.NONE, response.getIceLevel());
    assertEquals(DrinkOptionLevel.LESS, response.getSweetnessLevel());
  }
}
