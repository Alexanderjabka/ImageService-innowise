package io.github.alexanderjabka.imageservice.dto;

import java.time.LocalDateTime;

public class CommentResponse {
    
    private Long id;
    private Long imageId;
    private Long userId;
    private String text;
    private LocalDateTime createdAt;
    
    // Constructors
    public CommentResponse() {}
    
    public CommentResponse(Long id, Long imageId, Long userId, String text, LocalDateTime createdAt) {
        this.id = id;
        this.imageId = imageId;
        this.userId = userId;
        this.text = text;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getImageId() {
        return imageId;
    }
    
    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

