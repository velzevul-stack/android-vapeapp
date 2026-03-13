@echo off
chcp 65001 >nul
echo ========================================
echo   БЕЗОПАСНОЕ ОБНОВЛЕНИЕ ПРИЛОЖЕНИЯ
echo ========================================
echo.

set PACKAGE_NAME=com.example.vapestore
set DB_NAME=vape_database
set BACKUP_DIR=db_backup_%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set BACKUP_DIR=%BACKUP_DIR: =0%
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk

echo [ШАГ 1/4] Проверка подключения телефона...
adb devices | find "device" >nul
if errorlevel 1 (
    echo [ОШИБКА] Телефон не подключен или отладка по USB не включена!
    echo.
    echo Инструкция:
    echo 1. Подключите телефон через USB
    echo 2. Включите "Отладка по USB" в настройках разработчика
    echo 3. Разрешите отладку на телефоне
    pause
    exit /b 1
)
echo [OK] Телефон подключен
echo.

echo [ШАГ 2/4] Создание резервной копии БД...
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
adb exec-out run-as %PACKAGE_NAME% cat databases/%DB_NAME% > "%BACKUP_DIR%\%DB_NAME%.db" 2>&1

if not exist "%BACKUP_DIR%\%DB_NAME%.db" (
    echo [ОШИБКА] Не удалось создать бэкап БД!
    echo.
    echo Возможные причины:
    echo - Приложение не установлено на телефоне
    echo - БД еще не создана (приложение не запускалось)
    echo.
    echo Продолжить обновление без бэкапа? (y/n)
    set /p continue=
    if /i not "%continue%"=="y" (
        echo Отменено пользователем
        pause
        exit /b 1
    )
) else (
    for %%A in ("%BACKUP_DIR%\%DB_NAME%.db") do set SIZE=%%~zA
    if %SIZE% LSS 100 (
        echo [ПРЕДУПРЕЖДЕНИЕ] Файл БД очень маленький (%SIZE% байт)
        echo Возможно, БД пустая или произошла ошибка
    ) else (
        echo [OK] Бэкап создан: %BACKUP_DIR%\%DB_NAME%.db (%SIZE% байт)
    )
    
    echo Пытаюсь извлечь дополнительные файлы...
    adb exec-out run-as %PACKAGE_NAME% cat databases/%DB_NAME%-shm > "%BACKUP_DIR%\%DB_NAME%-shm" 2>nul
    adb exec-out run-as %PACKAGE_NAME% cat databases/%DB_NAME%-wal > "%BACKUP_DIR%\%DB_NAME%-wal" 2>nul
)
echo.

echo [ШАГ 3/4] Проверка наличия APK файла...
if not exist "%APK_PATH%" (
    echo [ОШИБКА] APK файл не найден: %APK_PATH%
    echo.
    echo Сначала соберите проект:
    echo gradlew assembleDebug
    pause
    exit /b 1
)
echo [OK] APK найден
echo.

echo [ШАГ 4/4] Установка нового приложения...
echo.
echo ВНИМАНИЕ: Сейчас будет установлено новое приложение поверх старого.
echo Если возникнет ошибка "Signature mismatch", данные останутся в старом приложении.
echo.
pause

adb install -r "%APK_PATH%"
set INSTALL_RESULT=%ERRORLEVEL%

echo.
if %INSTALL_RESULT% EQU 0 (
    echo ========================================
    echo   ОБНОВЛЕНИЕ УСПЕШНО!
    echo ========================================
    echo.
    echo [ВАЖНО] Проверьте приложение на телефоне:
    echo 1. Откройте приложение
    echo 2. Проверьте, что данные на месте (товары, продажи)
    echo 3. Если данные пропали - запустите restore_db.bat
    echo.
    echo Бэкап сохранен в: %CD%\%BACKUP_DIR%
) else (
    echo ========================================
    echo   ОШИБКА ПРИ УСТАНОВКЕ
    echo ========================================
    echo.
    echo Код ошибки: %INSTALL_RESULT%
    echo.
    if %INSTALL_RESULT% EQU 1 (
        echo Возможные причины:
        echo - Ошибка подписи (Signature mismatch)
        echo - Недостаточно места на телефоне
        echo - APK поврежден
        echo.
        echo СТАРОЕ ПРИЛОЖЕНИЕ НЕ УДАЛЕНО - данные в безопасности!
        echo Бэкап сохранен в: %CD%\%BACKUP_DIR%
        echo.
        echo Если нужно установить новое приложение:
        echo 1. Удалите старое приложение вручную (данные пропадут)
        echo 2. Установите новое: adb install "%APK_PATH%"
        echo 3. Восстановите БД: restore_db.bat %BACKUP_DIR%\%DB_NAME%.db
    )
)
echo.
pause
