package com.example.vapestoreapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM reservations WHERE id = :id")
    suspend fun getById(id: Int): Reservation?

    @Query("SELECT * FROM reservations WHERE isSold = 0 AND expirationDate < :currentTime")
    suspend fun getExpiredReservations(currentTime: Long): List<Reservation>
}
