package com.zwds.today.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zwds.today.data.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {

    @Query("SELECT * FROM family_members ORDER BY is_primary DESC, created_at ASC")
    fun observeAll(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members ORDER BY is_primary DESC, created_at ASC")
    suspend fun getAll(): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE is_primary = 1 LIMIT 1")
    suspend fun getPrimary(): FamilyMemberEntity?

    @Query("SELECT COUNT(*) FROM family_members")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(member: FamilyMemberEntity): Long

    @Update
    suspend fun update(member: FamilyMemberEntity)

    @Delete
    suspend fun delete(member: FamilyMemberEntity)

    /** 把指定家人設為主帳號，其他人取消。整個交易性。*/
    @Query("UPDATE family_members SET is_primary = 0")
    suspend fun clearPrimary()

    @Query("UPDATE family_members SET is_primary = 1 WHERE id = :id")
    suspend fun markPrimary(id: Long)
}
