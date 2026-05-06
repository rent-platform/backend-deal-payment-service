# Deal-Payment Service (Rent Platform)

## Overview

`deal-payment-service` — микросервис сделок и платежей арендной платформы.

Отвечает за:

- сделки аренды (создание, подтверждение, старт, завершение, отмена)
- двухстороннее подтверждение старта и завершения аренды
- интеграцию с ЮKassa (платежи, холдирование, возвраты)
- транзакции (аренда, залог, возврат, штраф)
- отзывы и рейтинги
- жалобы (создание и обработка модераторами)
- просрочки (авто-завершение с штрафом)
- интеграцию с catalog-service (проверка календаря доступности)
- интеграцию с user-service (платёжный профиль, публичные данные)

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Security (JWT Resource Server)
- Spring Data JPA
- PostgreSQL
- Flyway
- MapStruct
- RestClient
- Swagger (OpenAPI)
- Docker

---

## Ports

| Service      | Port |
|-------------|------|
| Gateway     | 8080 |
| Deal-Payment | 8083 |
| Catalog     | 8082 |
| User        | 8081 |

---

## Base URL

Через gateway:

- /api/deals
- /api/reviews
- /api/complaints
- /api/webhooks

## Domain Model

### Deal

Основная сущность сделки аренды.

Ключевые поля:

- id (UUID)
- itemId
- renterId (арендатор)
- ownerId (владелец)
- startDate / endDate
- pricingMode (DAY / HOUR)
- pricePerDaySnapshot / pricePerHourSnapshot
- totalPrice
- depositAmount
- status

### DealStatus

Жизненный цикл сделки:

```
PENDING
 ├─→ CONFIRMED (владелец подтвердил)
 ├─→ REJECTED (владелец отклонил)
 └─→ CANCELLED (любая сторона отменила)

CONFIRMED
 ├─→ PAYMENT_PENDING (создан платёж)
 └─→ CANCELLED (любая сторона отменила)

PAYMENT_PENDING
 ├─→ ACTIVE (обе стороны подтвердили старт)
 └─→ CANCELLED (любая сторона отменила)

ACTIVE
 ├─→ COMPLETED (обе стороны подтвердили завершение)
 └─→ CANCELLED (досрочная отмена с частичным возвратом)
```

- `PENDING` — заявка создана, ждёт подтверждения владельцем
- `CONFIRMED` — владелец подтвердил, ждёт создания платежа
- `PAYMENT_PENDING` — платёж создан, ждёт оплаты арендатором
- `ACTIVE` — обе стороны подтвердили старт, аренда идёт
- `COMPLETED` — обе стороны подтвердили завершение, деньги переведены
- `REJECTED` — владелец отклонил заявку
- `CANCELLED` — сделка отменена (с частичным возвратом, если была ACTIVE)

### Transaction

Финансовая транзакция по сделке.

Типы:

- `RENTAL` — оплата аренды
- `DEPOSIT_HOLD` — заморозка залога
- `DEPOSIT_RELEASE` — возврат залога
- `PENALTY` — штраф

Статусы:

- `PENDING` — ожидает
- `HELD` — заморожено (холдирование)
- `CAPTURED` — списано
- `REFUNDED` — возвращено
- `FAILED` — ошибка
- `CANCELLED` — отменено

### DealConfirmation

Двухстороннее подтверждение старта и завершения:

- dealId
- userId
- action (START / COMPLETE)
- confirmedAt

Аренда начинается когда **обе стороны** подтвердили START.
Завершается когда **обе стороны** подтвердили COMPLETE.

### DealReview

Отзыв о сделке:

- dealId
- reviewerId / reviewedUserId
- reviewType (RENTER_TO_OWNER / OWNER_TO_RENTER)
- rating (1-5)
- text

Можно оставить только после COMPLETED. Один отзыв на сделку от каждого участника.

### Complaint

Жалоба на пользователя или объявление:

- authorId
- targetType (USER / ITEM)
- targetId
- reason
- status (OPEN / IN_PROGRESS / RESOLVED / DISMISSED)
- handledBy (модератор)
- resolution

---

## Payments (ЮKassa)

### Принцип работы

