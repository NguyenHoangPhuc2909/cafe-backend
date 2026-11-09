package com.example.cafe.common.entity.inventory;

import com.example.cafe.common.entity.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "stocktakes", uniqueConstraints = @UniqueConstraint(
    name = "uk_stocktake_staff_request", columnNames = {"performed_by", "request_id"}))
public class Stocktake {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "request_id", nullable = false, updatable = false)
  private UUID requestId;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "performed_by", nullable = false, updatable = false)
  private User performedBy;
  @Column(nullable = false, updatable = false)
  private String performedByName;
  @Column(nullable = false, updatable = false)
  private Instant createdAt;
  @Column(nullable = false, updatable = false, length = 500)
  private String note;
  @ElementCollection
  @CollectionTable(name = "stocktake_items", joinColumns = @JoinColumn(name = "stocktake_id"))
  @OrderColumn(name = "line_number")
  private List<StocktakeItem> items = new ArrayList<>();
}
