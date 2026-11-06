# Fotografia CRM — Backend

API REST do CRM de estúdio fotográfico, construída com **Java 25**, **Spring Boot 4.1.2**, **Spring Modulith 2.1.1**, **Spring Data JPA**, **PostgreSQL 16** e **Flyway**.

O núcleo cobre **clientes, pacotes, agenda e contratos** (incluindo modelos, configuração do estúdio, aceite público e paginação/busca). Galerias, financeiro, upload de mídia e autenticação ficam para as próximas fases.

## Requisitos

- Java 25 (`java -version`)
- Maven 3.9+ (o wrapper já está configurado: `./mvnw`)
- Docker + Docker Compose para o PostgreSQL local

## Como rodar

```bash
docker compose -f docker-compose.dev.yml up -d
./mvnw spring-boot:run
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- PostgreSQL: `localhost:5432`, banco/usuário/senha `photoizer`
  - inspeção: `docker compose -f docker-compose.dev.yml exec postgres psql -U photoizer -d photoizer`

O Flyway aplica as migrações no primeiro boot e o Hibernate roda com `ddl-auto=validate`. O perfil `dev` popula dados de demonstração (`app.seed.enabled=true`) com os mesmos IDs do frontend (`cli-marina`, `pkg-gestante`, `apt-001`, `ct-001`, etc.). O seed só roda quando as tabelas estão vazias.

Para apontar para outro banco, use `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`.

> Se já existir outro PostgreSQL ocupando a porta 5432, pare-o antes de subir o compose deste projeto (ou ajuste a porta e a URL do datasource).

## Testes

```bash
./mvnw test                       # H2 + Spock (rápido)
./mvnw -Dtest='*Spec' test        # somente Spock
./mvnw verify                     # H2 + PostgresSchemaIT (requer Postgres)
```

Os testes rápidos usam **H2 em memória**, `create-drop`, Flyway desabilitado e `app.seed.enabled=false` (`src/test/resources/application.properties`). O `PostgresSchemaIT` roda no `verify` com o perfil `postgres`, aplica as migrações Flyway e valida o schema real com `ddl-auto=validate` (use `POSTGRES_TEST_URL`/`POSTGRES_TEST_USERNAME`/`POSTGRES_TEST_PASSWORD` para apontar o banco).

A suíte cobre:

- CRUD e validações de clientes, pacotes e agenda;
- paginação, ordenação estável, busca textual e validação de parâmetros;
- congelamento das condições comerciais do pacote;
- numeração de contratos (`CT-AAAA-0001`) e protocolos de aceite (`ACE-AAAA-0001`);
- ciclo de vida do link público (geração, visualização, aceite, expiração e cancelamento);
- pagamentos de entrada/saldo;
- sincronização contrato → agenda por eventos duráveis (`@ApplicationModuleListener`);
- concorrência otimista (`@Version`) e conflito de atualização;
- verificação das fronteiras dos módulos (`ApplicationModules.verify()`).

## Arquitetura

```
com.ranny.fotografia
├── shared/      Shared Kernel: auditoria, pagamentos, JSON, erros, eventos e portas
├── cliente/     Cadastro de clientes (API pública ClienteConsulta)
├── pacote/      Catálogo de pacotes (API pública PacoteConsulta)
├── agenda/      Agendamentos, pagamentos e congelamento de condições
└── contrato/    Contratos, modelos, estúdio e aceite público
```

- Cada módulo expõe apenas o necessário no pacote raiz; o restante é interno.
- A comunicação síncrona usa **portas públicas** (`ClienteConsulta`, `PacoteConsulta`, `AgendamentoConsulta`).
- A comunicação assíncrona usa **eventos duráveis** (`ContratoStatusAlteradoEvent`, `ContratoExcluidoEvent`) consumidos por `@ApplicationModuleListener` — o contrato não conhece a agenda. O registry do Spring Modulith persiste as publicações na tabela `event_publication` e garante reentrega.
- Entidades usam **composição** (`Auditoria`, `PagamentoEntrada`, `PagamentoSaldo`, `CondicoesPacote`), sem herança de entidades e sem `@Data`.
- Documentos grandes e imutáveis (`snapshot`, `cláusulas`, `histórico`, `aceite`) são persistidos como JSON em colunas `TEXT` via `AttributeConverter`.
- Valores consultáveis (status, número, valores) ficam em colunas relacionais.
- Erros seguem um único contrato (`ErrorResponse`) com 400/404/409/410/422/500. Conflitos de concorrência retornam `409 CONFLITO_CONCORRENCIA`.

## Migrações

- `src/main/resources/db/migration/V1__baseline.sql` cria todo o schema do núcleo (tabelas, constraints e índices) e as tabelas do outbox (`event_publication`, `event_publication_archive`).
- `src/main/resources/db/migration/V2__contratos_busca_paginacao.sql` adiciona `cliente_nome`/`pacote_nome` em `contratos` (com backfill a partir do snapshot), índices de busca/paginação e o índice de status/período da agenda.
- O Flyway é a fonte de verdade do schema; o Hibernate apenas valida (`ddl-auto=validate`).
- **Nunca edite uma migração já aplicada.** Mudanças entram em `V3__...`, `V4__...`, etc.
- `spring.flyway.baseline-on-migrate=true` protege bancos não vazios sem histórico; em banco novo, a V1 é aplicada normalmente.
- O evento de contrato é compacto (chaves JSON curtas) para caber no `serialized_event varchar(255)`; `EventSerializationSpec` garante o limite.

## Hardening

- **Concorrência otimista:** todas as entidades mutáveis possuem `@Version` (`lock_version`). Atualizações concorrentes do mesmo registro resultam em `409`.
- **Eventos duráveis:** `spring-modulith-starter-jpa` persiste eventos em `event_publication`; a tabela é criada na V1 com `serialized_event varchar(255)` e o payload do evento é compacto.
- **Entrega eventual:** com o registry, o listener de contrato → agenda roda após o commit; a UI é atualizada localmente de imediato e o backend converge na sequência.
- **CI:** `.github/workflows/ci.yml` executa a suíte H2 e um job PostgreSQL com service container, Flyway e `validate`.

## Paginação e busca

As listagens de clientes, pacotes, agenda e contratos aceitam:

| Parâmetro | Regra |
|---|---|
| `pagina` | 0-based, default `0` |
| `tamanho` | default `20`, entre `1` e `100` |
| `q` | busca textual (clientes, pacotes e contratos) |

Resposta padrão:

```json
{
  "itens": [],
  "pagina": 0,
  "tamanho": 20,
  "totalItens": 0,
  "totalPaginas": 0,
  "temAnterior": false,
  "temProxima": false
}
```

- `pagina` ou `tamanho` inválidos retornam `400 PAGINACAO_INVALIDA`.
- Clientes: busca em nome, WhatsApp e e-mail.
- Pacotes: busca em nome e descrição, além do filtro `ativo`.
- Contratos: busca em número, nome do cliente e nome do pacote (colunas desnormalizadas na V2), além de `statusAceite`, `statusPagamento`, `clienteId` e `agendamentoId`.
- Agenda: `GET /api/v1/agenda` é paginado e aceita `inicio`/`fim`/`status`. O calendário usa `GET /api/v1/agenda/calendario?inicio=...&fim=...`, que exige o período completo.

## Endpoints principais

| Recurso | Rotas |
|---|---|
| Clientes | `GET/POST /api/v1/clientes`, `GET/PUT/DELETE /api/v1/clientes/{id}` |
| Pacotes | `GET/POST /api/v1/pacotes`, `GET/PUT/DELETE /api/v1/pacotes/{id}`, `PATCH /api/v1/pacotes/{id}/ativo` |
| Agenda | `GET/POST /api/v1/agenda`, `GET /api/v1/agenda/calendario`, `GET/PUT/DELETE /api/v1/agenda/{id}`, `PATCH /{id}/status`, `PUT /{id}/pagamento-entrada`, `PUT /{id}/pagamento-final` |
| Contratos | `GET/POST /api/v1/contratos`, `GET/PUT/DELETE /{id}`, `POST /{id}/duplicar`, `GET/POST /{id}/links-aceite`, `POST /{id}/links-aceite/{linkId}/cancelar`, `POST /{id}/reabrir`, `POST /{id}/cancelar` |
| Pagamentos do contrato | `POST /{id}/pagamento/confirmar`, `POST /{id}/pagamento/rejeitar`, `POST /{id}/pagamento/isentar`, `PUT /{id}/pagamento-final`, `POST /{id}/pagamento-final/isentar`, `POST /{id}/pagamento-final/reabrir` |
| Modelos e estúdio | `/api/v1/modelos-contrato`, `/api/v1/estudio` |
| Aceite público | `GET/POST /api/v1/public/contratos/{token}`, `POST .../visualizar`, `POST .../aceite`, `POST .../comprovante` |

## Próximas fases

1. **Upload real de comprovante** com storage local/objeto, servindo o arquivo ao staff.
2. **Galerias**: upload de mídia, seleção, extras e entrega.
3. **Financeiro consolidado**.
4. **Autenticação/JWT** e perfis de acesso.
