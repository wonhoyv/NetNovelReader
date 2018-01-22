package com.netnovelreader.data.database

/**
 * Created by yangbo on 18-1-12.
 */
class ParseSQLManager{

    fun getChapterRule(hostname: String, field: String): String?{
        synchronized(SQLHelper) {
            var rule: String? = null
            var cursor = SQLHelper.getDB().rawQuery("select $field from ${SQLHelper.TABLE_PARSERULES} " +
                    "where ${SQLHelper.HOSTNAME}='$hostname';", null)
            if (cursor!!.moveToFirst()) {
                rule = cursor.getString(0)
            }
            cursor.close()
            return rule
        }
    }
}