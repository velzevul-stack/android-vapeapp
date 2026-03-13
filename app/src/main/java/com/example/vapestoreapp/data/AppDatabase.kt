package com.example.vapestoreapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Product::class, Sale::class, Debt::class, Reservation::class, PaymentCard::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun debtDao(): DebtDao
    abstract fun reservationDao(): ReservationDao
    abstract fun paymentCardDao(): PaymentCardDao

    companion object {
        const val DATABASE_NAME = "vape_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            val appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .enableMultiInstanceInvalidation()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS debts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        customerName TEXT NOT NULL,
                        products TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        totalAmount REAL NOT NULL,
                        isPaid INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        customerName TEXT NOT NULL,
                        productId INTEGER NOT NULL,
                        quantity INTEGER NOT NULL,
                        reservationDate INTEGER NOT NULL,
                        expirationDate INTEGER NOT NULL,
                        isSold INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем колонку orderIndex с значением по умолчанию 0
                database.execSQL(
                    "ALTER TABLE products ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0"
                )

                // Устанавливаем порядок для существующих записей
                database.execSQL(
                    """
                    UPDATE products 
                    SET orderIndex = (
                        SELECT row_number FROM (
                            SELECT id, ROW_NUMBER() OVER (
                                PARTITION BY category 
                                ORDER BY brand, flavor
                            ) as row_number
                            FROM products
                        ) sub
                        WHERE sub.id = products.id
                    )
                    """
                )
            }
        }

        // Миграция для добавления sourceType и sourceId в таблицу sales
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Проверяем, существуют ли колонки перед добавлением
                val cursor = database.query("PRAGMA table_info(sales)")
                val columns = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        val columnName = cursor.getString(nameIndex)
                        columns.add(columnName)
                    }
                }
                cursor.close()

                // Добавляем колонки sourceType и sourceId, если их еще нет
                // Обе колонки nullable, поэтому существующие записи получат NULL
                if (!columns.contains("sourceType")) {
                    database.execSQL("ALTER TABLE sales ADD COLUMN sourceType TEXT")
                }
                if (!columns.contains("sourceId")) {
                    database.execSQL("ALTER TABLE sales ADD COLUMN sourceId INTEGER")
                }
            }
        }

        // Миграция для раздельной оплаты (cashAmount/cardAmount) в sales
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val cursor = database.query("PRAGMA table_info(sales)")
                val columns = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        val columnName = cursor.getString(nameIndex)
                        columns.add(columnName)
                    }
                }
                cursor.close()

                if (!columns.contains("cashAmount")) {
                    database.execSQL("ALTER TABLE sales ADD COLUMN cashAmount REAL")
                }
                if (!columns.contains("cardAmount")) {
                    database.execSQL("ALTER TABLE sales ADD COLUMN cardAmount REAL")
                }
            }
        }

        // Миграция для долгов: отметка, что склад уже списан при создании
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val cursor = database.query("PRAGMA table_info(debts)")
                val columns = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        columns.add(cursor.getString(nameIndex))
                    }
                }
                cursor.close()

                if (!columns.contains("stockDeducted")) {
                    database.execSQL("ALTER TABLE debts ADD COLUMN stockDeducted INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        // Миграция для карт безнала + привязки продаж к карте
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS payment_cards (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                val cursor = database.query("PRAGMA table_info(sales)")
                val columns = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        columns.add(cursor.getString(nameIndex))
                    }
                }
                cursor.close()

                if (!columns.contains("cardId")) {
                    database.execSQL("ALTER TABLE sales ADD COLUMN cardId INTEGER")
                }
            }
        }
    }
}