package ma.nqiya.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates exceptions into RFC 9457 problem details carrying a stable {@code code} property for the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException ex) {
        return problem(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Some fields are invalid");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail handleMissingPart(MissingServletRequestPartException ex) {
        return problem(HttpStatus.BAD_REQUEST, "MISSING_PART", "Missing '" + ex.getRequestPartName() + "'");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleUploadSize(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "PHOTO_TOO_LARGE", "The photo is too large");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleConcurrency(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
                "Someone else updated this at the same time, please retry");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not allowed to do this");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNoResource(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong, please try again");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String message) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        return pd;
    }
}
