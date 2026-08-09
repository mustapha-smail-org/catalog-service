# CityPulse - Catalog Service
## Project Structure : 
```
catalog-service/
├── pom.xml
├── Dockerfile
├── compose.yaml
├── .gitignore
├── README.md
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/citypulse/catalog/
    │   │       ├── CatalogServiceApplication.java
    │   │       │
    │   │       ├── controller/
    │   │       │   ├── EventController.java
    │   │       │   └── CategoryController.java
    │   │       │
    │   │       ├── service/
    │   │       │   ├── EventIngestionService.java
    │   │       │   └── EventQueryService.java
    │   │       │
    │   │       ├── consumer/
    │   │       │   └── EventKafkaConsumer.java
    │   │       │
    │   │       ├── config/
    │   │       │   ├── KafkaConsumerConfig.java
    │   │       │   ├── KafkaProperties.java
    │   │       │   └── PersistenceConfig.java
    │   │       │
    │   │       ├── dto/
    │   │       │   ├── request/
    │   │       │   │   └── EventSearchCriteria.java
    │   │       │   │
    │   │       │   └── response/
    │   │       │       ├── EventSummaryResponse.java
    │   │       │       ├── EventDetailResponse.java
    │   │       │       ├── EventLocationResponse.java
    │   │       │       ├── EventOccurrenceResponse.java
    │   │       │       ├── EventAccessibilityResponse.java
    │   │       │       ├── EventPricingResponse.java
    │   │       │       └── PageResponse.java
    │   │       │
    │   │       ├── entity/
    │   │       │   ├── EventEntity.java
    │   │       │   ├── EventOccurrenceEntity.java
    │   │       │   ├── EventLocationEmbeddable.java
    │   │       │   ├── EventAccessibilityEmbeddable.java
    │   │       │   └── EventPricingEmbeddable.java
    │   │       │
    │   │       ├── repository/
    │   │       │   └── EventRepository.java
    │   │       │
    │   │       ├── specification/
    │   │       │   └── EventSpecification.java
    │   │       │
    │   │       ├── mapper/
    │   │       │   ├── EventAvroMapper.java
    │   │       │   └── EventResponseMapper.java
    │   │       │
    │   │       ├── exception/
    │   │       │   ├── EventNotFoundException.java
    │   │       │   ├── KafkaEventProcessingException.java
    │   │       │   ├── ApiErrorResponse.java
    │   │       │   └── GlobalExceptionHandler.java
    │   │       │
    │   │       └── utils/
    │   │           └── EventIdUtils.java
    │   │
    │   ├── avro/
    │   │   └── event.avsc
    │   │
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── application-test.yml
    │       ├── keystore/
    │       │   └── ca.pem
    │       └── db/
    │           └── migration/
    │               ├── V1__create_events.sql
    │               └── V2__create_event_indexes.sql
    │
    └── test/
        └── java/
            └── com/citypulse/catalog/
                ├── controller/
                │   ├── EventControllerTest.java
                │   └── CategoryControllerTest.java
                ├── service/
                │   ├── EventIngestionServiceTest.java
                │   └── EventQueryServiceTest.java
                ├── consumer/
                │   └── EventKafkaConsumerTest.java
                ├── mapper/
                │   ├── EventAvroMapperTest.java
                │   └── EventResponseMapperTest.java
                ├── repository/
                │   └── EventRepositoryTest.java
                └── integration/
                    ├── AbstractIntegrationTest.java
                    ├── EventKafkaIntegrationTest.java
                    └── EventApiIntegrationTest.java
```

## Project Roadmap :
Here is a practical breakdown of the **Catalog Service** into seven major parts. Each part is small enough to implement and test as a separate milestone.

## Part 1 — Project foundation

Goal: create a runnable Spring Boot service connected to PostgreSQL.

* Initialize the Maven project and dependencies.
* Create the MVC package structure.
* Configure `application.yml` and environment variables.
* Configure PostgreSQL locally with Docker Compose.
* Add health checks and verify application startup.

Main dependencies:

* Spring Web
* Spring Data JPA
* PostgreSQL Driver
* Flyway
* Spring Kafka
* Avro and Confluent serializer
* Validation
* Actuator
* Testcontainers

