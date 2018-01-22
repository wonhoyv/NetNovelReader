package com.netnovelreader.data.database

/**
 * Created by yangbo on 18-1-12.
 */
class ChapterSQLManager{
    fun createTable(tableName : String): ChapterSQLManager {
        synchronized(SQLHelper){
            SQLHelper.getDB().execSQL("create table if not exists $tableName (${SQLHelper.ID} " +
                    "integer primary key,${SQLHelper.CHAPTERNAME} varchar(128), " +
                    "${SQLHelper.CHAPTERURL} indicator, ${SQLHelper.ISDOWNLOADED} var char(128));")
            return this
        }
    }


    fun dropTable(tableName: String){
        synchronized(SQLHelper) {
            SQLHelper.getDB().execSQL("drop table if exists $tableName;")
        }
    }

    @Synchronized
    fun setChapterFinish(tableName: String, chaptername: String, url: String, isDownloadSuccess: Boolean){
        synchronized(SQLHelper) {
            val cursor = SQLHelper.getDB().rawQuery("select * from $tableName where " +
                    "${SQLHelper.CHAPTERNAME}='$chaptername';", null)
            if (!cursor.moveToNext()) {
                SQLHelper.getDB().execSQL("insert into $tableName (${SQLHelper.CHAPTERNAME}, " +
                        "${SQLHelper.CHAPTERURL}, ${SQLHelper.ISDOWNLOADED}) values ('$chaptername'," +
                        "'$url','${compareValues(isDownloadSuccess, false)}')")
            } else {
                SQLHelper.getDB().execSQL("update $tableName set ${SQLHelper.ISDOWNLOADED}=" +
                        "'${compareValues(isDownloadSuccess, false)}' " +
                        "where ${SQLHelper.CHAPTERNAME}='$chaptername';")
            }
            cursor.close()
        }
    }

    /**
     * @isDownloaded  0表示未下载,1表示已下载
     */
    @Synchronized
    fun getDownloadedOrNot(tableName: String, isDownloaded: Int): LinkedHashMap<String,String>{
        synchronized(SQLHelper) {
            val map = LinkedHashMap<String, String>()
            val cursor = SQLHelper.getDB().rawQuery("select ${SQLHelper.CHAPTERNAME}," +
                    "${SQLHelper.CHAPTERURL} from $tableName where ${SQLHelper.ISDOWNLOADED}=" +
                    "'$isDownloaded';", null)
            while (cursor.moveToNext()) {
                map.put(cursor.getString(0), cursor.getString(1))
            }
            cursor.close()
            return map
        }
    }

    @Synchronized
    fun getAllChapter(tableName: String): ArrayList<String>{
        synchronized(SQLHelper) {
            val arrayList = ArrayList<String>()
            val cursor = SQLHelper.getDB().rawQuery("select ${SQLHelper.CHAPTERNAME} from $tableName;", null)
            while (cursor.moveToNext()) {
                arrayList.add(cursor.getString(0))
            }
            cursor.close()
            return arrayList
        }
    }

    fun getChapterName(tableName: String, id: Int): String{
        synchronized(SQLHelper) {
            var chapterName: String? = null
            val cursor = SQLHelper.getDB().rawQuery("select ${SQLHelper.CHAPTERNAME} from " +
                    "$tableName where ${SQLHelper.ID}=$id;", null)
            if (cursor.moveToFirst()) {
                chapterName = cursor.getString(0)
            }
            cursor.close()
            return chapterName ?: ""
        }
    }

    fun getChapterId(tableName: String, chapterName: String): Int{
        synchronized(SQLHelper) {
            var id = 1
            val cursor = SQLHelper.getDB().rawQuery("select ${SQLHelper.ID} from $tableName where " +
                    "${SQLHelper.CHAPTERNAME}='$chapterName';", null)
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0)
            }
            cursor.close()
            return id
        }
    }

    fun getChapterCount(tableName: String): Int{
        synchronized(SQLHelper) {
            var c = 1
            val cursor = SQLHelper.getDB().rawQuery("select count(*) from $tableName;", null)
            if (cursor.moveToFirst()) {
                c = cursor.getInt(0)
            }
            cursor.close()
            return c
        }
    }
}