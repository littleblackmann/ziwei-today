package com.zwds.today.data.repository

import androidx.room.withTransaction
import com.zwds.today.data.AppDatabase
import com.zwds.today.data.dao.FamilyMemberDao
import com.zwds.today.data.entity.FamilyMemberEntity
import com.zwds.today.domain.model.Gender
import com.zwds.today.domain.model.SolarBirth
import kotlinx.coroutines.flow.Flow

/**
 * 家人資料層
 *
 * 對 UI 與 UseCase 暴露 Domain 物件（SolarBirth / Gender），不暴露 Entity。
 */
class FamilyRepository(
    private val database: AppDatabase,
    private val dao: FamilyMemberDao
) {

    fun observeAll(): Flow<List<FamilyMemberEntity>> = dao.observeAll()

    suspend fun getAll(): List<FamilyMemberEntity> = dao.getAll()

    suspend fun getById(id: Long): FamilyMemberEntity? = dao.getById(id)

    suspend fun getPrimary(): FamilyMemberEntity? = dao.getPrimary()

    suspend fun count(): Int = dao.count()

    suspend fun insert(member: FamilyMemberEntity): Long {
        // 第一筆自動標為主帳號
        return database.withTransaction {
            val isFirst = dao.count() == 0
            val toInsert = if (isFirst) member.copy(isPrimary = true) else member
            val newId = dao.insert(toInsert)
            // 若這筆要設為主，把其他人取消
            if (toInsert.isPrimary) {
                dao.clearPrimary()
                dao.markPrimary(newId)
            }
            newId
        }
    }

    suspend fun update(member: FamilyMemberEntity) {
        database.withTransaction {
            dao.update(member)
            if (member.isPrimary) {
                dao.clearPrimary()
                dao.markPrimary(member.id)
            }
        }
    }

    suspend fun delete(member: FamilyMemberEntity) {
        database.withTransaction {
            dao.delete(member)
            // 如果刪掉的是主帳號，把第一筆自動補上
            if (member.isPrimary) {
                val all = dao.getAll()
                all.firstOrNull()?.let { dao.markPrimary(it.id) }
            }
        }
    }

    suspend fun setPrimary(id: Long) {
        database.withTransaction {
            dao.clearPrimary()
            dao.markPrimary(id)
        }
    }
}

/** 把 entity 轉成演算法輸入 */
fun FamilyMemberEntity.toSolarBirth(): SolarBirth =
    SolarBirth(solarYear, solarMonth, solarDay, hour24)

fun FamilyMemberEntity.gender(): Gender =
    if (gender.equals("FEMALE", ignoreCase = true)) Gender.FEMALE else Gender.MALE
