package com.example.cafe.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Data 
@Entity 
@Table (name = "cafe_tables")
public class CafeTable {
  @Id
  @GeneratedValue (strategy = GenerationType.IDENTITY)
  private Long id;

  @Column (nullable = false, unique = true, length = 20)
  private String name;

  @Column(name = "qr_code", unique = true, nullable = false, updatable = false)
  private String qrCode;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @PrePersist
  private void generateQrCode() {
    if (qrCode == null || qrCode.isBlank()) {
      qrCode = UUID.randomUUID().toString();
    }
  }
}
