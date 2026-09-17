# social-service

## Описание проекта

Многосервисная социальная сеть, построенная на микросервисной архитектуре с использованием Spring Boot и Kotlin/Java. Платформа предоставляет функционал для управления пользователями, изображениями, комментариями, лайками и активностью пользователей.

## Архитектура

### Микросервисы

- [**User Service**](https://github.com/pYro-sudo12134/social-service-backup/tree/user-service) (`8081`) - управление пользователями и аутентификация
- [**Image Service**](https://github.com/pYro-sudo12134/social-service-backup/tree/image-service) (`8082`) - загрузка и управление изображениями
- [**Comment-Like Service**](https://github.com/pYro-sudo12134/social-service-backup/tree/comment-like-service) (`8083`) - система комментариев и лайков
- [**Activity Service**](https://github.com/pYro-sudo12134/social-service-backup/tree/activity-service) (`8085`) - отслеживание пользовательской активности
- [**API Gateway**](https://github.com/pYro-sudo12134/social-service-backup/tree/gateway) (`8080`) - единая точка входа, маршрутизация и аутентификация

## Технологический стек

### Базы данных и инфраструктура

- **PostgreSQL** - основное реляционное хранилище
- **MongoDB** - хранилище для данных активности
- **Redis** - кэширование и сессии
- **Kafka** - асинхронная коммуникация между сервисами
- **LocalStack** - AWS S3 эмуляция

### Бэкенд
- **Java 21**
- **Spring Boot 3** - основной фреймворк
- **Spring WebFlux** - реактивные REST API
- **Spring Data R2DBC** - реактивный доступ к PostgreSQL
- **Spring Data MongoDB** - работа с MongoDB
- **Spring Data Redis** - кэширование и распределенные данные
- **Spring Cloud Gateway** - API Gateway
- **Spring Kafka** - обработка событий
- **Spring Shell** - для cli(activity-service)
- **Liquibase** - управление миграциями базы данных

### Базы данных и кэширование
- **PostgreSQL 15** - основная реляционная БД
- **MongoDB 6.0** - документное хранилище для активности
- **Redis 7.2** - in-memory кэш

### Инфраструктура
- **Docker/Podman & Kubernetes** - контейнеризация и оркестрация
- **Kafka & Zookeeper** - message broker для событий
- **Nginx Ingress Controller** - маршрутизация в Kubernetes
- **Prometheus+Grafana** - для метрик
- **Waypoint** - деплой
- **Terraform** - тоже деплой, но будет нужен, если перееду на cloud

### Безопасность
- **JWT** - аутентификация и авторизация
- **Bcrypt** - для шифрования
- **Kubernetes Network Policies** - сетевая безопасность
- **CORS & Rate Limiting(Redis)** - защита API

### Тестирование
- **TestContainers** - для тестов с использованием контейнеров
- **JUnit5/Mockito** - используется для создания тестов модульных или дополнения интеграционных

### Фронтенд
- **React+Axios**

honorable mention: мне надо было написать ещё Makefile, чтобы не возиться с ручной загрузкой данных на ноду kind, но его польза не слишком велика


<!-- если на ориг ссылку, то это https://github.com/pYro-sudo/social-service -->
