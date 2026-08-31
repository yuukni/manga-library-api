[Manga Library API.md](https://github.com/user-attachments/files/31646644/Manga.Library.API.md)
# Manga Library API

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8-blue?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![MyAnimeList](https://img.shields.io/badge/API-MyAnimeList-2E51A2)](https://myanimelist.net/apiconfig)

A REST API for managing a personal manga library, built with Spring Boot and MySQL. Manga metadata can be searched and imported using the MyAnimeList API.

## Features

- Manga library management
- MyAnimeList API integration
- User authentication and authorization
- Reading progress tracking
- Author and genre management
- Swagger / OpenAPI documentation
- Docker Compose support

## Getting Started

Clone the repository:

```bash
git clone https://github.com/yuukni/manga-library-api.git
cd manga-library-api
```

Create a `.env` file:

```env
MYSQL_DATABASE=manga_db
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_USER=spring
MYSQL_PASSWORD=your_database_password
MAL_CLIENT_ID=your_myanimelist_client_id
```

Start the application:

```bash
docker compose up --build
```

## Services

| Service | URL |
| --- | --- |
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| phpMyAdmin | `http://localhost:8081` |

## MyAnimeList API

Create a MyAnimeList API application and add your Client ID as `MAL_CLIENT_ID`.

Example manga import:

```http
POST /api/manga/import?query=Naruto&limit=5
```

## API Testing

A Postman collection is included in the repository:

```text
Manga-Backend-API.postman_collection.json
```

## License

No license has currently been specified.
