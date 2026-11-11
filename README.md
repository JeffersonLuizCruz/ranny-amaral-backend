# Fotografia CRM — Backend

API REST do CRM de estúdio fotográfico, construída com **Java 25**, **Spring Boot 4.1.2**, **Spring Modulith 2.1.1**, **Spring Data JPA**, **PostgreSQL 16** e **Flyway**.

O núcleo cobre **clientes, pacotes, agenda, contratos, galerias e financeiro** (incluindo modelos, configuração do estúdio, aceite público, paginação/busca e upload de mídia), com **autenticação JWT e gestão de usuários**. O storage é o **Garage** (S3 compatível).

## Requisitos

- Java 25 (`java -version`)
- Maven 3.9+ (o wrapper já está configurado: `./mvnw`)
- Docker + Docker Compose para PostgreSQL e Garage local

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
- Garage (S3): `http://localhost:3900` (admin `http://localhost:3903`, não expor)
  - bucket: `crm-originais`
  - credenciais padrão de dev: `GK00000000000000000000000000000000` / `fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210`
- Admin local: `admin@rannyamaral.com.br` / `admin12345` (troca obrigatória no primeiro acesso)

O Flyway aplica as migrações no primeiro boot e o Hibernate roda com `ddl-auto=validate`. O perfil `dev` popula dados de demonstração (`app.seed.enabled=true`) com os mesmos IDs do frontend (`cli-marina`, `pkg-gestante`, `apt-001`, `ct-001`, etc.). O seed só roda quando as tabelas estão vazias.

Para apontar para outro banco, use `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`.

> Se já existir outro PostgreSQL ocupando a porta 5432, pare-o antes de subir o compose deste projeto (ou ajuste a porta e a URL do datasource).

### Limpar os dados locais

Para apagar todos os registros de teste preservando schema e histórico do Flyway:

```bash
bash scripts/reset-dev-db.sh
```

O script trunca todas as tabelas de `public`, exceto `flyway_schema_history`. No próximo boot, o perfil `dev` pode repopular os dados de demonstração. Para subir com o banco vazio:

```bash
APP_SEED_ENABLED=false ./mvnw spring-boot:run
```

## Testes

```bash
./mvnw test                       # H2 + Spock (rápido)
./mvnw -Dtest='*Spec' test        # somente Spock
./mvnw verify                     # H2 + PostgresSchemaIT (requer Postgres)
S3_TEST_ENDPOINT=http://localhost:3900 ./mvnw verify  # inclui o IT de storage (Garage)
```

Os testes rápidos usam **H2 em memória**, `create-drop`, Flyway desabilitado e `app.seed.enabled=false` (`src/test/resources/application.properties`). O `PostgresSchemaIT` roda no `verify` com o perfil `postgres`, aplica as migrações Flyway e valida o schema real com `ddl-auto=validate` (use `POSTGRES_TEST_URL`/`POSTGRES_TEST_USERNAME`/`POSTGRES_TEST_PASSWORD` para apontar o banco).

A suíte cobre:

- CRUD e validações de clientes, pacotes e agenda;
- paginação, ordenação estável, busca textual e validação de parâmetros;
- upload de comprovante com validação de tipo/tamanho, multipart e Garage real (quando `S3_TEST_ENDPOINT` está definido);
- upload de fotos/vídeos, versão final editada, derivados, seleção, extras, liberação de RAW e entrega (ZIP streaming);
- processamento assíncrono de derivados com polling no frontend;
- financeiro mensal com precedência de contrato, extras e exclusões;
- dashboard operacional com agenda, contratos, galerias e financeiro;
- migração incremental `V1 → V8` com backfill de dados existentes (`PostgresMigrationIT`);
- congelamento das condições comerciais do pacote;
- numeração de contratos (`CT-AAAA-0001`) e protocolos de aceite (`ACE-AAAA-0001`);
- ciclo de vida do link público (geração, visualização, aceite, expiração e cancelamento);
- pagamentos de entrada/saldo;
- sincronização contrato → agenda por eventos duráveis (`@ApplicationModuleListener`);
- concorrência otimista (`@Version`) e conflito de atualização;
- filtro de borda (payload, headers, content-type, scanning e IP);
- contrato de erro RFC 7807/9457 (`ProblemDetail`);
- verificação das fronteiras dos módulos (`ApplicationModules.verify()`).

## Arquitetura

