package io.github.alexanderjabka.imageservice.controller;

import io.github.alexanderjabka.imageservice.dto.CommentRequest;
import io.github.alexanderjabka.imageservice.dto.CommentResponse;
import io.github.alexanderjabka.imageservice.dto.PageResponse;
import io.github.alexanderjabka.imageservice.security.JwtUtil;
import io.github.alexanderjabka.imageservice.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/images")
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/{imageId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long imageId,
            @Valid @RequestBody CommentRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = jwtUtil.extractUserId(httpRequest);
        CommentResponse response = commentService.addComment(imageId, request, userId);
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{imageId}/comments")
    public ResponseEntity<PageResponse<CommentResponse>> getImageComments(
            @PathVariable Long imageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        PageResponse<CommentResponse> response = commentService.getImageComments(imageId, pageable);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @PutMapping("/{imageId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long imageId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = jwtUtil.extractUserId(httpRequest);
        CommentResponse response = commentService.updateComment(imageId, commentId, request, userId);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @DeleteMapping("/{imageId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long imageId,
            @PathVariable Long commentId,
            HttpServletRequest httpRequest) {
        
        Long userId = jwtUtil.extractUserId(httpRequest);
        commentService.deleteComment(imageId, commentId, userId);
        
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @GetMapping("/{imageId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> getCommentById(
            @PathVariable Long imageId,
            @PathVariable Long commentId) {
        
        CommentResponse response = commentService.getCommentById(imageId, commentId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

