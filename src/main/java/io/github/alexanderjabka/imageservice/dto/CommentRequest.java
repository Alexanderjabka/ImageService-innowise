package io.github.alexanderjabka.imageservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequest {
    
    @NotBlank(message = "Text cannot be blank")
    @Size(max = 500, message = "Text cannot exceed 500 characters")
    private String text;
    
    // Constructors
    public CommentRequest() {}
    
    public CommentRequest(String text) {
        this.text = text;
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
}

