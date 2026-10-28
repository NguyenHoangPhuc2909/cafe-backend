package com.example.cafe.feature.table.service.impl;

import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.TableSessionStatus;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.table.dto.response.TableSessionAccessResponse;
import com.example.cafe.feature.table.dto.response.TableSessionResponse;
import com.example.cafe.feature.table.mapper.TableSessionMapper;
import com.example.cafe.feature.table.repository.CafeTableRepository;
import com.example.cafe.feature.table.repository.TableSessionRepository;
import com.example.cafe.feature.table.service.TableSessionService;
import com.example.cafe.security.TableSessionTokenProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TableSessionServiceImpl implements TableSessionService {
  private final CafeTableRepository cafeTableRepository;
  private final TableSessionRepository tableSessionRepository;
  private final TableSessionTokenProvider tableSessionTokenProvider;

  @Override
  @Transactional
  public TableSessionResponse openSession(Long tableId) {
    CafeTable table = cafeTableRepository.findByIdForUpdate(tableId).orElseThrow(() -> new AppException(ErrorCode.TABLE_NOT_FOUND));

    if (!Boolean.TRUE.equals(table.getIsActive())) {
      throw new AppException(ErrorCode.TABLE_INACTIVE);
    }

    boolean alreadyOpen = tableSessionRepository.findByTable_IdAndStatus(tableId, TableSessionStatus.OPEN).isPresent();

    if (alreadyOpen) {
      throw new AppException(ErrorCode.TABLE_SESSION_ALREADY_OPEN);
    }

    TableSession session = new TableSession();
    session.setTable(table);
    session.setStatus(TableSessionStatus.OPEN);

    TableSession saved = tableSessionRepository.save(session);
    return TableSessionMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public TableSessionResponse getOpenSession(Long tableId) {
    CafeTable table = cafeTableRepository.findById(tableId)
        .orElseThrow(() -> new AppException(ErrorCode.TABLE_NOT_FOUND));

    TableSession session = tableSessionRepository.findByTable_IdAndStatus(tableId, TableSessionStatus.OPEN)
        .orElseThrow(() -> new AppException(ErrorCode.TABLE_SESSION_NOT_FOUND));

    return TableSessionMapper.toResponse(session);
  }

  @Override
  @Transactional(readOnly = true)
  public TableSessionAccessResponse grantGuestAccess(String qrCode) {
    CafeTable table = cafeTableRepository.findByQrCode(qrCode)
        .orElseThrow(() -> new AppException(ErrorCode.TABLE_NOT_FOUND));

    if (!Boolean.TRUE.equals(table.getIsActive())) {
      throw new AppException(ErrorCode.TABLE_INACTIVE);
    }

    TableSession session = tableSessionRepository.findByTable_IdAndStatus(table.getId(), TableSessionStatus.OPEN)
        .orElseThrow(() -> new AppException(ErrorCode.TABLE_SESSION_NOT_FOUND));

    Instant expiresAt = Instant.now().plusSeconds(7200).truncatedTo(ChronoUnit.SECONDS);

    String accessToken = tableSessionTokenProvider.generateToken(session.getId(), table.getId(), expiresAt);
    return TableSessionAccessResponse.builder()
        .sessionId(session.getId())
        .tableId(table.getId())
        .tableName(table.getName())
        .accessToken(accessToken)
        .expiresAt(expiresAt)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public TableSessionResponse getGuestSession(String token) {
    if (token == null || token.isBlank()) {
      throw new AppException(ErrorCode.INVALID_TABLE_SESSION_TOKEN);
    }

    Long sessionId;
    Long tableId;

    try {
      Claims claims = tableSessionTokenProvider.parseToken(token);

      sessionId = Long.valueOf(claims.getSubject());
      tableId = Long.valueOf(claims.get("tableId").toString());

      if (sessionId <= 0 || tableId <= 0) {
        throw new IllegalArgumentException("ID không hợp lệ");
      }
    } catch (JwtException | IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_TABLE_SESSION_TOKEN);
    }

    TableSession session = tableSessionRepository.findById(sessionId)
        .orElseThrow(() -> new AppException(ErrorCode.INVALID_TABLE_SESSION_TOKEN));

    CafeTable table = session.getTable();
    if (!table.getId().equals(tableId)) {
      throw new AppException(ErrorCode.INVALID_TABLE_SESSION_TOKEN);
    }

    if (session.getStatus() != TableSessionStatus.OPEN) {
      throw new AppException(ErrorCode.TABLE_SESSION_CLOSED);
    }

    if (!Boolean.TRUE.equals(table.getIsActive())) {
      throw new AppException(ErrorCode.TABLE_INACTIVE);
    }
    return TableSessionMapper.toResponse(session);
  }
}
