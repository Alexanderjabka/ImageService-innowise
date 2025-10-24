package io.github.alexanderjabka.imageservice.exception;

public class ImageNotFoundException extends RuntimeException {
    
    public ImageNotFoundException(String message) {
        super(message);
    }
}

