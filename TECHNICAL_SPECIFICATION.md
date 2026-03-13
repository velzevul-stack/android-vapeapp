# Техническое Задание (ТЗ) на доработку Android-приложения VapeStoreApp

## 📋 Общая информация

**Проект:** VapeStoreApp (CRM для магазина вейпов)  
**Текущая версия БД:** 2  
**Текущая версия приложения:** 1.0 (versionCode = 1)  
**Архитектура:** MVVM, Room Database, Jetpack Compose, Kotlin Coroutines  
**Текущий APK:** `app-debug.apk` (установлен на устройстве заказчика)

---

## 🔴 КРИТИЧЕСКИ ВАЖНОЕ ТРЕБОВАНИЕ: Сохранение данных при обновлении

### Проблема
Текущая версия приложения (`app-debug.apk`) уже используется заказчиком, и в ней накоплена реальная база данных продаж и товаров. При обновлении приложения **НЕДОПУСТИМО** потерять эти данные.

### Решение: Механизм экспорта/импорта БД

#### 1. Экспорт данных перед обновлением
**Файл для реализации:** `app/src/main/java/com/example/vapestoreapp/utils/DatabaseExporter.kt` (создать новый)

**Функционал:**
- Экспорт всех данных из Room БД в JSON файл
- Сохранение в `ExternalStorage` (Downloads или Documents)
- Структура JSON:
```json
{
  "version": 2,
  "exportDate": 1234567890,
  "products": [...],
  "sales": [...]
}
```

**Методы:**
```kotlin
suspend fun exportDatabase(context: Context): File? {
    // Экспорт всех продуктов и продаж в JSON
    // Сохранение в Downloads/VapeStoreBackup_YYYYMMDD_HHmmss.json
    // Возврат File или null при ошибке
}
```

#### 2. Импорт данных после обновления
**Файл:** `app/src/main/java/com/example/vapestoreapp/utils/DatabaseImporter.kt` (создать новый)

**Функционал:**
- Чтение JSON файла из ExternalStorage
- Восстановление данных в Room БД
- Проверка версии БД и миграция при необходимости

**Методы:**
```kotlin
suspend fun importDatabase(context: Context, backupFile: File): Boolean {
    // Импорт из JSON в Room БД
    // Возврат true при успехе, false при ошибке
}
```

#### 3. UI для экспорта/импорта
**Экран:** Добавить в настройки приложения (или отдельный экран "Резервное копирование")

**Элементы:**
- Кнопка "Экспортировать данные" → сохраняет JSON файл
- Кнопка "Импортировать данные" → открывает FilePicker для выбора JSON
- Toast/Alert с результатом операции

**Инструкция для заказчика:**
1. Перед обновлением: Открыть приложение → Настройки → "Экспортировать данные"
2. Сохранить файл на устройство (например, в Downloads)
3. Установить новую версию APK
4. После установки: Открыть приложение → Настройки → "Импортировать данные" → Выбрать сохраненный файл

---

## 📊 Миграция базы данных

### Текущее состояние
- **Версия БД:** 2
- **Существующая миграция:** `MIGRATION_1_2` (добавляет `orderIndex`)

### Новые сущности и поля

#### 1. Таблица `debts` (Долги)
```kotlin
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerName: String,
    val products: String, // JSON строка с массивом Product IDs и количеством
    val date: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val isPaid: Boolean = false
)
```

#### 2. Таблица `reservations` (Резервы)
```kotlin
@Entity(tableName = "reservations")
data class Reservation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerName: String,
    val productId: Int,
    val quantity: Int,
    val reservationDate: Long = System.currentTimeMillis(),
    val expirationDate: Long, // Дата истечения резерва
    val isSold: Boolean = false // true если резерв превращен в продажу
)
```

#### 3. Новые поля в `Sale`
- `paymentMethod` уже существует (String, по умолчанию "cash")
- Добавить возможность редактирования `date` и `time` (уже есть поле `date: Long`)

### Миграция MIGRATION_2_3

