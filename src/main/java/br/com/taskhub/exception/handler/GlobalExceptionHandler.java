package br.com.taskhub.exception.handler;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.taskhub.exception.ConflictException;
import br.com.taskhub.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@RestControllerAdvice 
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    public record FieldViolation(String field, String message) {

    }
    // 404
    @ExceptionHandler (ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso não encontrado");
        return problem;
    }

    // 409 - regra de negócio (inclui InvalidStatusTransitionException, que é subclasse)
    @ExceptionHandler (ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        log.warn("Conflito de regra de negócio: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflito");
        return problem;
    }

    // 409 - restrição violada no banco
    @ExceptionHandler (DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A operação viola uma restrição de integridade dos dados");
        problem.setTitle("Conflito");
        return problem;
    }

    // 500 - qualquer coisa não prevista
    @ExceptionHandler (Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado");
        problem.setTitle("Erro interno");
        return problem;
    }

    // 400 - Bean Validation (@NotBlank, @Size, @FutureOrPresent...)
    @Override 
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream().map(error -> new FieldViolation(error.getField(), error.getDefaultMessage())).toList();
        ProblemDetail problem = ex.getBody();
        problem.setTitle("Dados de entrada inválidos");
        problem.setDetail("Um ou mais campos inválidos");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    // 400 - JSON malformado ou valor fora do enum (ex.: "status": "FOO")
    @Override 
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou malformado");
        problem.setTitle("Requisição inválida");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    // 400 - Ordenação por propriedade inexistente (ex.: ?sort=foo)
    @ExceptionHandler (PropertyReferenceException.class)
    public ProblemDetail handleInvalidSort(PropertyReferenceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Não é possível ordenar por '" + ex.getPropertyName() + "'");
        problem.setTitle("Parâmetro de ordenação inválido");
        return problem;
    }
}