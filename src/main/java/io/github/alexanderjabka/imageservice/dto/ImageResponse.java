package io.github.alexanderjabka.imageservice.dto;

import java.time.LocalDateTime;

public class ImageResponse {
    
    private Long id;
    private String url;
    private String description;
    private Long userId;
    private LocalDateTime uploadedAt;
    private int likesCount;
    private int commentsCount;
    
    // Constructors
    public ImageResponse() {}
    
    public ImageResponse(Long id, String url, String description, Long userId, 
                        LocalDateTime uploadedAt, int likesCount, int commentsCount) {
        this.id = id;
        this.url = url;
        this.description = description;
        this.userId = userId;
        this.uploadedAt = uploadedAt;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public int getLikesCount() {
        return likesCount;
    }
    
    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }
    
    public int getCommentsCount() {
        return commentsCount;
    }
    
    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }
}

