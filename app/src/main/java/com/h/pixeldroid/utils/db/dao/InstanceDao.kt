package com.h.pixeldroid.utils.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity

@Dao
interface InstanceDao {
    @Query("SELECT * FROM instances")
    fun getAll(): List<InstanceDatabaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInstance(instance: InstanceDatabaseEntity)
}