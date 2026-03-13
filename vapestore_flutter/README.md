# VapeStore Flutter

Переписанное на Flutter приложение VapeStore с темой Premium Dark.

## Требования

- Flutter SDK 3.0+
- Dart 3.0+

## Установка

```bash
cd vapestore_flutter
flutter pub get
```

## Запуск

```bash
flutter run
```

## Первый запуск

При пустой базе данных откроется экран импорта. Выберите JSON-файл, экспортированный из Android VapeStoreApp.

## Структура

- `lib/core/theme/` — тема Premium Dark
- `lib/data/` — модели, БД (sqflite), репозиторий
- `lib/features/` — экраны: import, home, accept, sell, cabinet, management
