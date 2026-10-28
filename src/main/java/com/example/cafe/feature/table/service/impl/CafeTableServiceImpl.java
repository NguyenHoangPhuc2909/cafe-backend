package com.example.cafe.feature.table.service.impl;

import com.example.cafe.common.entity.table.CafeTable;
import com.example.cafe.common.exception.AppException;
import com.example.cafe.common.response.ErrorCode;
import com.example.cafe.feature.table.dto.request.CafeTableRequest;
import com.example.cafe.feature.table.dto.response.CafeTableResponse;
import com.example.cafe.feature.table.mapper.CafeTableMapper;
import com.example.cafe.feature.table.repository.CafeTableRepository;
import com.example.cafe.feature.table.service.CafeTableService;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeTableServiceImpl implements CafeTableService {

  private final CafeTableRepository cafeTableRepository;

  @Override
  @Transactional
  public CafeTableResponse createTable(CafeTableRequest request) {
    String name = request.getName().strip();

    if (cafeTableRepository.existsByName(name)) {
      throw new AppException(ErrorCode.TABLE_ALREADY_EXISTS);
    }

    CafeTable table = new CafeTable();
    table.setName(name);

    CafeTable saved;
    try {
      saved = cafeTableRepository.saveAndFlush(table);
    } catch (DataIntegrityViolationException e) {
      Throwable cause = e.getMostSpecificCause();

      if (cause instanceof SQLException sqlException
          && "23505".equals(sqlException.getSQLState())) {
        throw new AppException(ErrorCode.TABLE_ALREADY_EXISTS);
      }

      throw e;
    }
    return CafeTableMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CafeTableResponse> getAllTables() {
    return cafeTableRepository.findAll(Sort.by("id").ascending())
        .stream().map(cafeTable -> CafeTableMapper.toResponse(cafeTable)).toList();
  }

  @Override
  public CafeTableResponse getTableByQrCode(String qrCode) {
    CafeTable table = cafeTableRepository.findByQrCode(qrCode)
        .orElseThrow(() -> new AppException(ErrorCode.TABLE_NOT_FOUND));

    if(!Boolean.TRUE.equals(table.getIsActive())){
      throw new AppException(ErrorCode.TABLE_INACTIVE);
    }
    return CafeTableMapper.toResponse(table);
  }
}