**Файл:** `app/src/main/java/com/example/vapestoreapp/data/AppDatabase.kt`

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Создаем таблицу долгов
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS debts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                customerName TEXT NOT NULL,
                products TEXT NOT NULL,
                date INTEGER NOT NULL,
                totalAmount REAL NOT NULL,
                isPaid INTEGER NOT NULL DEFAULT 0
            )
        """)
        
        // Создаем таблицу резервов
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS reservations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                customerName TEXT NOT NULL,
                productId INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                reservationDate INTEGER NOT NULL,
                expirationDate INTEGER NOT NULL,
                isSold INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(productId) REFERENCES products(id)
            )
        """)
    }
}
```

**Обновить AppDatabase:**
```kotlin
@Database(
    entities = [Product::class, Sale::class, Debt::class, Reservation::class],
    version = 3, // Увеличиваем версию
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // ...
    companion object {
        // ...
        val MIGRATION_2_3 = ...
    }
}
```

**Обновить Repository:**
```kotlin
.addMigrations(
    AppDatabase.MIGRATION_1_2,
    AppDatabase.MIGRATION_2_3  // Добавляем новую миграцию
)
```

---

## 🔧 Функциональные изменения

### 1. Исправление асинхронности UI (UI Updates)

**Проблема:** Кнопка "Обновить" в управлении продажами не работает или требует ручного нажатия. Данные не актуальны.

**Решение:**
- ✅ **УЖЕ РЕАЛИЗОВАНО:** `SalesManagementViewModel` использует `Flow` и `combine` для автоматического обновления
- ✅ **УЖЕ РЕАЛИЗОВАНО:** Данные обновляются автоматически через `loadAllDataRealTime()`
- **Действие:** Убрать кнопку "Обновить" из UI или сделать её принудительной пересинхронизацией (опционально)

**Файлы:**
- `app/src/main/java/com/example/vapestoreapp/viewmodel/SalesManagementViewModel.kt` - уже использует Flow
- `app/src/main/java/com/example/vapestoreapp/screens/MainActivity.kt` - проверить наличие кнопки "Обновить"

---

### 2. Система "Долги" (Debts)

**Новая сущность:** `Debt`

**DAO:** `app/src/main/java/com/example/vapestoreapp/data/DebtDao.kt` (создать новый)
```kotlin
@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE isPaid = 0 ORDER BY date DESC")
    fun getAllActiveDebts(): Flow<List<Debt>>
    
    @Insert
    suspend fun insert(debt: Debt)
    
    @Update
    suspend fun update(debt: Debt)
    
    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Int): Debt?
}
```

**UI:**
- Отдельная вкладка или экран "Долги"
- Список всех долгов с указанием имени клиента
- Кнопка "Продать долг" возле каждого долга
- При нажатии: товары списываются (если были зарезервированы) или фиксируется продажа, долг помечается как оплаченный

**ViewModel:** `app/src/main/java/com/example/vapestoreapp/viewmodel/DebtsViewModel.kt` (создать новый)

**Методы:**
```kotlin
fun createDebt(customerName: String, products: List<Product>, totalAmount: Double)
fun payDebt(debtId: Int) // Превращает долг в продажу
```

---

### 3. Система "Резервы" (Reservations)

**Новая сущность:** `Reservation`

**DAO:** `app/src/main/java/com/example/vapestoreapp/data/ReservationDao.kt` (создать новый)
```kotlin
@Dao
interface ReservationDao {
    @Query("SELECT * FROM reservations WHERE isSold = 0 ORDER BY expirationDate ASC")
    fun getAllActiveReservations(): Flow<List<Reservation>>
    
    @Insert
    suspend fun insert(reservation: Reservation)
    
    @Update
    suspend fun update(reservation: Reservation)
    
