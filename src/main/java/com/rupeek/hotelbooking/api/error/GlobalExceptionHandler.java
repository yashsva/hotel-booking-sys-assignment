package com.rupeek.hotelbooking.api.error;

import com.rupeek.hotelbooking.domain.booking.InvalidBookingStateException;
import com.rupeek.hotelbooking.domain.common.DomainException;
import com.rupeek.hotelbooking.domain.common.DomainValidationException;
import com.rupeek.hotelbooking.domain.common.ResourceNotFoundException;
import com.rupeek.hotelbooking.domain.inventory.RoomUnavailableException;
import com.rupeek.hotelbooking.domain.payment.RefundFailedException;
import com.rupeek.hotelbooking.domain.payment.UnsupportedPaymentMethodException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

/**
 * Turns domain exceptions into RFC 7807 problem responses. Spring's own errors (bad JSON,
 * bean validation, missing parameters/headers) are handled by the base class.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Map<Class<? extends DomainException>, HttpStatus> STATUS_BY_EXCEPTION = Map.of(
            DomainValidationException.class, HttpStatus.BAD_REQUEST,
            ResourceNotFoundException.class, HttpStatus.NOT_FOUND,
            RoomUnavailableException.class, HttpStatus.CONFLICT,
            InvalidBookingStateException.class, HttpStatus.CONFLICT,
            UnsupportedPaymentMethodException.class, HttpStatus.UNPROCESSABLE_CONTENT,
            RefundFailedException.class, HttpStatus.BAD_GATEWAY);

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex) {
        HttpStatus status = STATUS_BY_EXCEPTION.getOrDefault(ex.getClass(), HttpStatus.UNPROCESSABLE_CONTENT);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("error", ex.getClass().getSimpleName());
        return problem;
    }

    /** Adds the failing fields to Spring's bean-validation response so clients see what to fix. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> fieldErrors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = ex.getBody();
        problem.setDetail("Request validation failed");
        problem.setProperty("fieldErrors", fieldErrors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }
}