```
com.ranny.fotografia
├── shared/      Shared Kernel: auditoria, pagamentos, JSON, erros, eventos e portas
├── auth/        Usuários, login, refresh, senhas, auditoria e segurança
├── cliente/     Cadastro de clientes (API pública ClienteConsulta)
├── pacote/      Catálogo de pacotes (API pública PacoteConsulta)
├── agenda/      Agendamentos, pagamentos e congelamento de condições
├── contrato/    Contratos, modelos, estúdio e aceite público
├── arquivo/     Catálogo de arquivos e uploads pré-assinados
├── galeria/     Mídias, seleção, edição, RAW e entrega
├── financeiro/  Read model financeiro mensal
└── dashboard/   Read model operacional agregado
```

- Cada módulo expõe apenas o necessário no pacote raiz; o restante é interno.
- A comunicação síncrona usa **portas públicas** (`ClienteConsulta`, `PacoteConsulta`, `AgendamentoConsulta`).
- A comunicação assíncrona usa **eventos duráveis** (`ContratoStatusAlteradoEvent`, `ContratoExcluidoEvent`) consumidos por `@ApplicationModuleListener` — o contrato não conhece a agenda. O registry do Spring Modulith persiste as publicações na tabela `event_publication` e garante reentrega.
- Entidades usam **composição** (`Auditoria`, `PagamentoEntrada`, `PagamentoSaldo`, `CondicoesPacote`), sem herança de entidades e sem `@Data`.
- Documentos grandes e imutáveis (`snapshot`, `cláusulas`, `histórico`, `aceite`) são persistidos como JSON em colunas `TEXT` via `AttributeConverter`.
- Valores consultáveis (status, número, valores) ficam em colunas relacionais.
- Erros seguem um único contrato RFC 7807/9457 (`ProblemDetail`) com 400/404/409/410/413/415/422/500. Conflitos de concorrência retornam `409 CONFLITO_CONCORRENCIA`. Campos de compatibilidade (`codigo`, `mensagem`, `caminho`, `campos`) são mantidos como extensões.

## Migrações

- `src/main/resources/db/migration/V1__baseline.sql` cria todo o schema do núcleo (tabelas, constraints e índices) e as tabelas do outbox (`event_publication`, `event_publication_archive`).
- `src/main/resources/db/migration/V2__contratos_busca_paginacao.sql` adiciona `cliente_nome`/`pacote_nome` em `contratos` (com backfill a partir do snapshot), índices de busca/paginação e o índice de status/período da agenda.
- `src/main/resources/db/migration/V3__arquivos_upload.sql` cria o catálogo genérico `arquivos` e adiciona `entrada_arquivo_id` em contratos/agendamentos.
- `src/main/resources/db/migration/V4__galerias.sql` cria galerias, mídias, derivados, links, seleção, extras, downloads e histórico; também inclui `VIDEO_ORIGINAL` no catálogo de arquivos.
- `src/main/resources/db/migration/V5__versao_editada.sql` adiciona `FOTO_EDITADA` ao catálogo e a origem (`ORIGINAL`/`EDITADA`) dos derivados.
- `src/main/resources/db/migration/V6__liberacao_raw.sql` adiciona a liberação auditada de RAW na galeria e a versão baixada no histórico.
- `src/main/resources/db/migration/V7__auth.sql` cria `usuarios`, `refresh_tokens` e `auth_eventos`.
- `src/main/resources/db/migration/V8__invariantes_pagamento.sql` garante que `saldo_status = 'CONFIRMADO'` só exista quando `saldo_valor >= saldo_restante` em contratos e agendamentos.
- `src/main/resources/db/migration/V9__extras_pix_comprovante.sql` adiciona a chave Pix e o comprovante do pedido de extras.
- `src/main/resources/db/migration/V10__numeracao_midias.sql` renumera as mídias existentes e cria a constraint única de `(galeria_id, tipo, ordem)`.
- `src/main/resources/db/migration/V11__download_selecao_staff.sql` cria os tokens de curta duração para o staff baixar as fotos selecionadas.
- `src/main/resources/db/migration/V12__faixas_foto_extra.sql` adiciona as faixas progressivas de preço da foto extra no catálogo e os snapshots JSON em agendamentos, galerias e pedidos de extras.
- `src/main/resources/db/migration/V13__zip_baixado_galeria.sql` adiciona `zip_baixado_em` na galeria, com backfill dos ZIPs antigos a partir do histórico.
- `src/main/resources/db/migration/V14__remove_pronta_entrega.sql` remove o estado `PRONTA_ENTREGA`, converte as galerias existentes e ajusta o check constraint.
- `src/main/resources/db/migration/V15__extras_adicionais.sql` adiciona `selecao_entregue_id` na galeria (trava de mídias já entregues) e o índice de busca do pedido de extras pendente.
- O Flyway é a fonte de verdade do schema; o Hibernate apenas valida (`ddl-auto=validate`).
- **Nunca edite uma migração já aplicada.** Mudanças entram em `V15__...`, `V16__...`, etc.
- `spring.flyway.baseline-on-migrate=true` protege bancos não vazios sem histórico; em banco novo, a V1 é aplicada normalmente.
- O evento de contrato é compacto (chaves JSON curtas) para caber no `serialized_event varchar(255)`; `EventSerializationSpec` garante o limite.

