package com.example.vapestoreapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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
