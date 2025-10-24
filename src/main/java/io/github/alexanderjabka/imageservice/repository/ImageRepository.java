package io.github.alexanderjabka.imageservice.repository;

import io.github.alexanderjabka.imageservice.entity.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    Optional<Image> findByIdAndUserId(Long id, Long userId);

    Page<Image> findByUserId(Long userId, Pageable pageable);

    Page<Image> findAllByOrderByUploadedAtDesc(Pageable pageable);
}
