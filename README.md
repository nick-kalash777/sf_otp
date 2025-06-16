# OTP-сервис

## Функционал:
1) Регистрация пользователей (логин/пароль/административные права)
2) Авторизация пользователей с помощью JWT-токенов
3) Генерация и валидация OTP-кодов
4) Отправка OTP-кодов через email, смс и телеграм.
5) Изменение конфигурации OTP-кодов (для администраторов)
6) Удаление пользователей и связанных с ними кодов (для администраторов)
7) Просмотр всех пользователей (для администраторов)


# Использование
Создайте датабазу, используя скрипт database.sql.

В /dao/DatabaseConnection подключите свою БД.

В /resources подключите свои данные для Email/SMS сервиса и телеграм бота.

## API
1) POST /register
   
   Передаются параметры username, password, is_admin=true/false (опционально)
   
   Пример запроса: curl.exe -X POST "http://localhost:8080/register?username=admin&password=123&is_admin=true"
   
   При успехе вернется 200 код.
2) POST /login
   
   Передаются параметры username, password.
   
   Пример запроса: curl.exe -X POST "http://localhost:8080/login?username=admin&password=123"
   
   При успехе вернется JWT-токен.
   
### Все последующие команды требуют передачи токена авторизации в Headers. 
3) GET /generate_otp
   
   Передается параметр operation_id.

   Пример запроса: curl.exe -X GET "http://localhost:8080/generate_otp?operation_id=1" -H "Authorization: Bearer TOKEN"

   При успехе OTP-код отправляется на почту, смс, телеграм и сохраняется в файл.
4) GET /validate_otp
   
   Передаются параметры code, operation_id.

   Пример запроса: curl.exe -X GET "http://localhost:8080/validate_otp?code=570183&operation_id=1" -H "Authorization: Bearer TOKEN"

   При успехе вернется 200 код.

Для администраторов:
1) POST /change_otp_config
   
   Передаются параметры code_length, code_ttl, max_attempts.

   Пример запроса: curl.exe -X GET "http://localhost:8080/change_otp_config?code_length=6&code_ttl=5&max_attempts=3" -H "Authorization: Bearer TOKEN"

   При успехе вернется 200 код.
2) DELETE /delete_user
   
   Передается параметр id.

   Пример запроса: curl.exe -X DELETE "http://localhost:8080/delete_user?id=2" -H "Authorization: Bearer TOKEN"

   При успехе вернется 200 код.
3) GET /get_users
   
   Пример запроса: curl.exe -X GET "http://localhost:8080/get_users" -H "Authorization: Bearer TOKEN"

   При успехе возвращает список всех пользователей (кроме администратора)