## Hardening

- **Concorrência otimista:** todas as entidades mutáveis possuem `@Version` (`lock_version`). Atualizações concorrentes do mesmo registro resultam em `409`.
- **Eventos duráveis:** `spring-modulith-starter-jpa` persiste eventos em `event_publication`; a tabela é criada na V1 com `serialized_event varchar(255)` e o payload do evento é compacto.
- **Entrega eventual:** com o registry, o listener de contrato → agenda roda após o commit; a UI é atualizada localmente de imediato e o backend converge na sequência.
- **CI:** `.github/workflows/ci.yml` executa a suíte H2, um job PostgreSQL + Garage e publica a imagem `amd64` no GHCR.

## Segurança de borda

O `FiltroBordaSeguranca` roda no início da cadeia do Spring Security, antes de Controllers e Services:

- limita o corpo por `Content-Length` e por contagem real de bytes (inclusive `Transfer-Encoding: chunked`) → `413 PAYLOAD_EXCEDIDO`;
- exige `User-Agent` em requisições de API → `400 HEADER_OBRIGATORIO`;
- exige `application/json` ou `+json` em `POST/PUT/PATCH` com corpo → `415 CONTENT_TYPE_*`;
- bloqueia User-Agents de scanners e padrões óbvios de path traversal/arquivos sensíveis → `403 PADRAO_MALICIOSO`;
- deixa um ponto de extensão (`AvaliadorAcessoIp`) para bloqueio/rate limiting por IP em Redis/cache, sem acoplar o filtro ao provedor;
- ignora `OPTIONS`, dispatch de erro, Swagger/OpenAPI e `/actuator/health/**`.

Configuração principal (`application.properties`):

```properties
app.seguranca.borda.max-payload-bytes=2097152
app.seguranca.borda.exigir-user-agent=true
app.seguranca.borda.exigir-content-type-em-mutacoes=true
app.seguranca.borda.bloquear-padroes-scanning=true
app.seguranca.borda.ip-check-habilitado=false
app.seguranca.borda.confiar-em-proxy=false
```

O filtro não lê o corpo: JSON malformado continua sendo tratado pelo `GlobalExceptionHandler` com `400 CORPO_INVALIDO`.

A autenticação usa **Spring Security + JWT RS256** com refresh em cookie `HttpOnly`, BCrypt, bloqueio por
tentativas e auditoria. Rotas públicas (`/api/v1/public/**`) continuam abertas; o restante exige token.
Perfis: `ADMIN` (tudo + `/api/v1/usuarios/**`) e `GESTOR` (tudo, menos usuários). Detalhes em `docs/AUTH.md`.

> A Parte B (Circuit Breaker/Bulkhead) não foi implementada nesta entrega: não há gateway de pagamento externo e o Pix é confirmado manualmente pelo staff na galeria. O tema fica registrado em `docs/seguranca-borda.md`.

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
- Contratos: busca em número, nome do cliente e nome do pacote (colunas desnormalizadas na V2), além de `statusAceite`, `statusEntrada`, `statusSaldoFinal`, `clienteId` e `agendamentoId`. A resposta também expõe `entradaQuitada`, `saldoQuitado`, `quitado`, `valorRecebido` e `valorPendente`.
- Agendamentos: a resposta também expõe `statusEntrada`, `statusSaldoFinal`, `entradaQuitada`, `saldoQuitado`, `quitado`, `valorRecebido` e `valorPendente`, com os mesmos campos derivados do contrato.
- Agenda: `GET /api/v1/agenda` é paginado e aceita `inicio`/`fim`/`status`. O calendário usa `GET /api/v1/agenda/calendario?inicio=...&fim=...`, que exige o período completo.
- Galerias: `GET /api/v1/galerias` aceita `status`, `q` e `baixada`. `baixada=true` filtra apenas galerias cujo ZIP da entrega já foi baixado pelo cliente (download individual não conta); `baixada=false` faz o inverso. A resposta expõe `zipBaixadoEm`. Seleções sem extras entram em `EM_EDICAO` automaticamente; a publicação da entrega acontece a partir de `EM_EDICAO`.

