package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.MyListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MyListDao {
    @Query("SELECT * FROM my_list ORDER BY savedTimestamp DESC")
    fun getAllMyList(): Flow<List<MyListEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM my_list WHERE movieId = :movieId)")
    fun isInMyList(movieId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM my_list WHERE movieId = :movieId)")
    suspend fun isInMyListSync(movieId: String): Boolean

    @Query("SELECT * FROM my_list WHERE movieId = :movieId LIMIT 1")
    suspend fun getMyListItem(movieId: String): MyListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToMyList(item: MyListEntity)

    @Query("DELETE FROM my_list WHERE movieId = :movieId")
    suspend fun removeFromMyList(movieId: String)

    @Query("DELETE FROM my_list")
    suspend fun clearAll()
}
