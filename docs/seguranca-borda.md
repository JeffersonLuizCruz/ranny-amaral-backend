# Segurança de borda e Problem Details — o que foi implementado

**Projeto:** `ranny-amaral-backend`  
**Data:** 2026-11-06  
**Stack:** Java 25, Spring Boot 4.1.2, Spring Security 7, Spring Modulith 2.1.1, Spock/Groovy 5

---

## 1. Objetivo e escopo

Esta entrega adiciona uma camada de segurança na borda da API, antes de qualquer Controller, Service, JPA ou object storage.

### Implementado

- Filtro `OncePerRequestFilter` no início da cadeia do Spring Security.
- Limite de payload por `Content-Length` e por contagem real de bytes.
- Exigência de `User-Agent`.
- Validação de `Content-Type` em operações mutantes com corpo.
- Bloqueio de User-Agents de scanners e padrões óbvios de path traversal/arquivos sensíveis.
- Ponto de extensão desacoplado para bloqueio/rate limiting por IP (Redis/cache no futuro).
- Configuração de Spring Security com CORS, sessão stateless e handlers 401/403.
- Migração de todo o contrato de erro para **RFC 7807/9457 (`ProblemDetail`)**.
- Suíte Spock cobrindo os cenários do filtro e do novo contrato de erro.

### Fora de escopo nesta entrega

- **Parte B (Resilience4j — Circuit Breaker/Bulkhead):** não implementada.
- Autenticação/JWT: implementada posteriormente no módulo `auth` (JWT RS256, refresh cookie, perfis).
- Gateway de pagamento/Pix automático: o Pix é confirmado manualmente pelo staff na galeria.

### Por que a Parte B não foi implementada

O fluxo de compra de foto extra é local:

`GaleriaService.confirmarExtras(...)` → grava `PedidoExtras` como pago → inicia edição.

Não existe chamada externa de pagamento. Aplicar Circuit Breaker nesse fluxo seria artificial. O object storage (Garage/S3) também foi descartado como alvo nesta fase. O desenho de resiliência fica documentado como próxima etapa, caso surja um serviço externo real (PSP Pix, storage resiliente etc.).

---

## 2. Arquitetura

```text
com.ranny.fotografia.shared.seguranca
├── ConfiguracaoSegurancaBorda.java      # composition root do filtro/validadores
├── ConfiguracaoSegurancaWeb.java        # SecurityFilterChain + registro do filtro
└── borda
    ├── FiltroBordaSeguranca.java
    ├── PropriedadesSegurancaBorda.java
    ├── ProblemaBorda.java
    ├── RespostaProblemaSeguranca.java
    ├── RequisicaoComLimiteTamanho.java
    ├── LimitePayloadExcedidoException.java
    ├── ip
    │   ├── AvaliadorAcessoIp.java
    │   ├── DecisaoAcessoIp.java
    │   └── AvaliadorAcessoIpLiberado.java
    └── validador
        ├── ValidadorRequisicaoBorda.java
        ├── ValidadorTamanhoPayload.java
        ├── ValidadorAcessoIp.java
        ├── ValidadorHeadersObrigatorios.java
        ├── ValidadorContentTypeMutacao.java
        └── ValidadorPadroesScanning.java
```

### Fluxo da requisição

```text
Cliente
  │
  ▼
CorsFilter (Spring Security)
  │
  ▼
FiltroBordaSeguranca
  ├─ ignora OPTIONS/ERROR/Swagger/OpenAPI
  ├─ ValidadorTamanhoPayload        (413)
  ├─ ValidadorAcessoIp              (403)
  ├─ ValidadorHeadersObrigatorios   (400)
  ├─ ValidadorContentTypeMutacao    (415)
  ├─ ValidadorPadroesScanning       (403)
  └─ RequisicaoComLimiteTamanho
        │
        ▼
DispatcherServlet → Controller → Service → JPA/Storage
```

---

## 3. Decisões de design e padrões

### 3.1 Filter Chain + Chain of Responsibility

**Onde:** `FiltroBordaSeguranca` + `ValidadorRequisicaoBorda`.

**Por quê:** a borda precisa aplicar várias regras independentes antes do MVC. Colocar tudo em um único método criaria um bloco difícil de testar e alterar.

