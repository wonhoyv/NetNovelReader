package com.netnovelreader.data.database

/**
 * Created by yangbo on 18-1-14.
 */
class SearchSQLManager{

    fun queryAll(): ArrayList<Array<String?>> {
        synchronized(SQLHelper) {
            val arraylist = ArrayList<Array<String?>>()
            val cursor = SQLHelper.getDB().rawQuery("select * from ${SQLHelper.TABLE_SEARCH};", null)
            while (cursor.moveToNext()) {
                arraylist.add(Array<String?>(8) { it -> cursor.getString(it + 2) })
            }
            cursor.close()
            return arraylist
        }
    }
}