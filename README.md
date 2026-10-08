# Subscription Service

Сервис учёта и управления подписками клиентов на тарифные планы на фреймворке
[onno](https://github.com/onno-erp/onno-framework).

Бизнес-модель описана типизированной Java-метамоделью (`@Catalog`, `@Document`,
`@AccumulationRegister`, `@Enumeration`). Схема БД, REST API, UI и история миграций
генерируются фреймворком. Ручных таблиц, DTO и CRUD-контроллеров в проекте нет.

## Live demo

Развёрнуто на Render: **https://subscription-service-3uxk.onrender.com**

- Логин: `admin` / `admin`
- После 15 минут простоя сервис "засыпает" - первый запрос может занять до минуты.

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

Интеграционные тесты на Testcontainers (`postgres:16-alpine`): для каждого
запуска поднимается одноразовый контейнер PostgreSQL, приложение стартует
против него, схема onno генерируется автоматически. Docker должен быть запущен.

Все тесты помечены тегом `level-2` и имеют `@DisplayName` со ссылкой на пункт
задания. Запуск только этой группы:

```bash
./gradlew test -Dgroups=level-2
```

### Трассировка

| Тест | Пункт задания |
|---|---|
| `SubscriptionBeforeWriteTest#totalAndEndDate_areComputedFromLines` | Автоподстановка: цена строки из тарифа; пересчёт сумм и даты окончания |
| `SubscriptionBeforeWriteTest#multiplePeriods_multiplyAmount_andExtendDuration` | Дата окончания — максимум по строкам, а не сумма |
| `SubscriptionPostingTest#posting_drawsBalance_andRecognisesRevenue` | Стоимость подписки списывается с лицевого счёта в момент проведения; выручка в разрезе тарифов и клиентов |
| `SubscriptionInsufficientFundsTest#posting_withInsufficientBalance_isRejected_andWritesNothing` | Оформить подписку при нехватке денег на счёте нельзя |
| `SubscriptionCancelledTest#posting_cancelledSubscription_doesNotTouchBalancesOrRevenue` | Отменённая подписка не создаёт никаких движений |
| `SubscriptionRulesTest` (4 `@Nested`-группы, 9 тестов) | Бизнес-правила: клиент обязателен, хотя бы одна строка, число периодов > 0, тариф доступен для подключения |
| `SubscriptionLifecycleJobTest` (3 теста) | Регламентное задание: `DRAFT → ACTIVE`, `ACTIVE → EXPIRED`, непроведённая подписка не активируется |
| `SubscriptionCancelActionTest` (2 теста) | Действие отмены (`ActionSpec`): статус меняется на `Cancelled`, причина сохраняется |

## Реализовано

### Модель

- Справочники: `Customer`, `Tariff`.
- Перечисления: `CustomerStatus`, `SubscriptionStatus`, `PaymentMethod`.
- Документы: `Payment`; `Subscription` с табличной частью `SubscriptionLine`.
- Регистры накопления: `AccountBalance` (BALANCE, `allowNegative = false`),
  `Revenue` (TURNOVER, с денормализованными именами тарифа и клиента для
  группировок на дашборде).
- `EntityView` для каждой сущности; `Layout` с разделами Sales и Reports.
- `DashboardPage` — главная страница с KPI, графиками и последними документами.

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
subscription-service/
├── Dockerfile
├── docker-compose.yaml
├── build.gradle
└── src/main/java/com/example/subscription/
    ├── SubscriptionApplication.java
    ├── config/
    │   ├── ApplicationContextHolder.java
    │   └── JobRunrConfig.java
    ├── domain/
    │   ├── catalogs/       Customer, Tariff
    │   ├── documents/      Payment, Subscription, SubscriptionLine
    │   ├── enumerations/   CustomerStatus, SubscriptionStatus, PaymentMethod
    │   └── registers/      AccountBalance, Revenue
    ├── jobs/
    │   └── SubscriptionLifecycleJob.java
    ├── repositories/       Spring Data интерфейсы
    ├── services/
    │   └── SubscriptionService.java
    └── ui/
        ├── layouts/        MainLayout
        ├── pages/          DashboardPage
        └── views/          CustomerView, TariffView, PaymentView, SubscriptionView
```