1. **Холдирование** — при создании платежа деньги замораживаются на карте арендатора (аренда + залог), но не списываются
2. **Оплата** — арендатор переходит по `confirmationUrl` и оплачивает картой. ЮKassa отправляет webhook `payment.succeeded` → статус транзакций HELD
3. **Двухсторонний старт** — обе стороны подтверждают → сделка ACTIVE
4. **Двухстороннее завершение** — обе стороны подтверждают:
    - `itemOk=true` → capture аренды арендодателю, refund залога арендатору
    - `itemOk=false` → capture аренды + залога арендодателю
5. **Досрочная отмена** — capture за использованные дни, refund за неиспользованные дни + возврат залога
6. **Просрочка 3+ часа** — авто-завершение с полным удержанием аренды и залога

### Конфигурация

**Переменные окружения:**

| Переменная              | Описание                  |
|------------------------|--------------------------|
| YOOKASSA_SHOP_ID       | ID магазина в ЮKassa      |
| YOOKASSA_SECRET_KEY    | Секретный ключ (test_... или live_...) |
| YOOKASSA_RETURN_URL    | URL для редиректа после оплаты |

**application.yml:**

```yaml
yookassa:
  shop-id: ${YOOKASSA_SHOP_ID}
  secret-key: ${YOOKASSA_SECRET_KEY}
  return-url: ${YOOKASSA_RETURN_URL:http://localhost:3000/payment/return}
  mock-enabled: false
```

## Тестовый режим
Для локальной разработки и тестирования без реальной ЮKassa:

```yaml
yookassa:
mock-enabled: true
```

### В мок-режиме:

- Платёж создаётся с фейковым paymentId
- confirmationUrl ведёт на локальный заглушечный URL
- Оплата симулируется через webhook вручную
- Транзакции обновляются без реальных запросов к API ЮKassa

### Симуляция оплаты:

```
curl -X 'POST' \
'http://localhost:8080/api/webhooks/yookassa' \
-H 'Content-Type: application/json' \
-d '{
"event": "payment.succeeded",
"object": {
"id": "mock_payment_...",
"status": "succeeded"
}
}'
```

### Боевой режим

Для продакшена:

```yaml
yookassa:
mock-enabled: false
shop-id: ${YOOKASSA_SHOP_ID}
secret-key: ${YOOKASSA_SECRET_KEY}
```

Для тестирования с реальной ЮKassa используются тестовые карты:

- Номер: 5555 5555 5555 4441
- Срок: любой будущий (12/28)
- CVC: любой (123)

После оплаты ЮKassa автоматически отправляет webhook на /api/webhooks/yookassa.

---

# Public Endpoints
### Рейтинг пользователя

```
GET /api/reviews/users/{userId}/summary
```

Ответ:
```
json
{
"overallRating": 4.5,
"totalReviews": 8,
"ownerRating": 4.8,
"ownerReviews": 5,
"renterRating": 4.0,
"renterReviews": 3
}
```

Поля:

* overallRating — общий рейтинг (среднее между арендодателем и арендатором)

* ownerRating / ownerReviews — рейтинг как арендодатель

* renterRating / renterReviews — рейтинг как арендатор

### Рейтинг товара

```
GET /api/reviews/items/{itemId}/summary
```

### Отзывы о пользователе

```
GET /api/reviews/users/{userId}
```

### Отзывы о товаре

```
GET /api/reviews/items/{itemId}
```

# Auth Required Endpoints

### Deals
Создать сделку

```
POST /api/deals
```

Проверки:

* товар ACTIVE

* арендатор ≠ владелец

* календарь доступности (через catalog-service)

* нет конфликтов с CONFIRMED / PAYMENT_PENDING / ACTIVE сделками

* нет повторной заявки от того же пользователя

### Получить сделку по ID

```
GET /api/deals/{dealId}
```

Доступно только участникам сделки.

### Мои сделки (арендатор)

```
GET /api/deals/my/renter
```

* Фильтр по статусу: ?status=ACTIVE

### Мои сделки (владелец)

```
GET /api/deals/my/owner
```

* Фильтр по статусу: ?status=PENDING

### Статусы сделок
Подтвердить сделку (владелец)

```
POST /api/deals/{dealId}/confirm
```

Автоматически отклоняет конфликтующие PENDING-сделки на те же даты.

### Отклонить сделку (владелец)

```
POST /api/deals/{dealId}/reject
```

Тело запроса:

```
json
{
"reason": "Не подходит по времени"
}
```

### Отменить сделку

