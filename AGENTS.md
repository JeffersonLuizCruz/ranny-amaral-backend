# AGENTS.md - API do CRM

Java 25, Spring Boot 4.1.2, Spring Modulith 2.1.1, JPA, PostgreSQL 16, Flyway,
S3 (Garage).

Rode sempre dentro deste diretório. Nunca use `mvn` do sistema.

## Comandos

| Objetivo | Comando |
|---|---|
| Dependências locais (Postgres + Garage) | `docker compose -f docker-compose.dev.yml up -d` |
| API local | `./mvnw spring-boot:run` |
| Suíte rápida (H2 + Spock) | `./mvnw test` |
| Somente Spock | `./mvnw -Dtest='*Spec' test` |
| Validação completa (exige Postgres) | `./mvnw verify` |
| Incluir storage (Garage) | `S3_TEST_ENDPOINT=http://localhost:3900 ./mvnw verify` |
| Empacotar | `./mvnw -DskipTests package` |

Validação mínima antes de concluir: `./mvnw test`. O CI roda `verify` com
PostgreSQL e Garage reais.

## Layout por módulo

```
modulo/
├── api/          controllers, DTOs, converters de enum
├── model/        entidades, value objects, enums
├── repository/   Spring Data
├── service/      regras
├── event/        eventos próprios
├── listener/     consumidores assíncronos
└── seed/         dados de demonstração
```

Pacotes: `shared`, `auth`, `cliente`, `pacote`, `agenda`, `contrato`, `arquivo`,
`armazenamento`, `galeria`, `financeiro`, `dashboard`, `notificacao`.

## Fronteiras

- Consumo síncrono só por porta pública: `*Consulta`.
- Consumo assíncrono por evento no outbox, com `@ApplicationModuleListener`.
- Nunca importe pacote interno de outro módulo.
- `ApplicationModules.verify()` precisa continuar verde.
- `shared` não depende de `auth`. Segurança de aplicação em `auth/security`; filtro
  de borda em `shared/seguranca/borda`.

## Entidades

- Composição, não herança de entidade.
- Sem `@Data`. Use `@Getter` e construtor protegido.
- `@Version` em toda entidade mutável (`lock_version`).
- `Auditoria` embutida para criação e alteração.
- Documentos grandes e imutáveis em `TEXT` via `AttributeConverter`; valores
  consultáveis em colunas relacionais.

## Banco

- Flyway é a fonte do schema. Hibernate só valida (`ddl-auto=validate`).
- Nunca edite migration aplicada. A próxima é um arquivo novo.
- Trocar `ddl-auto` para `update`/`create` é proibido.
- `event_publication.serialized_event` é `varchar(255)`: evento tem que ser compacto.

## Erros e dinheiro

- Um único `ProblemDetail` (RFC 7807/9457), com `codigo`, `mensagem`, `caminho`,
  `campos`.
- `codigo` é estável e usado pelo frontend.
- `BigDecimal` com `CalculadoraPagamento.normalizar`.
- Fuso do estúdio: `America/Sao_Paulo`. Persistência em UTC.

## Segurança

- JWT RS256, refresh em cookie `HttpOnly`.
- `/api/v1/public/**` aberto; o resto exige token.
- Segredos por variável de ambiente, nunca no código.
- Teste manual precisa de `User-Agent` e `Content-Type`, senão o filtro de borda
  rejeita antes do controller.

## Testes

- Spock para especificação (`*Spec`).
- H2 para a suíte rápida; `*IT` no `verify` com PostgreSQL real.
- `FakeObjectStorage` para storage; teste real só com `S3_TEST_ENDPOINT`.

## Armadilhas

- Editar migration aplicada.
- Importar pacote interno de outro módulo.
- Criar segundo formato de erro.
- Fazer upload passar pelo backend em vez de URL pré-assinada.
- Esquecer `@Version` em entidade mutável.
- Recalcular total financeiro fora do backend.

## Contexto de projeto

O domínio consolidado, os runbooks e o pipeline de release ficam no vault ICM em
`../icm-vault`. Para tarefas de código, este arquivo e o `README.md` bastam.
