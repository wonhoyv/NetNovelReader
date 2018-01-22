package com.netnovelreader.data.database

import android.content.ContentValues
import android.database.Cursor

/**
 * Created by yangbo on 18-1-12.
 */
class ShelfSQLManager{

    fun queryBookList(): Cursor?{
        synchronized(SQLHelper) {
            return SQLHelper.getDB().rawQuery("select * from ${SQLHelper.TABLE_SHELF};", null)
        }
    }

    fun addBookToShelf(bookname: String, url: String): Int{
        synchronized(SQLHelper) {
            var id = 0
            val cursor = SQLHelper.getDB().rawQuery("select ${SQLHelper.ID} from ${SQLHelper.TABLE_SHELF} where ${SQLHelper.BOOKNAME}='$bookname';", null)
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0)
            } else {
                val contentValue = ContentValues()
                contentValue.put(SQLHelper.BOOKNAME, bookname)
                contentValue.put(SQLHelper.DOWNLOADURL, url)
                id = SQLHelper.getDB().insert(SQLHelper.TABLE_SHELF, null, contentValue).toInt()
            }
            cursor.close()
            return id
        }
    }

    fun removeBookFromShelf(bookname: String): Int {
        synchronized(SQLHelper) {
            val cursor = SQLHelper.getDB().rawQuery("select ${SQLHelper.ID} from ${SQLHelper.TABLE_SHELF} where ${SQLHelper.BOOKNAME}='$bookname';", null)
            var id = 0
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0)
            }
            cursor.close()
            SQLHelper.getDB().execSQL("delete from ${SQLHelper.TABLE_SHELF} where ${SQLHelper.ID}=${SQLHelper.ID};")
            return id
        }
    }

    fun getRecord(bookname: String): Array<String>{
        synchronized(SQLHelper) {
            val result = Array<String>(2) { "" }
            val cursor = SQLHelper.getDB().rawQuery("select ${SQLHelper.ID},${SQLHelper.READRECORD} from ${SQLHelper.TABLE_SHELF} where ${SQLHelper.BOOKNAME}='$bookname';", null)
            if (cursor.moveToFirst()) {
                result[0] = cursor.getString(0) ?: ""
                result[1] = cursor.getString(1) ?: ""
            }
            cursor.close()
            return result
        }
    }

    fun setRecord(bookname: String, record: String){
        synchronized(SQLHelper) {
            SQLHelper.getDB().execSQL("update ${SQLHelper.TABLE_SHELF} set ${SQLHelper.READRECORD}='$record' where ${SQLHelper.BOOKNAME}='$bookname';");
        }
    }
}