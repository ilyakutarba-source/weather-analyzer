# Weather Analyzer

REST-сервис получает погоду в Минске из [OpenWeather Current Weather API](https://openweathermap.org/api/current)
по расписанию, сохраняет измерения в PostgreSQL и рассчитывает среднюю температуру.

**Стек:** Java 21, Spring Boot 3.5, Spring Data JPA, RestClient, PostgreSQL 17,
Flyway, JUnit 5, Mockito, Maven Wrapper.

## Запуск

Требуются JDK 21, Docker Compose и API-ключ OpenWeather Current Weather API.
Команды выполняются из корня проекта.

**Windows (PowerShell):**

```powershell
docker compose up -d --wait
$env:WEATHER_API_KEY = "<your-api-key>"
.\mvnw.cmd spring-boot:run
```

**macOS / Linux (bash или zsh):**

```bash
chmod +x mvnw
docker compose up -d --wait
export WEATHER_API_KEY="<your-api-key>"
./mvnw spring-boot:run
```

Приложение доступно на `http://localhost:8080`. Ключ передаётся процессу из текущего терминала.
Первый сбор запускается примерно через 5 секунд, следующие — через 15 минут после завершения предыдущего.
До первого успешного сохранения эндпоинты возвращают 404 при запросе отсутствующих данных.

Локальная БД: `localhost:5433/weather_analyzer`, пользователь `weather_user`, пароль `weather_password`.
Схему создаёт Flyway при запуске; SQL-миграция —
`src/main/resources/db/migration/V1__create_weather_measurements.sql`.

## API

### Последняя сохранённая погода

`GET /api/weather/current`

**PowerShell:**

```powershell
Invoke-RestMethod "http://localhost:8080/api/weather/current" | ConvertTo-Json
```

**macOS / Linux:**

```bash
curl http://localhost:8080/api/weather/current
```

Пример ответа:

```json
{
  "temperatureC": 12.02,
  "windSpeedMetersPerHour": 18828.00,
  "pressureHpa": 1022.00,
  "humidity": 84,
  "weatherCondition": "облачно с прояснениями",
  "location": "Минск",
  "measuredAt": "2026-10-22T11:27:00Z"
}
```

Температура — °C, ветер — м/ч (м/с от OpenWeather × 3600), давление — гПа, влажность — %.
Единица «м/ч» из задания трактуется как метры в час. `measuredAt` — время наблюдения в UTC.
Выбирается запись с самым поздним `measuredAt`, при равенстве — с большим `id`.

### Средняя температура за период

`POST /api/weather/average-temperature`, `Content-Type: application/json`.

**PowerShell:**

```powershell
$weatherBody = '{"from":"22-10-2026","to":"22-10-2026"}'
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/weather/average-temperature" -ContentType "application/json" -Body $weatherBody | ConvertTo-Json -Depth 5
```

**macOS / Linux:**

```bash
curl -X POST http://localhost:8080/api/weather/average-temperature \
  -H "Content-Type: application/json" \
  -d '{"from":"22-10-2026","to":"22-10-2026"}'
```

Пример ответа при наличии измерений:

```json
{
  "from": "22-10-2026",
  "to": "22-10-2026",
  "average_temp": 12.02,
  "daily_averages": [
    {
      "date": "22-10-2026",
      "average_temp": 12.02
    }
  ]
}
```

- `from` и `to` обязательны, формат — `дд-ММ-гггг`, `from` не позже `to`.
- Обе даты включаются в период; границы суток и группировка — в `Europe/Minsk`.
- `average_temp` — среднее всех измерений периода; `daily_averages` — средние по дням, отсортированные по дате.
- Каждая сохранённая запись имеет одинаковый вес; округление — до двух знаков, `HALF_UP`.
- Дни без измерений пропускаются; отсутствие данных за весь период даёт 404.

История накапливается во время работы сборщика. Средние рассчитываются по доступным измерениям,
которые могут покрывать часть суток. Оба эндпоинта читают БД без обращения к OpenWeather.

Ошибки API возвращаются в формате Problem Details (`application/problem+json`):
400 — некорректный запрос или даты, 404 — нет данных, 500 — непредвиденная ошибка.
Подробности серверных ошибок и ошибки фонового сбора записываются в лог.

## Настройки

Файл: `src/main/resources/application.properties`.

```properties
weather.scheduling.enabled=true
weather.scheduling.fixed-delay=PT15M
weather.scheduling.initial-delay=PT5S
```

API-ключ читается из `WEATHER_API_KEY`. Для чтения накопленных данных без ключа и сборщика:

**PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--weather.scheduling.enabled=false"
```

**macOS / Linux:**

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--weather.scheduling.enabled=false"
```

## Тесты и сборка

Полный набор тестов требует работающей PostgreSQL и отдельной БД.
Создайте её один раз, если ещё не существует:

```shell
docker compose exec postgres psql -U weather_user -d weather_analyzer -c "CREATE DATABASE weather_analyzer_test;"
```

| Задача | Windows (PowerShell) | macOS / Linux |
| --- | --- | --- |
| Запуск тестов | `.\mvnw.cmd test` | `./mvnw test` |
| Сборка JAR с тестами | `.\mvnw.cmd clean verify` | `./mvnw clean verify` |

Тесты проверяют HTTP-клиент, расчёты, REST-ответы и ошибки, сохранение и границы выборки в PostgreSQL.
Используется БД `weather_analyzer_test`, изменения тестов репозитория откатываются.
Настоящий API-ключ не требуется, планировщик отключён в тестовом профиле.

Запуск исполняемого JAR на обеих платформах:

```shell
java -jar target/weather-analyzer-0.0.1-SNAPSHOT.jar
```

Для запуска JAR нужны PostgreSQL и установленный `WEATHER_API_KEY`.
