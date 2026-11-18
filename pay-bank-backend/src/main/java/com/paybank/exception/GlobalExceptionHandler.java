package com.paybank.exception;

import java.util.stream.Collectors;

import com.paybank.common.web.InvalidPageQueryException;
import com.paybank.payment.exception.InvalidPaymentAmountException;
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

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    private final RefreshCookieFactory cookies;

    public GlobalExceptionHandler(RefreshCookieFactory cookies) {
        this.cookies = cookies;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        if (code != null) {
            problem.setProperty("code", code);
        }
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", e.getMessage(), null);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .body(problem(HttpStatus.UNAUTHORIZED, "Invalid refresh token", e.getMessage(), null));
    }

    /** Business rules (recipient not found, self payment, insufficient balance...) answer 422 with a code. */
    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violated", e.getMessage(), e.getCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Validation error", detail, VALIDATION_ERROR);
    }

    /** Out-of-range pagination parameters (PageQuery or the constraints of the generated API). */
    @ExceptionHandler({InvalidPageQueryException.class, HandlerMethodValidationException.class,
            ConstraintViolationException.class})
    public ProblemDetail handleInvalidParameters(Exception e) {
        String detail = e instanceof InvalidPageQueryException ? e.getMessage() : "Invalid request parameters";
        return problem(HttpStatus.BAD_REQUEST, "Validation error", detail, VALIDATION_ERROR);
    }

    @ExceptionHandler(InvalidPaymentAmountException.class)
    public ProblemDetail handleInvalidAmount(InvalidPaymentAmountException e) {
        return problem(HttpStatus.BAD_REQUEST, "Validation error", e.getMessage(), VALIDATION_ERROR);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", "Malformed request body", VALIDATION_ERROR);
    }
}
