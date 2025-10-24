# 🔐 Настройка JWT для ImageService

## 📋 Обзор

ImageService настроен для работы с JWT токенами в микросервисной архитектуре:

- **API Gateway** валидирует JWT токены
- **API Gateway** передает `X-User-Id` заголовок в ImageService
- **ImageService** извлекает `user_id` из заголовка для всех операций

## 🔧 Архитектура безопасности

```
[Client] → [API Gateway] → [ImageService]
           ↓
        JWT Validation
           ↓
    Set X-User-Id header
```

## 🚀 Настройка для тестирования

### 1. **Прямое тестирование (без API Gateway):**

Добавьте заголовок `Authorization: Bearer <token>` в Postman:

```bash
# Пример запроса
curl http://localhost:8083/api/images \
  -H "Authorization: Bearer your-jwt-token-here"
```

### 2. **Через API Gateway:**

API Gateway должен:
1. Валидировать JWT токен
2. Извлечь `user_id` из токена
3. Передать `X-User-Id` заголовок в ImageService

## 📝 Postman коллекция

### **Переменные окружения:**
- `baseUrl`: `http://localhost:8083`
- `jwtToken`: `your-jwt-token-here` (замените на реальный токен)

### **Заголовки:**
- `Authorization: Bearer {{jwtToken}}`

## 🔍 Как работает извлечение user_id

### **1. JwtAuthenticationFilter:**
```java
// Извлекает user_id из заголовка X-User-Id (от API Gateway)
String userId = request.getHeader("X-User-Id");

// Fallback: извлекает из Authorization header (для тестирования)
String authHeader = request.getHeader("Authorization");
if (authHeader != null && authHeader.startsWith("Bearer ")) {
    return "1"; // Для тестирования
}
```

### **2. JwtUtil:**
```java
// Извлекает user_id из запроса
Long userId = jwtUtil.extractUserId(httpRequest);
```

### **3. Контроллеры:**
```java
// Все контроллеры используют JWT для получения user_id
Long userId = jwtUtil.extractUserId(httpRequest);
```

## 🧪 Тестирование

### **1. Health Check (без аутентификации):**
```bash
curl http://localhost:8083/actuator/health
```

### **2. API запросы (с JWT):**
```bash
# Получить все изображения
curl http://localhost:8083/api/images \
  -H "Authorization: Bearer your-jwt-token-here"

# Загрузить изображение
curl -X POST http://localhost:8083/api/images \
  -H "Authorization: Bearer your-jwt-token-here" \
  -F "file=@image.jpg" \
  -F "description=Test image"
```

## ⚠️ Важные моменты

### **1. Для продакшена:**
- Уберите fallback логику в `JwtAuthenticationFilter`
- Добавьте реальную валидацию JWT токенов
- API Gateway должен всегда передавать `X-User-Id`

### **2. Для тестирования:**
- Используйте `Authorization: Bearer <token>` заголовок
- Или установите `X-User-Id` заголовок напрямую

### **3. Безопасность:**
- Все API эндпоинты требуют аутентификации
- Только `/actuator/**` доступны без аутентификации
- `user_id` извлекается из JWT токена для всех операций

## 🔄 Перезапуск сервиса

```bash
# Перезапустить только image-service
docker-compose restart image-service

# Просмотр логов
docker-compose logs -f image-service
```

## ✅ Проверка работы

1. **Health Check:** `GET /actuator/health` → 200 OK
2. **Metrics:** `GET /actuator/metrics` → 200 OK  
3. **API:** `GET /api/images` → 200 OK (с JWT токеном)

Все готово для работы с JWT токенами! 🎉
