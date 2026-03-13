# Как создать debug-APK через консоль

## Команда для создания debug-APK

Откройте терминал (PowerShell или CMD) в корне проекта и выполните:

```bash
.\gradlew assembleDebug
```

Или для Windows PowerShell:
```powershell
.\gradlew.bat assembleDebug
```

## Где находится созданный APK файл

После успешной сборки APK файл будет находиться по пути:

```
app\build\outputs\apk\debug\app-debug.apk
```

Полный путь для вашего проекта:
```
C:\Users\matv369\AndroidStudioProjects\VapeStoreApp\app\build\outputs\apk\debug\app-debug.apk
```

## Установка на телефон

1. **Через USB:**
   - Подключите телефон к компьютеру
   - Скопируйте файл `app-debug.apk` на телефон
   - На телефоне откройте файл и установите (разрешите установку из неизвестных источников)

2. **Через ADB (если телефон подключен):**
   ```bash
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

3. **Через облачное хранилище:**
   - Загрузите APK в Google Drive / Dropbox
   - Скачайте на телефон и установите

## Важно про ключ подписи

✅ **Debug-APK всегда подписывается одним и тем же debug-ключом**

- Android Studio автоматически использует один debug-ключ для всех debug-сборок
- Ключ находится в: `C:\Users\<username>\.android\debug.keystore`
- Все debug-APK из одного проекта имеют одинаковую подпись
- **Данные сохранятся** при обновлении через debug-APK, так как:
  - Package name одинаковый: `com.example.vapestoreapp`
  - Ключ подписи одинаковый
  - Android распознает это как обновление существующего приложения

## Дополнительные команды

**Очистка перед сборкой:**
```bash
.\gradlew clean assembleDebug
```

**Просмотр всех доступных задач:**
```bash
.\gradlew tasks
```

**Сборка с подробным выводом:**
```bash
.\gradlew assembleDebug --info
```