```
POST /api/deals/{dealId}/cancel
```
Доступно для статусов: PENDING, CONFIRMED, PAYMENT_PENDING.

Тело запроса:

```
json
{
"reason": "Передумал"
}
```

### Подтвердить старт аренды

```
POST /api/deals/{dealId}/confirm-start
```

Обе стороны должны подтвердить для перехода в ACTIVE.

### Подтвердить завершение аренды

```
POST /api/deals/{dealId}/confirm-complete?itemOk=true
```

* Обе стороны должны подтвердить.

* itemOk=true (по умолчанию) — залог возвращается арендатору

* itemOk=false — вещь повреждена, залог уходит арендодателю

## Платежи
### Создать платёж (владелец)

```
POST /api/deals/{dealId}/payment
```

Создаёт платёж в ЮKassa с холдированием (аренда + залог).

Ответ:

```
json
{
"paymentId": "2d5be9f2-000f-5000-8000-...",
"confirmationUrl": "https://yoomoney.ru/checkout/...",
"status": "pending"
}
```

### Webhook (ЮKassa)

```
POST /api/webhooks/yookassa
```

Принимает уведомления от ЮKassa без авторизации.

При payment.succeeded обновляет статус транзакций на HELD.

## Отзывы
### Оставить отзыв

```
POST /api/deals/{dealId}/review
```
Только для участников COMPLETED сделки. Один отзыв от каждого.

Тело запроса:

```
json
{
"rating": 5,
"text": "Отличная вещь!"
}
```

### Отзывы по сделке

```
GET /api/deals/{dealId}/reviews
```

## Жалобы
### Подать жалобу

```
POST /api/complaints
```

Тело запроса:

```
json
{
"targetType": "USER",
"targetId": "UUID",
"reason": "Мошенничество"
}
```

### Список жалоб (модератор, admin, super_admin)

```
GET /api/complaints
```
Фильтр: ?status=OPEN

По умолчанию возвращаются OPEN и IN_PROGRESS.

### Обработать жалобу (модератор, admin, super_admin)

```
PUT /api/complaints/{complaintId}/handle
```

Тело запроса:

```
json
{
"status": "RESOLVED",
"resolution": "Пользователь заблокирован"
}
```

# Роли
| Роль         | Жалобы | Блокировка           |
|-------------|:------:|:---------------------|
| user        | ❌     | ❌                   |
| moderator   | ✅     | user                 |
| admin       | ✅     | moderator, user       |
| super_admin | ✅     | admin, moderator, user |
---

## Error Handling

### 400 Bad Request

- invalid deal status transition
- deal time conflict (даты уже заняты)
- item not available for rent
- already reviewed this deal
- already have active deal for this item
- cannot set past date
- owner cannot rent own item

### 403 Forbidden

- access denied (not deal participant)
- insufficient permissions (roles)

### 404 Not Found

- deal not found
- complaint not found

### 500 Internal Server Error

- internal server error

---


## Pagination

Используется Spring Pageable:

```
?page=0&size=10&sort=createdAt,desc
```

---

## Run

### Build

```bash
./gradlew build -x test
```

## Docker
```bash
docker compose up --build
```

## Environment Variables

| Переменная               | Описание                  | По умолчанию |
|--------------------------|--------------------------|-------------|
| PG_HOST                  | PostgreSQL хост           | localhost   |
| PG_PORT                  | PostgreSQL порт           | 5433        |
| PG_DATABASE              | Имя БД                   | deal_db     |
| PG_USER                  | Пользователь БД           | postgres    |
| PG_PASSWORD              | Пароль БД                | 12345       |
| YOOKASSA_SHOP_ID         | ID магазина ЮKassa        | —           |
| YOOKASSA_SECRET_KEY      | Секретный ключ ЮKassa     | —           |
| YOOKASSA_RETURN_URL      | URL возврата после оплаты | —           |

# MVP Features
Создание сделок с проверкой календаря доступности

Подтверждение / отклонение сделок владельцем

Двухсторонний старт и завершение аренды

Интеграция с ЮKassa (платежи, холдирование, возвраты)

Мок-режим ЮKassa для локального тестирования

Частичный возврат при досрочной отмене

Авто-завершение с штрафом при просрочке

Отзывы и рейтинги (общий, как арендодатель, как арендатор)

Жалобы (создание пользователем, обработка модератором)