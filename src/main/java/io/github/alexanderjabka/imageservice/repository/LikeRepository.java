package io.github.alexanderjabka.imageservice.repository;

import io.github.alexanderjabka.imageservice.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByImageIdAndUserId(Long imageId, Long userId);
    long countByImageId(Long imageId);
    boolean existsByImageIdAndUserId(Long imageId, Long userId);
}
