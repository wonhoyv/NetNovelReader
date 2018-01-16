package com.netnovelreader.service

import android.util.Log
import com.netnovelreader.data.database.ChapterSQLManager
import com.netnovelreader.data.network.ParseHtml
import com.netnovelreader.utils.getSavePath
import com.netnovelreader.utils.mkdirs
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

/**
 * Created by yangbo on 2018/1/16.
 */
class DownloadTask{
    var tableName: String? = null
    var url: String? = null
    var chapterName: String? = null
    constructor(tableName: String, url: String){
        this.tableName = tableName
        this.url = url
    }

    @Throws(SocketTimeoutException::class)
    fun getRunnables(): ArrayList<ChapterRunnable> {
        val dir = mkdirs(getSavePath() + "/$tableName")
        var runnables: ArrayList<ChapterRunnable>? = null
        if(chapterName == null){
            runnables = formCatalog(dir)
        }
        return runnables ?: ArrayList()
    }

    @Throws(SocketTimeoutException::class)
    fun formCatalog(dir: String): ArrayList<ChapterRunnable>{
        val sqlManager = ChapterSQLManager()
        val map = ParseHtml().getCatalog(url!!)
        var alreadyExists: ArrayList<String>? = null
        if(sqlManager.isTableExists(tableName!!)){
            alreadyExists = sqlManager.getDownloaded(tableName!!)
//            alreadyExists = ArrayList<String>()
        }else{
            sqlManager.createTable(tableName!!).addAllChapter(map, tableName!!)
        }
        var runnables = ArrayList<ChapterRunnable>()
        val iterator = map.iterator()
        while (iterator.hasNext()){
            val entry = iterator.next()
            if(alreadyExists == null || !alreadyExists.contains(entry.key)){
                runnables.add(ChapterRunnable(tableName!!, dir, entry.key, entry.value))
            }
        }
        sqlManager.closeDB()
        return runnables
    }


    class ChapterRunnable(val tablename: String, val dir: String, val chapterName: String, val chapterUrl:String) : Runnable{
        lateinit var eON: () -> Unit ?
        override fun run() {
            var fos: FileOutputStream? = null
            var dbm = ChapterSQLManager()
            try{
                Log.d("=====Service ondownload", "$dir==$chapterName")
                fos = FileOutputStream(File(dir, chapterName))
                fos.write(ParseHtml().getChapter(chapterUrl).toByteArray())
                dbm.setChapterFinish(tablename, chapterName, true, chapterUrl)
            }catch (e: Exception){
                dbm.setChapterFinish(tablename, chapterName, false, chapterUrl)
            }finally {
//                RxBus.post(1)
                dbm.closeDB()
                fos?.close()
                eON()
            }
        }

        fun setFun(eON: () -> Unit): Runnable{
            this.eON = eON
            return this
        }
    }
}