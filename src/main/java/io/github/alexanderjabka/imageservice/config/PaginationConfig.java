package io.github.alexanderjabka.imageservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Configuration
public class PaginationConfig {
    
    @Bean
    public Pageable defaultPageable() {
        return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "uploadedAt"));
    }
    
    /**
     * Create pageable with custom parameters
     */
    public static Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
    
    /**
     * Create pageable with default sorting
     */
    public static Pageable createPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
    }
}

