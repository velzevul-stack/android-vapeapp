# 📝 Заметки для разработчика

## 🔴 Критически важные моменты

### 1. Сохранение данных при обновлении
**Проблема:** У заказчика нет доступа к оригинальному `debug.keystore`, поэтому обновление может не пройти из-за "Signature mismatch".

**Решение:** Реализовать механизм экспорта/импорта БД в JSON файл.

**Приоритет:** 🔴 **ВЫСОКИЙ** - это первое, что нужно сделать!

---

## 📊 Анализ текущего кода

### Формула расчета прибыли
**Найдена в коде:**
```kotlin
// SellViewModel.kt:179
profit = ((product.retailPrice - product.purchasePrice) * quantityToSell) - discount

// Repository.kt:182
profit = (product.retailPrice - product.purchasePrice) * finalQuantity - finalDiscount
```

**Вывод:** Формула правильная. Нужно подтвердить у заказчика.

---

### Асинхронность UI
**Статус:** ✅ **УЖЕ РЕАЛИЗОВАНО**

`SalesManagementViewModel` использует `Flow` и `combine` для автоматического обновления:
```kotlin
repository.getActiveSales()
    .combine(repository.getAllProducts()) { sales, products ->
        Pair(sales, products)
    }
    .collect { (sales, products) ->
        _activeSales.value = sales
        // ...
    }
```

**Действие:** Убрать кнопку "Обновить" из UI или сделать её опциональной.

---

### Метод оплаты
**Статус:** ✅ **УЖЕ РЕАЛИЗОВАНО**

Поле `paymentMethod: String` уже существует в `Sale`:
- Значения: `"cash"` или `"card"`
- По умолчанию: `"cash"`

**Действие:** Добавить отображение разбивки по методам оплаты в статистике.

---

## 🐛 Известные проблемы

### Баг: Crash при добавлении товаров категорий "Снюс"/"Одноразки"
**Локация:** `AcceptViewModel.addNewProduct()` (строки 211-297)

**Возможные причины:**
1. Поле `flavor` может быть пустым для некоторых категорий
2. Проблема с вычислением `orderIndex` для новых категорий
3. Null pointer при обращении к списку брендов

**Решение:**
- Добавить проверки на null/пустые строки
- Убедиться, что `getMaxOrderIndexInCategory()` работает для всех категорий
- Добавить try-catch с логированием ошибок

---

## 📁 Новые файлы для создания

### 1. Экспорт/Импорт БД
- `app/src/main/java/com/example/vapestoreapp/utils/DatabaseExporter.kt`
- `app/src/main/java/com/example/vapestoreapp/utils/DatabaseImporter.kt`

### 2. Новые сущности
- `app/src/main/java/com/example/vapestoreapp/data/Debt.kt`
- `app/src/main/java/com/example/vapestoreapp/data/Reservation.kt`
- `app/src/main/java/com/example/vapestoreapp/data/DebtDao.kt`
- `app/src/main/java/com/example/vapestoreapp/data/ReservationDao.kt`

### 3. Новые ViewModel
- `app/src/main/java/com/example/vapestoreapp/viewmodel/DebtsViewModel.kt`
- `app/src/main/java/com/example/vapestoreapp/viewmodel/ReservationsViewModel.kt`

### 4. Утилиты
- `app/src/main/java/com/example/vapestoreapp/utils/ThemeManager.kt` (для темной/светлой темы)

---

## 🔧 Файлы для изменения

### 1. AppDatabase.kt
- Добавить `Debt` и `Reservation` в `entities`
- Увеличить `version` с 2 до 3
- Создать `MIGRATION_2_3`

### 2. Repository.kt
- Добавить `MIGRATION_2_3` в `.addMigrations()`
- Добавить методы для работы с долгами и резервами
- Добавить методы экспорта/импорта

### 3. CabinetViewModel.kt
- Добавить вычисление статистики по методам оплаты
- Добавить поиск товаров

### 4. SalesManagementViewModel.kt
- Добавить возможность редактирования даты/времени продажи