## Upload de comprovantes

O arquivo **não passa pelo Spring**. O navegador envia direto ao Garage/S3 por URLs pré-assinadas:

1. `POST /api/v1/public/contratos/{token}/comprovante/upload`
2. envio das partes ao storage;
3. `POST /api/v1/public/contratos/{token}/comprovante/upload/{arquivoId}/complete`;
4. `GET /api/v1/contratos/{id}/pagamento/comprovante/url` gera uma URL temporária para o staff.

Características:

- até **10 MB** por comprovante;
- PDF, JPEG, PNG e WebP;
- validação por _magic bytes_ no backend;
- upload único até 5 MB e multipart acima disso;
- progresso real, cancelamento e retry no frontend;
- deduplicação por SHA-256;
- URLs curtas e download direto do storage;
- rollback remove o objeto recém-criado.

Configuração principal:

```properties
app.storage.provider=s3
app.storage.endpoint=http://localhost:3900
app.storage.public-endpoint=http://localhost:3900
app.storage.bucket=crm-originais
app.storage.presign-seconds=600
app.storage.presign-download-seconds=3600
app.storage.max-comprovante-bytes=10485760
```

Em produção (Coolify), use `APP_STORAGE_ENDPOINT=https://arquivos.seudominio.com` e
`APP_STORAGE_PUBLIC_ENDPOINT=https://arquivos.seudominio.com`. O endpoint público precisa estar no CORS do
bucket. O download do staff agora é protegido pelo JWT.

## Galerias

- Fotos até **50 MB** e vídeos até **500 MB**.
- Upload direto ao Garage/S3 com progresso e deduplicação (retomada após reload ainda pendente — DT-006).
- Fotos geram derivados `thumb-400` e `web-1600` via processamento de imagem.
- Vídeos são armazenados e entregues como original nesta fase.
- Cada foto e vídeo recebe um **número estável** (`ordem`) por tipo dentro da galeria. O grid público e o
  detalhe do staff exibem o mesmo número, da esquerda para a direita; remoções não reutilizam números e
  podem deixar lacunas.
- Seleção pública por token de 30 dias; entrega por token de 90 dias.
- O payload público só expõe a URL do original quando o RAW foi liberado; downloads individuais e ZIP
  passam pelo endpoint que valida o gate e registra o evento.
- `GET /api/v1/public/galerias/{token}/download-zip/validar` valida o lote antes do streaming.
- O staff baixa as fotos selecionadas para edição por
  `POST /api/v1/galerias/{id}/selecao/download`, que devolve uma URL temporária (10 min) para
  `GET /api/v1/public/galerias/downloads/{token}/selecao.zip`. O ZIP usa os originais, nomeia cada
  arquivo com o número da foto (`001_nome.jpg`) e registra o evento no histórico.
- Extras usam uma **chave Pix própria do estúdio**, separada da chave do ensaio, e o pedido guarda um
  snapshot dessa chave. O cliente envia o comprovante Pix pelo link de seleção; o staff confirma o
  recebimento antes de iniciar a edição.
- **Extras adicionais:** o staff pode reabrir a seleção mesmo com pedidos já pagos ou isentos. Os pedidos
  anteriores são preservados e o reenvio gera um novo pedido apenas com o **delta** de fotos, calculado
  pelas faixas progressivas congeladas na galeria. A reabertura é sempre uma ação do staff; o cliente
  combina a compra diretamente com o estúdio.
- **Mídias entregues:** ao publicar a entrega, a seleção é registrada em `selecao_entregue_id`. Em
  reaberturas futuras, todas as fotos e vídeos dessa seleção continuam obrigatórios; o cliente só pode
  acrescentar novas mídias. Isso impede trocar fotos já baixadas por outras sem pagar o adicional.
- Regras:
  - ensaio realizado e pagamentos quitados para publicar a seleção;
  - fotos selecionadas ≥ fotos inclusas;
  - vídeos exatamente iguais aos inclusos;
  - fotos extras geram pedido de extras;
  - reenvio com menos extras do que o já pago é bloqueado;
  - confirmação Pix dos extras exige comprovante;
  - qualquer pedido de extras pendente bloqueia o início da edição e a publicação da entrega;
  - entrega exige extras pagos ou isentos.