    @Query("SELECT * FROM reservations WHERE productId = :productId AND isSold = 0")
    suspend fun getReservationsForProduct(productId: Int): List<Reservation>
}
```

**Логика склада:**
- Товар в резерве **числится на складе** (stock не уменьшается)
- Товар в резерве **НЕ отображается в общем прайсе** для продажи другим клиентам
- На складе отображается: "Всего: 10 шт (из них 2 в резерве)"

**UI:**
- Отдельная вкладка "Резервы"
- Список резервов с именем клиента, датой резерва и сроком истечения
- Кнопка "Продать резерв" → превращает резерв в реальную продажу
- Автоматический возврат: Если срок истек, товар возвращается в свободную продажу (или уведомление)

**ViewModel:** `app/src/main/java/com/example/vapestoreapp/viewmodel/ReservationsViewModel.kt` (создать новый)

**Методы:**
```kotlin
fun createReservation(customerName: String, productId: Int, quantity: Int, expirationDays: Int)
fun sellReservation(reservationId: Int) // Превращает резерв в продажу
fun checkExpiredReservations() // Проверяет истекшие резервы
```

**Обновить StockFormatter:**
- При форматировании склада учитывать резервы
- Показывать: "Всего: X шт (из них Y в резерве)"

---

### 4. Финансы и Статистика

#### 4.1. Разделение оплаты
- ✅ **УЖЕ РЕАЛИЗОВАНО:** Поле `paymentMethod` в `Sale` (String: "cash" или "card")
- ✅ **УЖЕ РЕАЛИЗОВАНО:** При оформлении продажи есть выбор метода оплаты
- **Действие:** Проверить, что UI корректно отображает метод оплаты

#### 4.2. Экран "Продажи"
**Требования:**
- Отображать **Общую выручку** (Total Revenue) за период (сумма всех `revenue`)
- Отображать **Чистую прибыль** (Profit) за период (сумма всех `profit`)
- **Важно:** Перепроверить формулу расчета прибыли

**Текущая формула прибыли:**
```kotlin
// В SellViewModel.kt (строка 179):
profit = ((product.retailPrice - product.purchasePrice) * quantityToSell) - discount

// В Repository.kt (строка 182):
profit = (product.retailPrice - product.purchasePrice) * finalQuantity - finalDiscount
```

**Формула правильная:** `(Цена продажи - Цена закупки) * Количество - Скидка`

**Действие:** Добавить отображение разбивки по методам оплаты:
- "Карта: 5000₽ / Наличные: 2000₽"

**Файл:** `app/src/main/java/com/example/vapestoreapp/viewmodel/CabinetViewModel.kt`
- Метод `loadSalesData()` уже вычисляет `dayRevenue` и `dayProfit`
- Добавить вычисление по `paymentMethod`

#### 4.3. Статистика за день
**Требования:**
- Показывать, сколько за день было оборота по карте, а сколько наличными

**Реализация:**
```kotlin
data class DayStatistics(
    val date: Long,
    val totalRevenue: Double,
    val cashRevenue: Double,
    val cardRevenue: Double,
    val totalProfit: Double,
    val cashProfit: Double,
    val cardProfit: Double
)
```

**Метод в SaleDao:**
```kotlin
@Query("""
    SELECT 
        SUM(CASE WHEN paymentMethod = 'cash' THEN revenue ELSE 0 END) as cashRevenue,
        SUM(CASE WHEN paymentMethod = 'card' THEN revenue ELSE 0 END) as cardRevenue,
        SUM(CASE WHEN paymentMethod = 'cash' THEN profit ELSE 0 END) as cashProfit,
        SUM(CASE WHEN paymentMethod = 'card' THEN profit ELSE 0 END) as cardProfit
    FROM sales 
    WHERE date BETWEEN :start AND :end AND isCancelled = 0
""")
suspend fun getDayStatistics(start: Long, end: Long): DayStatistics?
```

---

### 5. Управление товарами (Products)

#### 5.1. Поиск
**Требование:** Добавить кнопку поиска в раздел "Товары"

**Реализация:**
- Добавить `SearchBar` или `TextField` с иконкой поиска
- Использовать метод `repository.searchProducts(query)`
- Фильтровать список товаров по введенному запросу

**Файл:** `app/src/main/java/com/example/vapestoreapp/viewmodel/CabinetViewModel.kt`
- Добавить `_searchQuery = MutableStateFlow("")`
- В `applyFilter()` учитывать `searchQuery`

#### 5.2. Массовое изменение цен
**Требование:** Добавить функцию "Изменить цену закупки для бренда X"

**Реализация:**
- Диалог: Выбор бренда → Ввод новой цены закупки → Подтверждение
- Обновить все товары этого бренда новой ценой закупки

**Метод в Repository:**
```kotlin
suspend fun updatePurchasePriceForBrand(brand: String, newPrice: Double) {
    val products = productDao.getProductsByBrand(brand)
    products.forEach { product ->
        productDao.update(product.copy(purchasePrice = newPrice))
    }
}
```

#### 5.3. Копирование прайса
**Проблема:** При копировании прайса появляется строка "📊 ИТОГО: ...", которую нужно убрать

**Решение:**
- Найти место копирования прайса в UI
- Исключить строку "📊 ИТОГО: ..." из копируемого текста
- Общее количество товаров вынести в заголовок или отдельное сообщение, не включать в список для копирования

**Файл:** Вероятно `app/src/main/java/com/example/vapestoreapp/MainActivity.kt` или отдельный компонент

---

### 6. Редактирование продаж

**Требование:** В истории продаж добавить возможность изменить **Дату** и **Время** уже совершенной продажи (для коррекции ошибок учета)

**Реализация:**
- В диалоге редактирования продажи добавить поля для выбора даты и времени
- Использовать `DatePicker` и `TimePicker` (или `DateTimePicker`)
- При сохранении обновлять поле `date: Long` в `Sale`

**Файл:** `app/src/main/java/com/example/vapestoreapp/viewmodel/SalesManagementViewModel.kt`
- Метод `saveCorrection()` уже существует
- Добавить параметры `newDate: Long?` в `correctSale()`

---

## 🎨 UI/UX и исправление багов

### 1. Темы оформления

**Требование:** Реализовать поддержку **Dark Mode** (Тёмная тема) и **Light Mode** (Светлая тема)

**Реализация:**
- Использовать Material 3 Dynamic Colors
- Добавить переключатель в настройках приложения
- Сохранять выбор в `SharedPreferences`

**Файлы:**
- `app/src/main/java/com/example/vapestoreapp/ui/theme/Theme.kt` - уже существует
- Добавить `ThemeManager` для управления темой
- Использовать `MaterialTheme.colorScheme` для автоматической поддержки темной/светлой темы

**Код:**
```kotlin
// ThemeManager.kt
class ThemeManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)
    
    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }
}

