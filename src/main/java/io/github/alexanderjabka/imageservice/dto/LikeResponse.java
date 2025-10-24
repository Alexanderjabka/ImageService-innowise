package io.github.alexanderjabka.imageservice.dto;

import java.time.LocalDateTime;

public class LikeResponse {
    
    private Long id;
    private Long imageId;
    private Long userId;
    private LocalDateTime createdAt;
    private boolean liked;
    
    // Constructors
    public LikeResponse() {}
    
    public LikeResponse(Long id, Long imageId, Long userId, LocalDateTime createdAt, boolean liked) {
        this.id = id;
        this.imageId = imageId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.liked = liked;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isLiked() {
        return liked;
    }
    
    public void setLiked(boolean liked) {
        this.liked = liked;
    }
}

