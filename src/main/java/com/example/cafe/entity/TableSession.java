package com.example.cafe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "table_sessions")
public class TableSession {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "table_id", nullable = false)
  private CafeTable table;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TableSessionStatus status = TableSessionStatus.OPEN;

  @Column(name = "opened_at", nullable = false, updatable = false)
  private LocalDateTime openedAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @PrePersist
  private void onCreate(){
    openedAt = LocalDateTime.now();
  }
}