- Rotas administrativas e públicas estão em `/api/v1/galerias` e `/api/v1/public/galerias`.

## Financeiro

- `GET /api/v1/financeiro/mensal?mes=YYYY-MM` retorna o resumo consolidado e as entradas do mês.
- Competência pelo início do ensaio no fuso `America/Sao_Paulo`.
- Contrato vinculado tem prioridade sobre os valores do agendamento.
- Extras vêm dos pedidos de extras da galeria, que podem ser cumulativos (pedido inicial + adicionais).
- Agendamentos cancelados e extras cancelados/isentos não inflam o previsto.
- O backend calcula totais, recebidos, a receber, ticket médio e percentual recebido.
- A tela não recalcula valores localmente.

## Endpoints principais

| Recurso | Rotas |
|---|---|
| Auth | `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me`, `POST /api/v1/auth/change-password` |
| Usuários (ADMIN) | `GET/POST /api/v1/usuarios`, `POST /api/v1/usuarios/{id}/reset-password`, `PUT /api/v1/usuarios/{id}`, `PATCH /api/v1/usuarios/{id}/ativo` |
| Clientes | `GET/POST /api/v1/clientes`, `GET/PUT/DELETE /api/v1/clientes/{id}` |
| Pacotes | `GET/POST /api/v1/pacotes`, `GET/PUT/DELETE /api/v1/pacotes/{id}`, `PATCH /api/v1/pacotes/{id}/ativo` |
| Agenda | `GET/POST /api/v1/agenda`, `GET /api/v1/agenda/calendario`, `GET/PUT/DELETE /api/v1/agenda/{id}`, `PATCH /{id}/status`, `PUT /{id}/pagamento-entrada`, `PUT /{id}/pagamento-final` |
| Contratos | `GET/POST /api/v1/contratos`, `GET/PUT/DELETE /{id}`, `GET /{id}/pagamento/comprovante/url`, `POST /{id}/duplicar`, `GET/POST /{id}/links-aceite`, `POST /{id}/links-aceite/{linkId}/cancelar`, `POST /{id}/reabrir`, `POST /{id}/cancelar` |
| Pagamentos do contrato | `POST /{id}/pagamento/confirmar`, `POST /{id}/pagamento/rejeitar`, `POST /{id}/pagamento/isentar`, `PUT /{id}/pagamento-final`, `POST /{id}/pagamento-final/isentar`, `POST /{id}/pagamento-final/reabrir` |
| Upload de comprovante | `POST /api/v1/public/contratos/{token}/comprovante/upload`, `POST .../upload/{arquivoId}/complete`, `POST .../upload/{arquivoId}/abort`, `GET .../upload/{arquivoId}/status` |
| Galerias | `GET/POST /api/v1/galerias`, `GET /{id}`, `GET /{id}/midias`, `POST /{id}/midias/upload`, `POST .../complete`, `DELETE /{id}/midias/{midiaId}` |
| Seleção e extras | `POST /{id}/selecao/publicar`, `POST /{id}/selecao/reabrir`, `POST /{id}/selecao/download`, `POST /{id}/extras/confirmar`, `POST /{id}/extras/isentar`, `POST /{id}/edicao/iniciar`, `POST /{id}/edicao/liberar` |
| Entrega | `POST /{id}/entrega/publicar`, `POST /{id}/entrega/revogar` |
| Galeria pública | `GET /api/v1/public/galerias/{token}`, `POST .../selecao`, `POST .../download`, `GET /api/v1/public/galerias/downloads/{token}/selecao.zip` |
| Financeiro | `GET /api/v1/financeiro/mensal?mes=YYYY-MM` |
| Modelos e estúdio | `/api/v1/modelos-contrato`, `/api/v1/estudio` |
| Aceite público | `GET/POST /api/v1/public/contratos/{token}`, `POST .../visualizar`, `POST .../aceite`, `POST .../comprovante` |

## Próximas fases

1. **MFA opcional e reset de senha por e-mail** (hoje a temporária é exibida uma vez pelo admin).
2. **Backup** de PostgreSQL e Garage (decisão atual: sem backup).
3. **Infraestrutura**: checksum server-side, job de limpeza de uploads órfãos, rate limit, observabilidade.
4. **Vídeos**: poster e transcode H.264/WebM (hoje são entregues como original).
5. **Relatórios financeiros** adicionais (exportação, fluxo de caixa e conciliação).
6. **Notificações** automáticas por WhatsApp/e-mail (hoje o link é copiado manualmente).
