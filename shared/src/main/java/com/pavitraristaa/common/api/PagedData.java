package com.pavitraristaa.common.api;

import java.util.List;

public record PagedData<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
}
