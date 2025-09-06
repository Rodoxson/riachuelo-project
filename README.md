# Storefront & Warehouse Microservices

**Storefront ↔ RabbitMQ ↔ Warehouse** (Spring Boot + Gradle + Docker + H2 + Swagger).

---

## Sumário

* [Visão Geral](#visão-geral)
* [Arquitetura](#arquitetura)
* [Pré-requisitos](#pré-requisitos)
* [Como rodar (Docker Compose)](#como-rodar-docker-compose)
* [Como rodar localmente (sem Docker)](#como-rodar-localmente-sem-docker)
* [Endpoints principais](#endpoints-principais)
* [Swagger / H2 / Actuator](#swagger--h2--actuator)
* [Fluxo de mensagens (RabbitMQ)](#fluxo-de-mensagens-rabbitmq)
* [Roteiro de teste ponta a ponta (adaptado)](#roteiro-de-teste-ponta-a-ponta-adaptado)
* [Configurações importantes / variáveis](#configurações-importantes--variáveis)
* [Dockerfile — explicação](#dockerfile--explicação-multi-stage)
* [Dicas para desenvolvimento (hot reload)](#dicas-para-desenvolvimento-hot-reload)
* [Testes automatizados](#testes-automatizados)
* [Contribuição / Contato](#contribuição--contato)
* [Licença](#licença)

---

## Visão Geral

Este projeto contém dois microserviços Java Spring Boot:

* **Storefront** — interface de ponta, fornece endpoints para consulta de produtos e realização de compras. Atua como *producer* de mensagens para o RabbitMQ.
* **Warehouse** — serviço de inventário, armazena produtos (H2) e consome mensagens do RabbitMQ para atualizar o estoque.

Ambos os serviços são empacotados com Gradle e podem ser executados via Docker (multi-stage Dockerfile) ou localmente.

---

## Arquitetura

```
[Client/Swagger] --> [Storefront (/storefront, 8081)] --(pub msg)--> [RabbitMQ (5672/15672)] --(sub msg)--> [Warehouse (/warehouse, 8080)] --> [H2]
                                         ^
                                         |
                                  (management UI 15672)
```

* Comunicação síncrona: Storefront consulta Warehouse (HTTP) para obter produto.
* Comunicação assíncrona: Storefront publica eventos de compra no RabbitMQ; Warehouse consome e atualiza estoque.

---

## Pré-requisitos

* Docker & Docker Compose
* JDK 21 instalado (apenas se for rodar local sem Docker)
* Gradle Wrapper (o projeto inclui `./gradlew`)
* Browser para Swagger / H2 Console / RabbitMQ UI

---

## Como rodar (Docker Compose)

Exemplo rápido:

```bash
# Limpa e sobe tudo
docker-compose down -v
docker-compose up --build
```

Principais portas (padrão deste projeto):

* Storefront: `http://localhost:8081/storefront`
* Warehouse: `http://localhost:8080/warehouse`
* RabbitMQ Management UI: `http://localhost:15672` (user `guest` / `guest`)

Verificação básica:

```bash
docker ps
# logs das aplicações
docker-compose logs -f storefrontapp
docker-compose logs -f warehouse
```

---

## Como rodar localmente (sem Docker)

No root do serviço (storefront ou warehouse):

```bash
# executa build (gera jar)
./gradlew clean build

# executar a aplicação
java -jar build/libs/<seu-jar>.jar
```

Ou usar o Gradle BootRun (dev):

```bash
./gradlew bootRun
```

> Garanta que `spring.datasource` e `server.port` nas propriedades estão configurados para não conflitar entre serviços.

---

## Endpoints principais

> Ajuste os caminhos conforme seu código se tiver nomes diferentes.

### Warehouse (porta 8080, context path `/warehouse`)

* `GET /warehouse/products/{uuid}` — retorna produto por id
* `POST /warehouse/products/{uuid}/purchase` — endpoint interno (opcional) para processar compra

### Storefront (porta 8081, context path `/storefront`)

* `GET /storefront/products/{uuid}` — busca produto (pode proxiar para Warehouse)
* `POST /storefront/products/{uuid}/purchase` — realiza compra; publica mensagem no RabbitMQ

---

## Swagger / H2 / Actuator

* Swagger UI (Storefront): `http://localhost:8081/storefront/swagger-ui/index.html`
* Swagger UI (Warehouse): `http://localhost:8080/warehouse/swagger-ui/index.html`
* OpenAPI JSON: `http://localhost:<porta>/<context-path>/v3/api-docs`
* H2 Console: `http://localhost:<porta>/<context-path>/h2-console`

    * JDBC URL (exemplo em memória): `jdbc:h2:mem:testdb`
    * Usuário: `sa` (senha em branco por padrão)
* Actuator health: `http://localhost:<porta>/<context-path>/actuator/health`

**Propriedades úteis (application.properties)**

```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
springdoc.swagger-ui.enabled=true
```

---

## Fluxo de mensagens (RabbitMQ)

* **Producer**: Storefront — publica mensagens de compra em uma *exchange* com uma routing key.
* **Exchange**: conforme `AMQPConfig` (topic/direct — conforme sua configuração).
* **Queue**: `productChangeAvailabilityQueue` — ligada via binding à exchange.
* **Consumer**: Warehouse — lê mensagens, converte para DTO e atualiza o H2 (estoque).

No RabbitMQ Management UI você pode checar exchanges, queues, bindings e mensagens publicadas/entregues.

---

## Roteiro de Teste Ponta a Ponta (adaptado)

> Versão rápida resumida — roteiro completo e detalhado também foi gerado como `roteiro_testes_microservicos.txt`.

1. Subir ambiente:

   ```bash
   docker-compose down -v
   docker-compose up --build
   ```
2. Inserir produto no Warehouse (H2) via H2 Console `http://localhost:8080/warehouse/h2-console`:

   ```sql
   INSERT INTO products (id, sku, name, price, stock)
   VALUES ('b6a0b1a8-9d2e-4f2f-8a0d-02e9e2a11111', 'SKU-TEST-001', 'Controle Gamer', 199.90, 5);
   ```
3. Validar GET no Warehouse:
   `GET /warehouse/products/b6a0b1a8-...` → esperar stock = 5
4. Validar GET no Storefront:
   `GET /storefront/products/b6a0b1a8-...` → deve retornar mesmo produto
5. Realizar compra pelo Storefront:
   `POST /storefront/products/{uuid}/purchase` com body `{ "quantity": 2 }`
6. Conferir RabbitMQ — Queue `productChangeAvailabilityQueue` recebeu a mensagem
7. Conferir H2 do Warehouse — stock = 3
8. Verificar novamente GET no Storefront — stock atualizado

Cenários de erro:

* Comprar quantidade maior que o estoque → 400/409
* Produto inexistente → 404

---

## Configurações importantes / variáveis

* `JAVA_OPTS` — pode ser passada no Dockerfile/compose para ajustar memória/flags JVM.
* `spring.datasource.url` — `jdbc:h2:mem:testdb` (dev) ou `jdbc:h2:file:/app/data/warehouse-db` (persistência em file mode)
* RabbitMQ:

    * Host: `rabbitmq` (no compose) / `localhost` (sem compose)
    * Port: `5672` (AMQP), Management UI `15672`
    * User/Password: `guest`/`guest` (dev)

---

## Dockerfile — explicação (multi-stage)

Exemplo usado no projeto (resumido):

```dockerfile
# build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew clean bootJar -x test

# runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS=""
COPY --from=build /app/build/libs/*.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
```

* O Gradle é executado **dentro do stage de build**. O runtime final só contém o JRE e o jar gerado.
* `-x test` evita executar testes durante o build em CI local — ajuste conforme preferir.

---

## Dicas para desenvolvimento (hot reload)

Para não rebuildar o jar a cada mudança, configure um `docker-compose.override.yml` ou um serviço dev:

```yaml
services:
  storefront:
    build: .
    volumes:
      - .:/app
      - ~/.gradle:/home/gradle/.gradle
    command: ./gradlew bootRun
    ports:
      - "8081:8081"
```

* Use `spring-boot-devtools` e `bootRun` para ver hot reload.
* Mapear o diretório do projeto como volume evita rebuilds caros.

---

## Testes automatizados

* Execute testes unitários e de integração localmente:

```bash
./gradlew test
```

* Para testes de integração que dependem de RabbitMQ ou H2, recomenda-se usar **Testcontainers**.
* Cobertura de testes: configure Jacoco ou outro plugin de cobertura no Gradle para monitorar resultados.

---

## Contribuição / Contato

* Abra *issues* para reportar bugs ou sugerir melhorias.
* Para alterações na arquitetura ou funcionalidades, proponha PRs detalhando as mudanças e roteiro de testes.
* Contato: [rodolfo@example.com](mailto:rodolfo@example.com) (ou substituir pelo email real do responsável).

---

## Licença

* Este projeto está sob licença **MIT**.
* Consulte o arquivo `LICENSE` para detalhes completos.
