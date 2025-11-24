package io.github.alexanderjabka.imageservice.repository;

import io.github.alexanderjabka.imageservice.entity.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
    Page<Image> findByUserId(Long userId, Pageable pageable);
}
