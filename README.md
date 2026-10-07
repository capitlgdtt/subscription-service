# Subscription Service

Сервис учёта и управления подписками клиентов на тарифные планы на фреймворке
[onno](https://github.com/onno-erp/onno-framework).

Бизнес-модель описана типизированной Java-метамоделью (`@Catalog`, `@Document`,
`@AccumulationRegister`, `@Enumeration`). Схема БД, REST API, UI и история миграций
генерируются фреймворком. Ручных таблиц, DTO и CRUD-контроллеров в проекте нет.

## Стек

Java 21 · Spring Boot 3.4.4 · onno-framework 2.0.0 · PostgreSQL 16 · Gradle

## Требования

- JDK 21
- Docker

## Запуск

```bash
./gradlew bootRun
```

Spring Boot поднимает PostgreSQL из `docker-compose.yaml` (порт 5433) и
останавливает его при выходе. UI: **http://localhost:8080**, учётные записи
`admin/admin` и `manager/manager`.

## Тесты

```bash
./gradlew test
```

Интеграционные тесты на Testcontainers (`postgres:16-alpine`).

Покрытие:

| Тест | Что проверяет |
|---|---|
| `SubscriptionBeforeWriteTest` | `total` = сумма строк; `endDate` = дата начала + **максимум** длительности строк (не сумма) |
| `SubscriptionPostingTest` | проведение списывает баланс и признаёт выручку в разрезе тарифа и клиента |
| `SubscriptionInsufficientFundsTest` | при нехватке денег проведение отклоняется, движений не создаётся |
| `SubscriptionCancelledTest` | отменённая подписка не создаёт движений |
| `SubscriptionRulesTest` | бизнес-правила документа |

## Реализовано

### Модель

- Справочники: `Customer`, `Tariff`.
- Перечисления: `CustomerStatus`, `SubscriptionStatus`, `PaymentMethod`.
- Документы: `Payment`; `Subscription` с табличной частью `SubscriptionLine`.
- Регистры накопления: `AccountBalance` (BALANCE), `Revenue` (TURNOVER).
- `EntityView` для каждой сущности; `Layout` с разделами Sales и Reports.

### Бизнес-логика

- **Проведение**: `Payment` пополняет лицевой счёт; `Subscription` списывает
  сумму с лицевого счёта и признаёт выручку по каждой строке; `CANCELLED`
  подписка не создаёт движений.
- **Нехватка денег**: отказ обеспечивается объявлением `AccountBalance`
  регистром `BALANCE` с `allowNegative = false` — движок проведения отклоняет
  проводку, уводящую остаток в минус. Ручных проверок в коде нет.
- **Правила** (`Validated` + `BusinessRule`): клиент обязателен, хотя бы одна
  строка (кроме отменённых), периоды > 0, тариф доступен.
- **Автоподстановка**: дата документа и дата начала при создании; цена строки
  из тарифа; итог и дата окончания пересчитываются при каждом сохранении.
- **Регламентное задание** (`@ScheduledJob`): `SubscriptionLifecycleJob`
  переводит `DRAFT → ACTIVE` и `ACTIVE → EXPIRED` по датам.
- **Действие отмены** (`ActionSpec`): модальное окно с причиной, причина
  сохраняется в `Cancel reason`.

## Структура

```text
src/main/java/com/example/subscription/
├── SubscriptionApplication.java
├── config/            ApplicationContextHolder, JobRunrConfig
├── domain/
│   ├── catalogs/      Customer, Tariff
│   ├── documents/     Payment, Subscription, SubscriptionLine
│   ├── enumerations/  CustomerStatus, SubscriptionStatus, PaymentMethod
│   └── registers/     AccountBalance, Revenue
├── jobs/              SubscriptionLifecycleJob
├── repositories/
└── ui/
├── layouts/       MainLayout
└── views/         EntityView для каждой сущности
```