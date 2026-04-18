package com.personal.happygallery.application.search.port.out;

import java.time.LocalDate;
import java.util.List;

public interface AdminSearchPort<S, R> {

    List<R> search(S status, LocalDate dateFrom, LocalDate dateTo,
                   String keyword, int offset, int size);

    long count(S status, LocalDate dateFrom, LocalDate dateTo, String keyword);
}
