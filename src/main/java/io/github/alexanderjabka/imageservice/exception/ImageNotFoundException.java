package io.github.alexanderjabka.imageservice.exception;

public class ImageNotFoundException extends RuntimeException {
    public ImageNotFoundException(Long imageId) {
        super("Image not found with id: " + imageId);
    }
}
