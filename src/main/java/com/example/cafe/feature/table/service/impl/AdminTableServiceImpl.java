package com.example.cafe.feature.table.service.impl;

import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.entity.table.TableSession;
import com.example.cafe.common.enums.TableSessionStatus;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.table.dto.request.AdminTableRequest;
import com.example.cafe.feature.table.dto.request.UpdateTableStatusRequest;
import com.example.cafe.feature.table.dto.response.AdminTableResponse;
import com.example.cafe.feature.table.repository.CafeTableRepository;
import com.example.cafe.feature.table.repository.TableSessionRepository;
import com.example.cafe.feature.table.service.AdminTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminTableServiceImpl implements AdminTableService {
  private final CafeTableRepository tables;
  private final TableSessionRepository sessions;

  @Override
  @Transactional(readOnly = true)
  public List<AdminTableResponse> list() {
    var historyIds = new HashSet<>(sessions.findDistinctTableIds());
    Map<Long, Long> openSessionByTableId = new HashMap<>();
    for (TableSession session : sessions.findByStatus(TableSessionStatus.OPEN)) {
      if (session.getTable() != null && session.getTable().getId() != null) {
        openSessionByTableId.putIfAbsent(session.getTable().getId(), session.getId());
      }
    }

    return tables.findAll(Sort.by("id").ascending()).stream()
        .map(table -> {
          Long openSessionId = openSessionByTableId.get(table.getId());
          boolean hasHistory = openSessionId != null || historyIds.contains(table.getId());
          return toResponse(table, openSessionId, hasHistory);
        })
        .toList();
  }

  @Override
  @Transactional
  public AdminTableResponse create(AdminTableRequest request) {
    String name = request.name().strip();
    if (tables.existsByNameIgnoreCaseAndIdNot(name, -1L)) {
      throw new AppException(ErrorCode.TABLE_ALREADY_EXISTS);
    }

    CafeTable table = new CafeTable();
    table.setName(name);
    table.setQrCode(UUID.randomUUID().toString());
    table.setIsActive(true);

    CafeTable saved = saveHandlingDuplicate(table);
    return toResponse(saved, null, false);
  }

  @Override
  @Transactional
  public AdminTableResponse rename(Long id, AdminTableRequest request) {
    CafeTable table = findForUpdate(id);
    String name = request.name().strip();
    if (tables.existsByNameIgnoreCaseAndIdNot(name, id)) {
      throw new AppException(ErrorCode.TABLE_ALREADY_EXISTS);
    }

    table.setName(name);
    CafeTable saved = saveHandlingDuplicate(table);
    return buildLiveResponse(saved);
  }

  @Override
  @Transactional
  public AdminTableResponse updateStatus(Long id, UpdateTableStatusRequest request) {
    CafeTable table = findForUpdate(id);
    boolean active = Boolean.TRUE.equals(request.isActive());

    var openSession = sessions.findByTable_IdAndStatus(id, TableSessionStatus.OPEN);
    if (!active && openSession.isPresent()) {
      throw new AppException(ErrorCode.TABLE_HAS_OPEN_SESSION);
    }

    table.setIsActive(active);
    CafeTable saved = tables.saveAndFlush(table);
    Long openSessionId = openSession.map(TableSession::getId).orElse(null);
    boolean hasHistory = openSessionId != null || sessions.existsByTable_Id(id);
    return toResponse(saved, openSessionId, hasHistory);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    CafeTable table = findForUpdate(id);
    if (sessions.findByTable_IdAndStatus(id, TableSessionStatus.OPEN).isPresent()) {
      throw new AppException(ErrorCode.TABLE_HAS_OPEN_SESSION);
    }
    if (sessions.existsByTable_Id(id)) {
      throw new AppException(ErrorCode.TABLE_IN_USE);
    }

    try {
      tables.delete(table);
      tables.flush();
    } catch (DataIntegrityViolationException e) {
      throw new AppException(ErrorCode.TABLE_IN_USE);
    }
  }

  private CafeTable findForUpdate(Long id) {
    return tables.findByIdForUpdate(id)
        .orElseThrow(() -> new AppException(ErrorCode.TABLE_NOT_FOUND));
  }

  private CafeTable saveHandlingDuplicate(CafeTable table) {
    try {
      return tables.saveAndFlush(table);
    } catch (DataIntegrityViolationException e) {
      throw new AppException(ErrorCode.TABLE_ALREADY_EXISTS);
    }
  }

  private AdminTableResponse buildLiveResponse(CafeTable table) {
    Long openSessionId = sessions.findByTable_IdAndStatus(table.getId(), TableSessionStatus.OPEN)
        .map(TableSession::getId)
        .orElse(null);
    boolean hasHistory = openSessionId != null || sessions.existsByTable_Id(table.getId());
    return toResponse(table, openSessionId, hasHistory);
  }

  private AdminTableResponse toResponse(CafeTable table, Long openSessionId, boolean hasHistory) {
    boolean hasOpenSession = openSessionId != null;
    return new AdminTableResponse(
        table.getId(),
        table.getName(),
        table.getQrCode(),
        Boolean.TRUE.equals(table.getIsActive()),
        hasOpenSession,
        openSessionId,
        hasHistory,
        !hasOpenSession,
        !hasOpenSession && !hasHistory
    );
  }
}