**Benefício:** cada regra é um elo testável, com ordem explícita via `@Order`. Novas regras entram como novos beans, sem alterar o filtro.

### 3.2 Strategy

**Onde:** `AvaliadorAcessoIp`.

**Por quê:** o mecanismo de bloqueio por IP pode ser no-op, Redis, cache local ou um serviço externo.

**Benefício:** o filtro depende da abstração; trocar a implementação não exige mudança na cadeia.

### 3.3 Null Object

**Onde:** `AvaliadorAcessoIpLiberado`.

**Por quê:** o projeto ainda não possui Redis/cache de IP. A ausência de regra não deve gerar `null` nem `if` espalhado.

**Benefício:** comportamento default seguro e previsível; o adaptador futuro substitui o bean.

### 3.4 Decorator

**Onde:** `RequisicaoComLimiteTamanho`.

**Por quê:** `Content-Length` pode estar ausente ou mentir. É necessário limitar o stream real, sem bufferizar o payload inteiro.

**Benefício:** Controllers e converters continuam recebendo `HttpServletRequest` normal; o limite é imposto de forma transparente.

### 3.5 Factory

**Onde:** `Problemas`.

**Por quê:** filtro, Security e `GlobalExceptionHandler` precisam produzir o mesmo formato RFC 7807/9457.

**Benefício:** um único ponto de montagem de `type`, `title`, `status`, `detail`, `instance` e extensões de compatibilidade.

> `OncePerRequestFilter` e `HttpServletRequestWrapper` são extensões exigidas pelo framework. Não há herança de domínio; o comportamento variável é composto por injeção.

---

## 4. Regras do filtro

| Regra | Implementação | Resposta |
|---|---|---|
| Payload por `Content-Length` | `ValidadorTamanhoPayload` | `413 PAYLOAD_EXCEDIDO` |
| Payload real/chunked | `RequisicaoComLimiteTamanho` | `413 PAYLOAD_EXCEDIDO` |
| `Content-Length` inválido | `ValidadorTamanhoPayload` | `400 CONTENT_LENGTH_INVALIDO` |
| IP bloqueado | `ValidadorAcessoIp` + `AvaliadorAcessoIp` | `403 ACESSO_NEGADO` |
| `User-Agent` ausente | `ValidadorHeadersObrigatorios` | `400 HEADER_OBRIGATORIO` |
| `User-Agent` muito longo | `ValidadorHeadersObrigatorios` | `400 HEADER_INVALIDO` |
| `Content-Type` ausente em mutação com corpo | `ValidadorContentTypeMutacao` | `415 CONTENT_TYPE_OBRIGATORIO` |
| `Content-Type` não JSON | `ValidadorContentTypeMutacao` | `415 CONTENT_TYPE_INVALIDO` |
| Scanner/UA malicioso | `ValidadorPadroesScanning` | `403 PADRAO_MALICIOSO` |
| Path traversal/arquivo sensível | `ValidadorPadroesScanning` | `403 PADRAO_MALICIOSO` |

### O filtro não lê o corpo

A validação de JSON malformado continua no MVC, via `HttpMessageNotReadableException` → `400 CORPO_INVALIDO`. Isso evita double-read, perda de performance e consumo desnecessário de memória.

### Caminhos ignorados

- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/webjars/**`
- `/actuator/health`
- `/error`
- `/favicon.ico`

`OPTIONS` e dispatch de `ERROR` também são ignorados.

---

## 5. Configuração de segurança

`ConfiguracaoSegurancaWeb` declara:

- CSRF desabilitado (API stateless);
- CORS com `CorsConfigurationSource` compartilhado com o MVC;
- sessão `STATELESS`;
- `formLogin`, `httpBasic` e `logout` desabilitados;
- `permitAll()` apenas para rotas públicas de negócio, login/refresh/logout e health;
- JWT obrigatório nas demais rotas administrativas;
- filtro inserido **depois do `CorsFilter`**;
- handlers de `401`/`403` retornando `ProblemDetail`;
- `FilterRegistrationBean` desabilitado para evitar execução duplicada do filtro.

`UserDetailsServiceAutoConfiguration` foi excluído em `FotografiaApplication` para não gerar usuário/senha default em log.

### Autenticação

A cadeia agora vive em `auth/security/ConfiguracaoSegurancaWeb`. O filtro de borda continua em
`shared/seguranca` e permanece na mesma posição. Os handlers de 401/403 continuam respondendo
`ProblemDetail`.

---

## 6. Contrato de erro RFC 7807/9457

### Antes

`ErrorResponse` com `timestamp`, `status`, `error`, `codigo`, `mensagem`, `caminho`, `campos`.

### Depois

`ProblemDetail` com campos padrão + extensões de compatibilidade:

```json
{
  "type": "urn:ranny:fotografia:problema:payload-excedido",
  "title": "Payload Too Large",
  "status": 413,
  "detail": "O corpo da requisicao excede o limite permitido.",
  "instance": "/api/v1/clientes",
  "codigo": "PAYLOAD_EXCEDIDO",
  "mensagem": "O corpo da requisicao excede o limite permitido.",
  "caminho": "/api/v1/clientes",
  "timestamp": "2026-11-06T12:00:00Z"
}
```

- `Content-Type: application/problem+json`
- `type`: URN estável por código (`urn:ranny:fotografia:problema:{slug}`)
- `codigo`, `mensagem`, `caminho`, `campos`: extensões de compatibilidade
- `ErrorResponse` foi removido

### Status preservados

| Status | Código |
|---|---|
| 400 | `CORPO_INVALIDO`, `PARAMETRO_INVALIDO`, `CONTENT_LENGTH_INVALIDO` |
| 403 | `ACESSO_NEGADO`, `PADRAO_MALICIOSO` |
| 404 | `RECURSO_NAO_ENCONTRADO` |
| 409 | `CONFLITO_CONCORRENCIA`, `CONFLITO_DE_DADOS` |
| 413 | `PAYLOAD_EXCEDIDO` |
| 415 | `CONTENT_TYPE_OBRIGATORIO`, `CONTENT_TYPE_INVALIDO` |
| 422 | `VALIDACAO` |
| 500 | `ERRO_INTERNO`, `ARMAZENAMENTO_INDISPONIVEL` |

---

## 7. Configuração

`src/main/resources/application.properties`:

```properties
app.seguranca.borda.habilitado=true
app.seguranca.borda.max-payload-bytes=2097152
app.seguranca.borda.exigir-user-agent=true
app.seguranca.borda.max-user-agent-length=512
app.seguranca.borda.exigir-content-type-em-mutacoes=true
app.seguranca.borda.bloquear-padroes-scanning=true
app.seguranca.borda.ip-check-habilitado=false
app.seguranca.borda.confiar-em-proxy=false
```

Todas aceitam override por variável de ambiente, por exemplo:

```bash
APP_SEGURANCA_BORDA_MAX_PAYLOAD=5242880
APP_SEGURANCA_BORDA_IP_CHECK=true
APP_SEGURANCA_BORDA_CONFIAR_PROXY=true
```

### IP atrás de proxy

`confiar-em-proxy=false` por padrão. Só habilite quando a aplicação estiver atrás de um proxy reverso confiável; caso contrário, um cliente pode forjar `X-Forwarded-For`.

---

## 8. Como estender

### 8.1 Bloqueio/rate limiting por IP com Redis

Crie um bean que implemente `AvaliadorAcessoIp`:

```java
@Component
@Primary
public class AvaliadorAcessoIpRedis implements AvaliadorAcessoIp {
    @Override
    public DecisaoAcessoIp avaliar(String ip, HttpServletRequest request) {
        // consulta Redis/cache
        return DecisaoAcessoIp.liberado();
    }
}
```

Ative:

```properties
app.seguranca.borda.ip-check-habilitado=true
```

O `AvaliadorAcessoIpLiberado` default não será usado.

### 8.2 Rate limiting por rota/usuário

Crie um novo `ValidadorRequisicaoBorda` com `@Order` apropriado. O filtro o injeta automaticamente; nenhuma alteração em `FiltroBordaSeguranca` é necessária.

### 8.3 Novo tipo de conteúdo permitido

A regra atual aceita `application/json` e qualquer subtipo `+json`. Para multipart ou outro tipo, evolua `ValidadorContentTypeMutacao` com uma allowlist configurável.

---

## 9. Testes

### `FiltroBordaSegurancaSpec`

9 cenários via `where:`:

- requisição válida → 200;
- sem `User-Agent` → 400;
- scanner `sqlmap` → 403;
- payload acima do limite → 413;
- `Content-Type` ausente → 415;
- `Content-Type: text/plain` → 415;
- path `/.env` → 403;
- IP `10.9.9.9` bloqueado → 403.

Também valida `application/problem+json`, `status`, `codigo`, `instance` e `timestamp`.

### `RequisicaoComLimiteTamanhoSpec`

- corpo acima do limite lança `LimitePayloadExcedidoException`;
- corpo exatamente no limite é lido integralmente.

### `ProblemaHttpSpec`

- 404 de recurso inexistente com `ProblemDetail`;
- 422 de validação com `campos` preservados.

### Resultado

```text
Tests run: 178, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Comandos:

```bash
./mvnw -Dtest='FiltroBordaSegurancaSpec,ProblemaHttpSpec' test
./mvnw test
./mvnw verify
```

---

## 10. Limitações e próximos passos

1. **Autenticação/JWT** implementada; `permitAll()` ficou restrito às rotas públicas e ao login/refresh.
2. **Rate limiting distribuído** não foi implementado; o ponto de extensão está pronto.
3. **Parte B (Resilience4j)** não foi implementada nesta fase.
4. **Pix** continua manual: o staff confirma o recebimento na galeria.
5. Se um serviço externo real surgir (PSP Pix, storage resiliente, notificações), aplicar Circuit Breaker/Bulkhead via decorator na porta correspondente, mantendo controllers/services isolados da infraestrutura.

---

## 11. Arquivos alterados/criados

### Criados

- `shared/seguranca/ConfiguracaoSegurancaBorda.java`
- `auth/security/ConfiguracaoSegurancaWeb.java`
- `shared/seguranca/borda/FiltroBordaSeguranca.java`
- `shared/seguranca/borda/PropriedadesSegurancaBorda.java`
- `shared/seguranca/borda/ProblemaBorda.java`
- `shared/seguranca/borda/RespostaProblemaSeguranca.java`
- `shared/seguranca/borda/RequisicaoComLimiteTamanho.java`
- `shared/seguranca/borda/LimitePayloadExcedidoException.java`
- `shared/seguranca/borda/ip/AvaliadorAcessoIp.java`
- `shared/seguranca/borda/ip/DecisaoAcessoIp.java`
- `shared/seguranca/borda/ip/AvaliadorAcessoIpLiberado.java`
- `shared/seguranca/borda/validador/ValidadorRequisicaoBorda.java`
- `shared/seguranca/borda/validador/ValidadorTamanhoPayload.java`
- `shared/seguranca/borda/validador/ValidadorAcessoIp.java`
- `shared/seguranca/borda/validador/ValidadorHeadersObrigatorios.java`
- `shared/seguranca/borda/validador/ValidadorContentTypeMutacao.java`
- `shared/seguranca/borda/validador/ValidadorPadroesScanning.java`
- `shared/exception/Problemas.java`
- `src/test/groovy/.../seguranca/borda/ConfiguracaoTesteBorda.groovy`
- `src/test/groovy/.../seguranca/borda/FiltroBordaSegurancaSpec.groovy`
- `src/test/groovy/.../seguranca/borda/RequisicaoComLimiteTamanhoSpec.groovy`
- `src/test/groovy/.../exception/ProblemaHttpSpec.groovy`

### Alterados

- `pom.xml` — `spring-boot-starter-security`
- `FotografiaApplication.java` — exclusão do `UserDetailsServiceAutoConfiguration`
- `shared/exception/GlobalExceptionHandler.java` — `ProblemDetail`
- `shared/exception/ErrorResponse.java` — removido
- `shared/config/CorsConfig.java` — `CorsConfigurationSource`
- `src/main/resources/application.properties`
- `src/test/resources/application.properties`
- `README.md`

---

## 12. Referências

- RFC 7807 (Problem Details for HTTP APIs)
- RFC 9457 (obsoleta a RFC 7807)
- Spring Security 7 — `SecurityFilterChain`, `CorsFilter`
- Spring Framework 7 — `ProblemDetail`, `ProblemDetailJacksonMixin`
- Spring Boot 4 — `FilterRegistrationBean`, `@ConfigurationProperties`