// В MainActivity.kt
val isDarkMode = remember { mutableStateOf(themeManager.isDarkMode()) }
MaterialTheme(
    colorScheme = if (isDarkMode.value) darkColorScheme() else lightColorScheme()
) {
    // UI
}
```

---

### 2. Баг-фиксы

#### 2.1. Crash при добавлении товаров категорий "Снюс" или "Одноразки"
**Проблема:** `Attempt to invoke... on a null object reference`

**Возможные причины:**
1. Не инициализировано поле `flavor` или `specification`
2. Null pointer при обращении к списку брендов
3. Проблема с `orderIndex` при добавлении

**Решение:**
- Проверить `AcceptViewModel.addNewProduct()` - убедиться, что все поля инициализированы
- Добавить null-safety проверки
- Проверить `repository.insertProduct()` - метод уже вычисляет `maxOrder`, но нужно убедиться, что он работает для всех категорий

**Файл:** `app/src/main/java/com/example/vapestoreapp/viewmodel/AcceptViewModel.kt`
- Строки 211-297: метод `addNewProduct()`
- Убедиться, что для категорий "snus" и "disposable" `flavor` не null

#### 2.2. Оптимизация интерфейса
**Требование:** Устранить "провисания" интерфейса

**Решение:**
- Вынести тяжелые операции в фоновые потоки (уже используется `viewModelScope.launch`)
- Использовать `Dispatchers.IO` для работы с БД (уже используется)
- Оптимизировать запросы к БД (использовать индексы, если нужно)

---

## 📦 Требования к сдаче работы

### 1. Файл APK
- Подписанный APK (debug или release) готовый к установке поверх старой версии
- Инструкция по установке

### 2. Исходный код
- Ссылка на репозиторий или архив с кодом
- Все новые файлы должны быть включены

### 3. Инструкция по обновлению (Migration Guide)

**Документ:** `MIGRATION_GUIDE.md` (создать)

**Содержание:**
1. **Экспорт данных перед обновлением:**
   ```
   1. Откройте приложение
   2. Перейдите в Настройки → Резервное копирование
   3. Нажмите "Экспортировать данные"
   4. Сохраните файл в безопасное место (Downloads)
   5. Запомните путь к файлу
   ```

2. **Установка новой версии:**
   ```
   1. Установите новый APK поверх старой версии
   2. Если возникла ошибка "Signature mismatch":
      - Это означает, что APK подписан другим ключом
      - Нужно либо использовать тот же keystore, либо
      - Удалить старое приложение (ДАННЫЕ БУДУТ ПОТЕРЯНЫ!)
      - Установить новое приложение
      - Импортировать данные из резервной копии
   ```

3. **Импорт данных после обновления:**
   ```
   1. Откройте приложение
   2. Перейдите в Настройки → Резервное копирование
   3. Нажмите "Импортировать данные"
   4. Выберите сохраненный JSON файл
   5. Дождитесь завершения импорта
   6. Проверьте, что данные восстановлены
   ```

4. **Проверка данных:**
   - Проверить количество товаров
   - Проверить историю продаж
   - Проверить статистику

---

## ❓ Вопросы для разработчика (задать перед началом работы)

### 1. Keystore для подписи
**Вопрос:** Есть ли у вас доступ к `debug.keystore`, которым подписывалась текущая версия приложения (`app-debug.apk`)?

**Если НЕТ:**
- Нужно срочно реализовать механизм экспорта/импорта БД (см. раздел "Сохранение данных")
- Либо запросить у заказчика его `debug.keystore` файл

**Если ДА:**
- Использовать тот же keystore для подписи новой версии
- Обновление пройдет без проблем

### 2. Формула расчета прибыли
**Вопрос:** Как сейчас реализован подсчет прибыли? (Нужно показать формулу, чтобы заказчик подтвердил её правильность)

**Ответ:** Формула найдена в коде:
```kotlin
profit = (retailPrice - purchasePrice) * quantity - discount
```

**Проверка:** Заказчик должен подтвердить, что эта формула правильная.

### 3. Механизм резервного копирования
**Вопрос:** Предпочтительнее реализовать автоматический экспорт при первом запуске новой версии или ручной экспорт перед обновлением?

**Рекомендация:** Реализовать оба варианта:
- Ручной экспорт (перед обновлением)
- Автоматическая проверка при первом запуске новой версии (если версия БД изменилась, предложить импорт)

---

## 📝 Чек-лист выполнения

- [ ] Создан `DatabaseExporter.kt` с функцией экспорта в JSON
- [ ] Создан `DatabaseImporter.kt` с функцией импорта из JSON
- [ ] Добавлен UI для экспорта/импорта в настройках
- [ ] Создана миграция `MIGRATION_2_3` для новых таблиц
- [ ] Создана сущность `Debt` и `DebtDao`
- [ ] Создана сущность `Reservation` и `ReservationDao`
- [ ] Реализован экран "Долги" с функционалом продажи долга
- [ ] Реализован экран "Резервы" с функционалом продажи резерва
- [ ] Обновлен `StockFormatter` для отображения резервов
- [ ] Добавлено отображение общей выручки и прибыли в разделе продаж
- [ ] Добавлена разбивка по методам оплаты (карта/наличные)
- [ ] Добавлена статистика за день с разбивкой по оплате
- [ ] Добавлен поиск в разделе "Товары"
- [ ] Добавлена функция массового изменения цены закупки для бренда
- [ ] Исправлено копирование прайса (убран "📊 ИТОГО")
- [ ] Добавлена возможность редактирования даты/времени продажи
- [ ] Реализована поддержка темной/светлой темы
- [ ] Исправлен баг с добавлением товаров категорий "Снюс"/"Одноразки"
- [ ] Оптимизирован интерфейс (устранены провисания)
- [ ] Создан документ `MIGRATION_GUIDE.md`
- [ ] Собран подписанный APK
- [ ] Протестировано обновление с сохранением данных

---

## 🔍 Дополнительные заметки

### Архитектура проекта
- **База данных:** Room (версия 2, будет 3)
- **UI:** Jetpack Compose
- **Архитектура:** MVVM
- **Асинхронность:** Kotlin Coroutines + Flow
- **Навигация:** Compose Navigation

### Структура пакетов
```
com.example.vapestoreapp/
├── data/
│   ├── AppDatabase.kt
│   ├── Product.kt
│   ├── Sale.kt
│   ├── Debt.kt (новый)
│   ├── Reservation.kt (новый)
│   ├── ProductDao.kt
│   ├── SaleDao.kt
│   ├── DebtDao.kt (новый)
│   ├── ReservationDao.kt (новый)
│   └── Repository.kt
├── viewmodel/
│   ├── MainViewModel.kt
│   ├── CabinetViewModel.kt
│   ├── SalesManagementViewModel.kt
│   ├── SellViewModel.kt
│   ├── AcceptViewModel.kt
│   ├── DebtsViewModel.kt (новый)
│   └── ReservationsViewModel.kt (новый)
├── utils/
│   ├── DatabaseExporter.kt (новый)
│   ├── DatabaseImporter.kt (новый)
│   ├── StockFormatter.kt
│   └── ExcelImporter.kt
└── ui/
    └── theme/
        └── Theme.kt
```

---

**Дата создания ТЗ:** 2026-03-06  
**Версия ТЗ:** 1.0
