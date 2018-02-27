package com.netnovelreader.data.db

import android.arch.persistence.room.*

@Dao
interface ShelfDao {

    @Query("SELECT * FROM shelf order by latest_read DESC")
    fun getAll(): List<ShelfBean>?

    @Query("SELECT * FROM shelf WHERE book_name LIKE :bookname")
    fun getBookInfo(bookname: String): ShelfBean?

    @Query("SELECT max(latest_read) from shelf")
    fun getLatestReaded(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg beans: ShelfBean)

    @Delete
    fun delete(bean: ShelfBean)
}