package io.github.alexanderjabka.imageservice.dto;

import jakarta.validation.constraints.Size;

public class ImageUploadRequest {
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    public ImageUploadRequest() {}
    
    public ImageUploadRequest(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}

