package com.example.vapestoreapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentCardDao {
    @Query("SELECT * FROM payment_cards WHERE isArchived = 0 ORDER BY label COLLATE NOCASE")
    fun getActiveCards(): Flow<List<PaymentCard>>

    @Query("SELECT * FROM payment_cards ORDER BY isArchived ASC, label COLLATE NOCASE")
    fun getAllCards(): Flow<List<PaymentCard>>

    @Query("SELECT * FROM payment_cards WHERE id = :id")
    suspend fun getById(id: Int): PaymentCard?

    @Query("SELECT * FROM payment_cards WHERE lower(label) = lower(:label) LIMIT 1")
    suspend fun getByLabel(label: String): PaymentCard?

    @Insert
    suspend fun insert(card: PaymentCard): Long

    @Update
    suspend fun update(card: PaymentCard)

    @Query("DELETE FROM payment_cards")
    suspend fun deleteAll()
}

