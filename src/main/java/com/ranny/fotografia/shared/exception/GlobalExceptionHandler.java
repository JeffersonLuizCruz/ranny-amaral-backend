package com.ranny.fotografia.shared.exception;

import com.ranny.fotografia.shared.seguranca.borda.LimitePayloadExcedidoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Contrato unico de erro da API, agora em RFC 7807/9457.
 *
 * <p>PATTERN: Chain of Responsibility + Front Controller (Spring MVC).
 *
 * <p>O {@code @RestControllerAdvice} intercepta as excecoes de todos os
 * controllers e as traduz para {@link ProblemDetail}. Cada
 * {@code @ExceptionHandler} e um elo da cadeia: o Spring escolhe o handler mais
 * especifico para o tipo lancado.
 *
 * <p>Os campos {@code codigo}, {@code mensagem}, {@code caminho} e
 * {@code campos} sao extensoes de compatibilidade com o contrato anterior,
 * enquanto {@code type}, {@code title}, {@code status}, {@code detail} e
 * {@code instance} seguem o padrao.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> tratarApiException(ApiException ex,
                                                            HttpServletRequest request) {
        return resposta(ex.getStatus(), ex.getCodigo(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> tratarValidacao(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        List<CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoErro(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO",
                "Um ou mais campos estao invalidos.", request, campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> tratarValidacaoDeParametro(
            HandlerMethodValidationException ex, HttpServletRequest request) {
        List<CampoErro> campos = ex.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(erro -> new CampoErro(
                                String.valueOf(resultado.getMethodParameter().getParameterName()),
                                erro.getDefaultMessage())))
                .toList();
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO",
                "Um ou mais parametros estao invalidos.", request, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> tratarConstraint(ConstraintViolationException ex,
                                                          HttpServletRequest request) {
        List<CampoErro> campos = ex.getConstraintViolations().stream()
                .map(violacao -> new CampoErro(
                        violacao.getPropertyPath().toString(), violacao.getMessage()))
                .toList();
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO",
                "Um ou mais campos estao invalidos.", request, campos);
    }

    @ExceptionHandler(LimitePayloadExcedidoException.class)
    public ResponseEntity<ProblemDetail> tratarPayloadExcedido(
            LimitePayloadExcedidoException ex, HttpServletRequest request) {
        return resposta(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_EXCEDIDO",
                "O corpo da requisicao excede o limite permitido.", request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> tratarCorpoIlegivel(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "CORPO_INVALIDO",
                "O corpo da requisicao e invalido ou nao pode ser lido.", request, null);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ProblemDetail> tratarParametroInvalido(Exception ex,
                                                                 HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "Um parametro da requisicao e invalido.", request, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> tratarConcorrencia(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Conflito de concorrencia em {}: {}", request.getRequestURI(), ex.getMessage());
        return resposta(HttpStatus.CONFLICT, "CONFLITO_CONCORRENCIA",
                "O registro foi alterado por outra operacao. Recarregue e tente novamente.",
                request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> tratarIntegridade(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violacao de integridade em {}: {}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "CONFLITO_DE_DADOS",
                "A operacao conflita com dados existentes.", request, null);
    }

    @ExceptionHandler(ArmazenamentoException.class)
    public ResponseEntity<ProblemDetail> tratarArmazenamento(
            ArmazenamentoException ex, HttpServletRequest request) {
        log.error("Falha de armazenamento em {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ARMAZENAMENTO_INDISPONIVEL",
                "Nao foi possivel processar o arquivo no armazenamento.", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> tratarAcessoNegado(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                "Acesso negado.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> tratarInesperado(Exception ex,
                                                          HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Erro interno inesperado.", request, null);
    }

    private ResponseEntity<ProblemDetail> resposta(HttpStatus status,
                                                   String codigo,
                                                   String detalhe,
                                                   HttpServletRequest request,
                                                   List<CampoErro> campos) {
        ProblemDetail problema = Problemas.de(status, codigo, detalhe, request.getRequestURI());
        Problemas.adicionarCampos(problema, campos);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }
}
