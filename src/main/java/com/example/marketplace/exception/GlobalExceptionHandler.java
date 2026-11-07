package com.example.marketplace.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource Not Found");
        pd.setType(URI.create("urn:problem-type:not-found"));
        return pd;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource Not Found");
        pd.setType(URI.create("urn:problem-type:not-found"));
        return pd;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Access Denied");
        pd.setType(URI.create("urn:problem-type:access-denied"));
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Forbidden");
        pd.setType(URI.create("urn:problem-type:forbidden"));
        return pd;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        pd.setTitle("Authentication Failed");
        pd.setType(URI.create("urn:problem-type:unauthorized"));
        return pd;
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLocked(LockedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.LOCKED,
                "Account is temporarily locked due to multiple failed login attempts. Please try again later.");
        pd.setTitle("Account Locked");
        pd.setType(URI.create("urn:problem-type:account-locked"));
        return pd;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimitExceeded(RateLimitExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        pd.setTitle("Too Many Requests");
        pd.setType(URI.create("urn:problem-type:rate-limit-exceeded"));
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Bad Request");
        pd.setType(URI.create("urn:problem-type:bad-request"));
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict");
        pd.setType(URI.create("urn:problem-type:conflict"));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed");
        pd.setTitle("Validation Error");
        pd.setType(URI.create("urn:problem-type:validation-error"));
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
        pd.setTitle("Malformed Request");
        pd.setType(URI.create("urn:problem-type:malformed-request"));
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid parameter type for: " + ex.getName());
        pd.setTitle("Invalid Parameter");
        pd.setType(URI.create("urn:problem-type:invalid-parameter"));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled server exception: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal server error occurred");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("urn:problem-type:internal-error"));
        return pd;
    }
}
