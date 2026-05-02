package com.logic.advice;

import com.logic.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {


    // NoSuchElementException (optional if using Optional.get())
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleNoSuchElement(
            NoSuchElementException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request
        );
    }


    // Endpoint not found (wrong URL)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                "Requested resource not found",
                HttpStatus.NOT_FOUND,
                request
        );
    }

    // ResourceNotFoundException (MAIN FIX)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request
        );
    }

    @ExceptionHandler({
            AccessDeniedException.class,
            AuthorizationDeniedException.class
    })
    public ResponseEntity<ApiResponse<ApiError>> handleAccessDenied(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                "You are not allowed to access this resource",
                HttpStatus.FORBIDDEN,
                request
        );
    }

    @ExceptionHandler({
            AuthenticationException.class,
            AuthenticationCredentialsNotFoundException.class
    })
    public ResponseEntity<ApiResponse<ApiError>> handleAuthenticationException(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                "Authentication is required to access this resource",
                HttpStatus.UNAUTHORIZED,
                request
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ApiResponse<ApiError>> handleBadRequest(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ApiError>> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                "Something went wrong",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    private ResponseEntity<ApiResponse<ApiError>> buildErrorResponse(
            String message,
            HttpStatus status,
            HttpServletRequest request
    ) {
        ApiError apiError = ApiError.builder()
                .message(message)
                .status(status.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(new ApiResponse<>(apiError), status);
    }

}
