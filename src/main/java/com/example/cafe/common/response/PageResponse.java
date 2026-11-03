package com.example.cafe.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageResponse<T> {
  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private boolean last;

  public static <T> PageResponse<T> from(Page<T> source) {
    return new PageResponse<>(
        source.getContent(),
        source.getNumber(),
        source.getSize(),
        source.getTotalElements(),
        source.getTotalPages(),
        source.isLast()
    );
  }
}
