package com.ranny.fotografia.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * PATTERN: Chain of Responsibility + Front Controller (Spring MVC).
 *
 * <p>O {@code @RestControllerAdvice} intercepta as excecoes de todos os
 * controllers e as traduz para um unico contrato {@link ErrorResponse}. Cada
 * {@code @ExceptionHandler} e um elo da cadeia: o Spring escolhe o handler mais
 * especifico para o tipo lancado.
 *
 * <p>Beneficios: controllers e services ficam livres de {@code try/catch} e de
 * detalhes HTTP; a API mantem consistencia de status e de corpo de erro mesmo
 * quando novos modulos sao adicionados.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> tratarApiException(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.de(ex.getStatus(), ex.getCodigo(), ex.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoErro(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return ResponseEntity.unprocessableEntity()
                .body(ErrorResponse.de(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO",
                        "Um ou mais campos estao invalidos.", request.getRequestURI(), campos));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> tratarValidacaoDeParametro(
            HandlerMethodValidationException ex, HttpServletRequest request) {
        List<CampoErro> campos = ex.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(erro -> new CampoErro(
                                String.valueOf(resultado.getMethodParameter().getParameterName()),
                                erro.getDefaultMessage())))
                .toList();
        return ResponseEntity.unprocessableEntity()
                .body(ErrorResponse.de(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO",
                        "Um ou mais parametros estao invalidos.", request.getRequestURI(), campos));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> tratarConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<CampoErro> campos = ex.getConstraintViolations().stream()
                .map(violacao -> new CampoErro(
                        violacao.getPropertyPath().toString(), violacao.getMessage()))
                .toList();
        return ResponseEntity.unprocessableEntity()
                .body(ErrorResponse.de(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO",
                        "Um ou mais campos estao invalidos.", request.getRequestURI(), campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> tratarCorpoIlegivel(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de(HttpStatus.BAD_REQUEST, "CORPO_INVALIDO",
                        "O corpo da requisicao e invalido ou nao pode ser lido.", request.getRequestURI(), null));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> tratarParametroInvalido(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                        "Um parametro da requisicao e invalido.", request.getRequestURI(), null));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> tratarConcorrencia(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Conflito de concorrencia em {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de(HttpStatus.CONFLICT, "CONFLITO_CONCORRENCIA",
                        "O registro foi alterado por outra operacao. Recarregue e tente novamente.",
                        request.getRequestURI(), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> tratarIntegridade(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violacao de integridade em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de(HttpStatus.CONFLICT, "CONFLITO_DE_DADOS",
                        "A operacao conflita com dados existentes.", request.getRequestURI(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> tratarInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.de(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                        "Erro interno inesperado.", request.getRequestURI(), null));
    }
}
