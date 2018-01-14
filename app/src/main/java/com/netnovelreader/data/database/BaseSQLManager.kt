package com.netnovelreader.data.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.netnovelreader.utils.getSavePath
import java.io.File

/**
 * Created by yangbo on 17-12-24.
 */
open class BaseSQLManager {
    var db: SQLiteDatabase? = null
    val filepath = "${getSavePath()}/databases/mynovelreader.db"
    constructor() {
        var file = File(filepath).parentFile
        if(!file.exists()){
            file.mkdirs()
        }
        db ?: run {db = SQLiteDatabase.openOrCreateDatabase(File(filepath), null)}
    }
    open fun getDataBase(): SQLiteDatabase?{
        return db
    }

    open fun execSQL(sql: String){
        getDataBase()?.execSQL(sql)
    }

    open fun rawQuery(sql: String) : Cursor? = getDataBase()?.rawQuery(sql, null)

    open fun dropTable(tableName : String){

    }

    open fun closeDB(){
        db?.close()
    }

    companion object {
        val ID = "_id"

        val TABLE_SHELF = "shelf"
        //如qidian.com
        val HOSTNAME = "hostname"
        //目录网址解析规则
        val CATALOG_RULE = "catalog_rule"
        //章节网址解析规则
        val CHAPTER_RULE = "chapter_rule"
        //目录网址封面解析规则
        val COVER_RULE = "cover_rule"

        val TABLE_PARSERULES = "parserules"
        //书名
        val BOOKNAME = "bookname"
        //最新章节
        val LATESTCHAPTER = "latestChapter"
        //阅读记录
        val READRECORD = "readRecord"
        //编码
        val CHARSET = "charset"
        //书对应的表名
        val BOOKTABLE = "booktable"
        //章节存放文件夹
        val BOOKDIR = "bookdir"
        //来源网址
        val DOWNLOADURL = "downloadurl"

        val TABLE_SEARCH = "search"
        val SEARCH_HOSTNAME = HOSTNAME
        val SEARCHURL = "search_url"
        val ISREDIRECT = "is_redirect"
        val REDIRECTFILELD = "redirect_fileld"
        val REDIRECTSELECTOR = "redirect_selector"
        val NOREDIRECTSELECTOR = "no_redirect_selector"
        val SEARCHCHARSET = CHARSET

        //章节名
        val CHAPTERNAME = "chaptername"
        //章节来源网址
        val CHAPTERURL = "chapterurl"
    }
}