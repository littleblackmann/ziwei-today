package com.zwds.today.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 家人檔案
 *
 * 儲存使用者輸入的家人資料。**這個表絕對不可破壞性重建**：
 * 長者不會願意重輸資料一次。任何 schema 變更都必須走真的 migration。
 *
 * 欄位設計：
 *  - 直接存國曆出生資料（year/month/day/hour24）
 *  - 加 [isLunarInput] 旗標只是「使用者當初輸入時用什麼曆法」的記錄，
 *    不影響演算法計算 — 演算法永遠收國曆，內部用 LunarCalendar 轉農曆
 *  - 國曆出生若使用者輸入時是農曆，我們在輸入頁就先轉成國曆再存
 *  - 不存性別 default 是「為自己第一筆建檔可推預設」，但仍以欄位儲存
 */
@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    @ColumnInfo(name = "name") val name: String,

    /** 國曆 */
    @ColumnInfo(name = "solar_year") val solarYear: Int,
    @ColumnInfo(name = "solar_month") val solarMonth: Int,
    @ColumnInfo(name = "solar_day") val solarDay: Int,
    /** 24 小時制（0..23）*/
    @ColumnInfo(name = "hour24") val hour24: Int,

    /** 使用者當初是用農曆輸入的（記錄用，UI 編輯時可預先切回農曆模式）*/
    @ColumnInfo(name = "is_lunar_input", defaultValue = "0") val isLunarInput: Boolean = false,

    /** 男 / 女（"MALE" / "FEMALE"）*/
    @ColumnInfo(name = "gender") val gender: String,

    /** 是否為主帳號（推播以這位為主）*/
    @ColumnInfo(name = "is_primary", defaultValue = "0") val isPrimary: Boolean = false,

    /** 建立時間（毫秒）*/
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