### 5. AcceptViewModel.kt
- Исправить баг с добавлением товаров категорий "snus"/"disposable"

### 6. StockFormatter.kt
- Добавить отображение резервов на складе

### 7. MainActivity.kt
- Добавить UI для экспорта/импорта данных
- Добавить переключатель темы
- Добавить поиск в разделе товаров
- Добавить экраны для долгов и резервов

---

## 🎯 Приоритеты выполнения

### Фаза 1: Критически важно (сначала это!)
1. ✅ Экспорт/Импорт БД (DatabaseExporter/Importer)
2. ✅ UI для экспорта/импорта
3. ✅ Миграция БД (MIGRATION_2_3)
4. ✅ Тестирование обновления с сохранением данных

### Фаза 2: Новый функционал
5. ✅ Система долгов (Debt + DebtDao + DebtsViewModel + UI)
6. ✅ Система резервов (Reservation + ReservationDao + ReservationsViewModel + UI)
7. ✅ Статистика по методам оплаты
8. ✅ Поиск товаров
9. ✅ Массовое изменение цен
10. ✅ Редактирование даты/времени продажи

### Фаза 3: UI/UX и баг-фиксы
11. ✅ Темная/светлая тема
12. ✅ Исправление бага с добавлением товаров
13. ✅ Исправление копирования прайса
14. ✅ Оптимизация интерфейса

---

## 🧪 Тестирование

### Критические тесты:
1. **Экспорт данных:**
   - Создать тестовые данные (товары + продажи)
   - Экспортировать в JSON
   - Проверить содержимое JSON файла

2. **Импорт данных:**
   - Установить новое приложение (или очистить БД)
   - Импортировать JSON файл
   - Проверить, что все данные восстановлены

3. **Миграция БД:**
   - Установить приложение с версией БД 2
   - Добавить тестовые данные
   - Обновить до версии с БД 3
   - Проверить, что данные не потеряны и новые таблицы созданы

4. **Обновление приложения:**
   - Установить старую версию APK
   - Добавить данные
   - Экспортировать данные
   - Установить новую версию APK
   - Импортировать данные
   - Проверить, что все работает

---

## 📚 Полезные ссылки

### Room Database Migration
- https://developer.android.com/training/data-storage/room/migrating-db-versions

### JSON Serialization (для экспорта/импорта)
- Использовать `kotlinx.serialization` или `Gson`
- Или простой ручной парсинг JSON

### File Picker (для импорта)
- Использовать `ActivityResultContracts.GetContent()` или `OpenDocument`

### Date/Time Picker
- Material 3 DatePicker и TimePicker для Compose

---

## ❓ Вопросы к заказчику

1. **Keystore:** Есть ли у вас `debug.keystore` файл, которым подписывалась текущая версия?
2. **Формула прибыли:** Подтвердите формулу: `(retailPrice - purchasePrice) * quantity - discount`
3. **Резервное копирование:** Предпочтительнее автоматический экспорт при первом запуске или ручной экспорт перед обновлением?
4. **Долги:** Нужна ли возможность частичной оплаты долга или только полная?
5. **Резервы:** Нужна ли автоматическая проверка истекших резервов при запуске приложения?

---

## 📝 Заметки по реализации

### Экспорт БД в JSON
```kotlin
data class DatabaseBackup(
    val version: Int,
    val exportDate: Long,
    val products: List<Product>,
    val sales: List<Sale>
)
```

### Импорт БД из JSON
- Проверить версию БД в JSON
- Если версия < текущей, выполнить миграцию
- Вставить данные в БД

### Резервы на складе
- При создании резерва НЕ уменьшать `stock`
- При отображении склада учитывать резервы: `availableStock = stock - reservedQuantity`
- При продаже резерва уменьшить `stock` и пометить резерв как проданный

### Долги
- При создании долга можно зарезервировать товары (создать Reservation)
- При оплате долга превратить в обычную продажу (Sale)

---

**Дата создания:** 2026-03-06  
**Версия:** 1.0
