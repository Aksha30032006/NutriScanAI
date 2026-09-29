package com.nutriscan;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> api(ApiException e) {
        return ResponseEntity.status(e.status).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<Map<String, String>> upload(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Please upload a food image under 8 MB"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> other(Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body(Map.of("error", "Something went wrong on the server"));
    }
}