---

## Part 2 — Database and persistence model

Goal: define how events are stored.

* Design the `events` and `event_occurrences` tables.
* Create the initial Flyway migrations.
* Implement the JPA entities and embeddable objects.
* Implement `EventRepository`.
* Add indexes for dates, categories, city and text search.
* Write repository tests with PostgreSQL Testcontainers.

Main classes:

```text
entity/
├── EventEntity.java
├── EventOccurrenceEntity.java
├── EventLocationEmbeddable.java
├── EventAccessibilityEmbeddable.java
└── EventPricingEmbeddable.java

repository/
└── EventRepository.java
```

---

## Part 3 — Kafka consumer

Goal: receive normalized events from the ingestion service.

* Add the same `event.avsc` used by the producer.
* Configure Aiven Kafka and Schema Registry.
* Implement `EventAvroMapper`.
* Implement `EventKafkaConsumer`.
* Configure manual acknowledgement and consumer retry behaviour.
* Add unit tests for mapping and message consumption.

Execution flow:

```text
Kafka EventAvro
→ EventKafkaConsumer
→ EventAvroMapper
→ EventEntity
```

---

## Part 4 — Event ingestion and upsert

Goal: persist every Kafka event safely.

* Implement `EventIngestionService`.
* Insert a new event when its ID does not exist.
* Update the existing event when newer data arrives.
* Replace or synchronize occurrences and categories.
* Make processing transactional and idempotent.
* Test creation, update, duplicate delivery and rollback scenarios.

Execution flow:

```text
EventKafkaConsumer
→ EventIngestionService
→ EventRepository
→ PostgreSQL
→ Kafka acknowledgement
```

The Kafka message should only be acknowledged after the database transaction succeeds.

---

## Part 5 — Event query API

Goal: expose searchable events to the future API Gateway.

* Implement event summary and detail response DTOs.
* Implement `EventResponseMapper`.
* Implement `EventQueryService`.
* Implement pagination, filtering and sorting.
* Create event list and detail endpoints.
* Add controller and service unit tests.

Initial endpoints:

```http
GET /events
GET /events/{id}
GET /categories
```

Example search request:

```http
GET /events?query=concert
           &category=Music
           &city=Paris
           &startDate=2026-08-10
           &endDate=2026-08-20
           &free=true
           &accessible=true
           &page=0
           &size=20
           &sort=startDate,asc
```

---

## Part 6 — Error handling and resilience

Goal: make failures predictable and observable.

* Create `EventNotFoundException`.
* Implement `GlobalExceptionHandler`.
* Define a consistent `ApiErrorResponse`.
* Configure Kafka retries and dead-letter handling.
* Add structured logs and correlation IDs.
* Configure readiness and liveness health endpoints.

Example API error:

```json
{
  "status": 404,
  "code": "EVENT_NOT_FOUND",
  "message": "Event event-123 was not found",
  "timestamp": "2026-08-07T10:30:00Z",
  "path": "/events/event-123"
}
```

---

## Part 7 — Integration testing and packaging

Goal: validate the complete service before connecting the frontend.

* Test Kafka-to-PostgreSQL processing.
* Test the REST API against a real test database.
* Verify filtering, pagination and sorting.
* Add a production Dockerfile.
* Document environment variables and local startup.
* Run the complete Maven build and test suite.

Final verified flows:

```text
Kafka → Consumer → Upsert → PostgreSQL
```

```text
HTTP → Controller → Query Service → PostgreSQL → JSON response
```

## Recommended implementation order

| Milestone      | Result                                       |
| -------------- | -------------------------------------------- |
| 1. Foundation  | Service and PostgreSQL run locally           |
| 2. Persistence | Event schema and repository work             |
| 3. Kafka       | `EventAvro` messages are consumed            |
| 4. Ingestion   | Events are inserted and updated safely       |
| 5. REST API    | Events can be searched and retrieved         |
| 6. Resilience  | Errors, retries and monitoring are handled   |
| 7. Integration | Complete service is tested and containerized |

The best next step is **Part 1: project foundation**, beginning with the Catalog Service `pom.xml`.
