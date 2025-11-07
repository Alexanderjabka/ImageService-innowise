package io.github.alexanderjabka.imageservice.repository;

import io.github.alexanderjabka.imageservice.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByImageId(Long imageId);
    Optional<Comment> findByIdAndUserId(Long id, Long userId);
}
