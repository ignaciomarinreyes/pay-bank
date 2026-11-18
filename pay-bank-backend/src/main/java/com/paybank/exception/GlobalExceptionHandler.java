package com.paybank.exception;

import java.util.stream.Collectors;

import com.paybank.common.web.InvalidPageQueryException;
import com.paybank.security.RefreshCookieFactory;
import com.paybank.user.exception.InvalidCredentialsException;
import com.paybank.user.exception.InvalidRefreshTokenException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final RefreshCookieFactory cookies;

    public GlobalExceptionHandler(RefreshCookieFactory cookies) {
        this.cookies = cookies;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problem.setTitle("Invalid credentials");
        return problem;
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problem.setTitle("Invalid refresh token");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation error");
        return problem;
    }

    /** Out-of-range pagination parameters (PageQuery or the constraints of the generated API). */
    @ExceptionHandler({InvalidPageQueryException.class, HandlerMethodValidationException.class,
            ConstraintViolationException.class})
    public ProblemDetail handleInvalidParameters(Exception e) {
        String detail = e instanceof InvalidPageQueryException ? e.getMessage() : "Invalid request parameters";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation error");
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
        problem.setTitle("Bad request");
        return problem;
    }
}
