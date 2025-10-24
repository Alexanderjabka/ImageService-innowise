package io.github.alexanderjabka.imageservice.service;

import io.github.alexanderjabka.imageservice.dto.CommentRequest;
import io.github.alexanderjabka.imageservice.dto.CommentResponse;
import io.github.alexanderjabka.imageservice.dto.PageResponse;
import io.github.alexanderjabka.imageservice.entity.Comment;
import io.github.alexanderjabka.imageservice.exception.CommentNotFoundException;
import io.github.alexanderjabka.imageservice.exception.ImageNotFoundException;
import io.github.alexanderjabka.imageservice.repository.CommentRepository;
import io.github.alexanderjabka.imageservice.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private ImageRepository imageRepository;
    
    public CommentResponse addComment(Long imageId, CommentRequest request, Long userId) {
        if (!imageRepository.existsById(imageId)) {
            throw new ImageNotFoundException("Image not found with id: " + imageId);
        }
        
        Comment comment = new Comment(imageId, userId, request.getText());
        Comment savedComment = commentRepository.save(comment);
        
        return convertToResponse(savedComment);
    }
    
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getImageComments(Long imageId, Pageable pageable) {
        if (!imageRepository.existsById(imageId)) {
            throw new ImageNotFoundException("Image not found with id: " + imageId);
        }
        
        Page<Comment> commentPage = commentRepository.findByImageId(imageId, pageable);
        return convertToPageResponse(commentPage);
    }
    
    public CommentResponse updateComment(Long imageId, Long commentId, CommentRequest request, Long userId) {
        if (!imageRepository.existsById(imageId)) {
            throw new ImageNotFoundException("Image not found with id: " + imageId);
        }
        
        Comment comment = commentRepository.findByIdAndImageId(commentId, imageId)
            .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));
        
        if (!comment.getUserId().equals(userId)) {
            throw new CommentNotFoundException("You don't have permission to update this comment");
        }
        
        comment.setText(request.getText());
        Comment updatedComment = commentRepository.save(comment);
        
        return convertToResponse(updatedComment);
    }
    
    public void deleteComment(Long imageId, Long commentId, Long userId) {
        if (!imageRepository.existsById(imageId)) {
            throw new ImageNotFoundException("Image not found with id: " + imageId);
        }
        
        Comment comment = commentRepository.findByIdAndImageId(commentId, imageId)
            .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));
        
        if (!comment.getUserId().equals(userId)) {
            throw new CommentNotFoundException("You don't have permission to delete this comment");
        }
        
        commentRepository.delete(comment);
    }
    
    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long imageId, Long commentId) {
        if (!imageRepository.existsById(imageId)) {
            throw new ImageNotFoundException("Image not found with id: " + imageId);
        }
        
        Comment comment = commentRepository.findByIdAndImageId(commentId, imageId)
            .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));
        
        return convertToResponse(comment);
    }
    
    private CommentResponse convertToResponse(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getImageId(),
            comment.getUserId(),
            comment.getText(),
            comment.getCreatedAt()
        );
    }
    
    private PageResponse<CommentResponse> convertToPageResponse(Page<Comment> commentPage) {
        List<CommentResponse> content = commentPage.getContent().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return new PageResponse<>(
            content,
            commentPage.getNumber(),
            commentPage.getSize(),
            commentPage.getTotalElements(),
            commentPage.getTotalPages(),
            commentPage.isFirst(),
            commentPage.isLast(),
            commentPage.hasNext(),
            commentPage.hasPrevious()
        );
    }
}
