@echo off
chcp 65001 >nul
echo ========================================
echo   ВОССТАНОВЛЕНИЕ БД ИЗ БЭКАПА
echo ========================================
echo.

set PACKAGE_NAME=com.example.vapestore
set DB_NAME=vape_database

if "%~1"=="" (
    echo Использование: restore_db.bat путь\к\vape_database.db
    echo.
    echo Пример: restore_db.bat db_backup_20240306_143022\vape_database.db
    echo.
    echo Или просто перетащите файл .db на этот скрипт!
    pause
    exit /b 1
)

set BACKUP_FILE=%~1

if not exist "%BACKUP_FILE%" (
    echo [ОШИБКА] Файл не найден: %BACKUP_FILE%
    pause
    exit /b 1
)

echo [ШАГ 1/3] Проверка подключения телефона...
adb devices | find "device" >nul
if errorlevel 1 (
    echo [ОШИБКА] Телефон не подключен!
    pause
    exit /b 1
)
echo [OK] Телефон подключен
echo.

echo [ШАГ 2/3] Проверка, что приложение установлено...
adb shell pm list packages | findstr "%PACKAGE_NAME%" >nul
if errorlevel 1 (
    echo [ОШИБКА] Приложение не установлено на телефоне!
    echo Установите приложение сначала.
    pause
    exit /b 1
)
echo [OK] Приложение установлено
echo.

echo [ШАГ 3/3] Восстановление БД...
echo Загружаю файл на телефон...
adb push "%BACKUP_FILE%" /data/local/tmp/%DB_NAME%.db
if errorlevel 1 (
    echo [ОШИБКА] Не удалось загрузить файл на телефон
    pause
    exit /b 1
)

echo Копирую БД в папку приложения...
adb shell run-as %PACKAGE_NAME% cp /data/local/tmp/%DB_NAME%.db databases/%DB_NAME%
if errorlevel 1 (
    echo [ОШИБКА] Не удалось скопировать БД
    pause
    exit /b 1
)

adb shell run-as %PACKAGE_NAME% chmod 660 databases/%DB_NAME%

echo Очищаю временный файл...
adb shell rm /data/local/tmp/%DB_NAME%.db

echo.
echo ========================================
echo   БД ВОССТАНОВЛЕНА!
echo ========================================
echo.
echo [ВАЖНО] Перезапустите приложение на телефоне!
echo Закройте приложение полностью и откройте заново.
echo.
pause
