package io.github.alexanderjabka.imageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private long totalItems;
    private int totalPages;
    private int page;
    private int size;
    private List<T> items;

    public List<T> getImages() {
        return items;
    }

    public List<T> getData() {
        return items;
    }

    public long getTotal() {
        return totalItems;
    }

    public int getCurrentPage() {
        return page;
    }
}
