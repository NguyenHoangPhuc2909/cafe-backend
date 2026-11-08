package com.example.cafe.feature.inventory.service.impl;

import com.example.cafe.common.entity.inventory.StockMovement;
import com.example.cafe.common.enums.StockMovementType;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.common.response.PageResponse;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.inventory.dto.request.StockReceiptRequest;
import com.example.cafe.feature.inventory.dto.response.StockMovementResponse;
import com.example.cafe.feature.inventory.repository.IngredientRepository;
import com.example.cafe.feature.inventory.repository.StockMovementRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cafe.common.enums.ProductType;
import com.example.cafe.feature.catalog.repository.ProductRepository;
import com.example.cafe.feature.inventory.dto.response.PackagedStockResponse;
import com.example.cafe.feature.inventory.service.InventoryService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final IngredientRepository ingredientRepository;
  private final StockMovementRepository stockMovementRepository;

  @Override
  @Transactional(readOnly = true)
  public List<PackagedStockResponse> getPackagedStock() {
    return productRepository.findByProductTypeOrderByIdAsc(ProductType.PACKAGED)
        .stream()
        .map(product -> new PackagedStockResponse(product.getId(), product.getName(), product.getStockQuantity(), product.getIsActive(), product.getUnit()))
        .toList();
  }

  @Override
  @Transactional
  public StockMovementResponse receiveStock(StockReceiptRequest request, String username) {
    if (request == null || request.getRequestId() == null) {
      throw new AppException(ErrorCode.INVALID_STOCK_RECEIPT);
    }

    Long ingredientId = request.getIngredientId();
    Long productId = request.getProductId();
    Double quantity = request.getQuantity();
    String note = request.getNote();

    if ((ingredientId == null) == (productId == null)
        || (ingredientId != null && ingredientId <= 0)
        || (productId != null && productId <= 0)
        || quantity == null
        || !Double.isFinite(quantity)
        || quantity <= 0
        || note == null
        || note.isBlank()
        || note.length() > 500) {
      throw new AppException(ErrorCode.INVALID_STOCK_RECEIPT);
    }

    var staff = userRepository.findByUsernameForUpdate(username)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

    var existing = stockMovementRepository.findByPerformedBy_IdAndRequestId(
        staff.getId(),
        request.getRequestId()
    );

    if (existing.isPresent()) {
      StockMovement saved = existing.get();

      Long savedIngredientId = saved.getIngredient() == null
          ? null
          : saved.getIngredient().getId();

      Long savedProductId = saved.getProduct() == null
          ? null
          : saved.getProduct().getId();

      boolean sameContent =
          saved.getType() == StockMovementType.RECEIPT
              && Objects.equals(savedIngredientId, ingredientId)
              && Objects.equals(savedProductId, productId)
              && Objects.equals(saved.getQuantityDelta(), quantity)
              && Objects.equals(saved.getNote(), note.strip());

      if (!sameContent) {
        throw new AppException(ErrorCode.STOCK_RECEIPT_CONFLICT);
      }

      return toResponse(saved);
    }

    StockMovement movement = new StockMovement();
    movement.setRequestId(request.getRequestId());
    movement.setType(StockMovementType.RECEIPT);
    movement.setPerformedBy(staff);
    movement.setNote(note.strip());
    movement.setQuantityDelta(quantity);

    if (ingredientId != null) {
      var ingredient = ingredientRepository.findByIdForUpdate(ingredientId)
          .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));

      Double before = ingredient.getStockQuantity();

      if (before == null || !Double.isFinite(before) || before < 0) {
        throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
      }

      double after = before + quantity;
      if (!Double.isFinite(after) || after <= before) {
        throw new AppException(ErrorCode.INVALID_STOCK_RECEIPT);
      }

      ingredient.setStockQuantity(after);
      movement.setIngredient(ingredient);
      movement.setAvailableBefore(before);
      movement.setAvailableAfter(after);
    } else {
      // Hàng đóng gói phải nhập số nguyên.
      if (quantity != Math.rint(quantity)) {
        throw new AppException(ErrorCode.PACKAGED_QUANTITY_NOT_INTEGER);
      }

      if (quantity > Integer.MAX_VALUE) {
        throw new AppException(ErrorCode.INVALID_STOCK_RECEIPT);
      }

      var product = productRepository.findByIdForUpdate(productId)
          .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

      if (product.getProductType() != ProductType.PACKAGED) {
        throw new AppException(ErrorCode.INVALID_STOCK_RECEIPT);
      }

      Integer before = product.getStockQuantity();
      if (before == null || before < 0) {
        throw new AppException(ErrorCode.INVENTORY_DATA_INVALID);
      }

      long after = (long) before + quantity.longValue();
      if (after > Integer.MAX_VALUE) {
        throw new AppException(ErrorCode.INVALID_STOCK_RECEIPT);
      }

      product.setStockQuantity((int) after);
      movement.setProduct(product);
      movement.setAvailableBefore(before.doubleValue());
      movement.setAvailableAfter((double) after);
    }

    // Cộng tồn và ghi lịch sử trong cùng transaction.
    movement = stockMovementRepository.saveAndFlush(movement);

    return toResponse(movement);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<StockMovementResponse> getReceiptHistory(
      int page,
      int size,
      LocalDate fromDate,
      LocalDate toDate,
      Long ingredientId,
      Long productId
  ) {
    if ((fromDate != null && toDate != null && fromDate.isAfter(toDate))
        || LocalDate.MAX.equals(toDate)
        || (ingredientId != null && ingredientId <= 0)
        || (productId != null && productId <= 0)
        || (ingredientId != null && productId != null)) {
      throw new AppException(ErrorCode.INVALID_STOCK_HISTORY_QUERY);
    }

    var zone = ZoneId.of("Asia/Ho_Chi_Minh");

    var fromInstant = fromDate == null ? null : fromDate.atStartOfDay(zone).toInstant();

    var toExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay(zone).toInstant();

    Specification<StockMovement> spec = (root, query, cb) -> {
      var conditions = new ArrayList<Predicate>();

      conditions.add(
          cb.equal(root.get("type"), StockMovementType.RECEIPT)
      );

      if (fromInstant != null) {
        conditions.add(
            cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant)
        );
      }

      if (toExclusive != null) {
        conditions.add(
            cb.lessThan(root.get("createdAt"), toExclusive)
        );
      }

      if (ingredientId != null) {
        conditions.add(
            cb.equal(root.get("ingredient").get("id"), ingredientId)
        );
      }

      if (productId != null) {
        conditions.add(
            cb.equal(root.get("product").get("id"), productId)
        );
      }

      return cb.and(conditions.toArray(new Predicate[0]));
    };

    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);

    var pageable = PageRequest.of(
        safePage,
        safeSize,
        Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
        )
    );

    var movements = stockMovementRepository.findAll(spec, pageable);

    return PageResponse.from(movements.map(this::toResponse));
  }

  private StockMovementResponse toResponse(StockMovement movement) {
    var ingredient = movement.getIngredient();
    var product = movement.getProduct();
    var staff = movement.getPerformedBy();

    String staffName = staff.getFullName();
    if (staffName == null || staffName.isBlank()) {
      staffName = staff.getUsername();
    }

    return new StockMovementResponse(
        movement.getId(),
        movement.getType(),
        ingredient == null ? null : ingredient.getId(),
        product == null ? null : product.getId(),
        ingredient != null ? ingredient.getName() : product.getName(),
        ingredient != null ? ingredient.getUnit() : product.getUnit(),
        movement.getQuantityDelta(),
        movement.getAvailableBefore(),
        movement.getAvailableAfter(),
        staff.getId(),
        staffName,
        movement.getNote(),
        movement.getCreatedAt()
    );
  }
}
