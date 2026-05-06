# Docker

## Создание и запуск

Принудительное пересоздание контейнеров:
```bash
docker-compose up --force-recreate
```

Запуск только приложения (без зависимостей):
```bash
docker-compose up --no-deps app
```

Запуск только бд:
```bash
docker-compose up -d db
```

### Остановка и удаление контейнеров

Остановка с удалением контейнеров:
```bash
docker-compose down
```

Остановка с удалением контейнеров и томов (volumes):
```bash
docker-compose down -v
```

Остановка с удалением контейнеров, томов (volumes) и контейнеров старых версий:
```bash
docker-compose down -v --remove-orphans
```

Остановка без удаления контейнеров:
```bash
docker-compose stop
```

### Просмотр и отладка

Логи всех сервисов:
```bash
docker-compose logs
```

Логи приложения в реальном времени:
```bash
docker-compose logs -f app
```

Список запущенных контейнеров:
```bash
docker-compose ps
```