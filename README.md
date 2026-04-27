# Book Tracker and Advisor Bot

Telegram-бот для учёта прочитанных книг, ведения списков чтения и получения интеллектуальных рекомендаций. Интегрируется с Google Books API для метаданных и с GigaChat API для персонализированных советов. Реализован на **Java 25 + Spring Framework 7** (без Spring Boot), с MongoDB для хранения данных и ZeroMQ для асинхронной отправки напоминаний.

## Возможности

- Поиск книг через Google Books (`/search`, `/random`).
- Личный список чтения со статусами `хочу прочитать / читаю / прочитано`, прогрессом, оценками, заметками и цитатами.
- Литературные предпочтения (`/preferences add|show|remove`).
- LLM-советник: рекомендации, поиск похожих, саммари книги, свободные вопросы (`/recommend`, `/similar`, кнопка `Саммари от LLM`, `/ask`).
- Шеринг списка по deep-link (`/share`) — друзья видят его в read-only режиме и могут добавить любую книгу к себе.
- Ежедневные напоминания о чтении (`/remind on HH:MM` / `/remind off`) через очередь ZeroMQ.
- Административные HTTP-эндпоинты (`GET /healthcheck`, `GET /users`).

## Команды Telegram

```
/help                            — справка по командам
/search <запрос>                 — поиск книг (до 100 символов)
/search                          — поиск по сохранённым предпочтениям
/random                          — случайная книга
/list [хочу прочитать|читаю|прочитано] — список чтения
/status <номер> <статус>         — изменить статус
/progress <номер> <значение>     — прогресс (только для "читаю")
/remove <номер>                  — удалить книгу
/rate <номер> <1-10>             — оценка (только прочитано)
/note <номер> <текст>            — заметка (≤300 символов, только прочитано)
/quote add <номер> <текст>       — добавить цитату (≤500 символов, до 10 на книгу)
/quotes [номер]                  — показать цитаты (одной книги или всех)
/preferences add <текст>         — добавить предпочтение (≤50 символов, до 15)
/preferences show                — показать предпочтения
/preferences remove <номер>      — удалить предпочтение
/recommend                       — персональные рекомендации LLM
/similar <номер>                 — похожие книги через LLM
/ask <вопрос>                    — свободный вопрос (≤500 символов)
/share                           — поделиться ссылкой на свой список
/remind on <ЧЧ:ММ> | /remind off — ежедневные напоминания
```

## Технологический стек

- **Java 25**, **Spring Framework 7** (Context, Beans, Web, Data MongoDB) — **без Spring Boot**.
- **Spring `RestClient`** — клиент к Telegram, Google Books и GigaChat.
- **Embedded Jetty 12** + Spring `DispatcherServlet` для HTTP API.
- **MongoDB 7** + Spring Data MongoDB.
- **JeroMQ** (Java-биндинги ZeroMQ) для очереди напоминаний.
- **Gradle 9** + Shadow plugin для Fat JAR; **Checkstyle** + **Spotless** (google-java-format) для качества кода.
- **JUnit 5**, **Mockito**, **AssertJ**, **Testcontainers** для тестов.
- **Docker** + **Docker Compose**.


### Запуск через Docker Compose 

```bash
docker compose up --build
```

Поднимется сервис `mongo` и приложение. После старта:
- Бот сразу начнёт опрашивать Telegram.
- HTTP API доступен на `http://localhost:8080`.

Остановка: `docker compose down` (данные Mongo сохраняются в `./docker/data/mongo`).

### Сборка и запуск Fat JAR без Docker

Требуется JDK 25 (Eclipse Temurin или другой OpenJDK 25). Gradle Wrapper загрузит сам Gradle 9.0.

```bash
# Сборка
./gradlew shadowJar

# Запуск (Mongo должен быть доступен)
java -jar build/libs/book-tracker-advisor-bot.jar
```

### Тесты и проверки

```bash
./gradlew check        # unit-тесты + Checkstyle + Spotless
./gradlew test         # только тесты
./gradlew spotlessApply # автоформатирование google-java-format
```

Интеграционный тест `UserRepositoryIT` поднимает Testcontainers MongoDB — для него нужен Docker.

## HTTP API

### `GET /healthcheck`

Публичный, без авторизации. Возвращает статус и список авторов проекта.

```bash
curl http://localhost:8080/healthcheck
# {"status":"UP","authors":["Митяев О.","Попов И.","Фомин М."]}
```

### `GET /users`

Только для администраторов. Требует заголовок `X-API-Key: <ADMIN_API_KEY>`.

```bash
# Без ключа → 401
curl -i http://localhost:8080/users

# С ключом → 200 + JSON со списком пользователей
curl -H "X-API-Key: $ADMIN_API_KEY" http://localhost:8080/users
```

## Docker-образ на Docker Hub

Опубликованный образ: **TODO: ссылка на Docker Hub после публикации**.

## Ссылка на Telegram-бота

`https://t.me/book_tracker_advisor_bot`.

## Архитектура и требования

- Требования: [`docs/Requirements.md`](docs/Requirements.md).
- Архитектура: [`docs/Архитектура.pdf`](docs/Архитектура.pdf).


## Авторы

- Митяев О.
- Попов И.
- Фомин М.

Санкт-Петербургский политехнический университет Петра Великого, 2026.
