package io.github.alexanderjabka.imageservice.repository;

import io.github.alexanderjabka.imageservice.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByImageIdAndUserId(Long imageId, Long userId);

    boolean existsByImageIdAndUserId(Long imageId, Long userId);

    void deleteByImageIdAndUserId(Long imageId, Long userId);
}
