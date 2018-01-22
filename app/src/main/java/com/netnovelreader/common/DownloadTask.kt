package com.netnovelreader.common

import com.netnovelreader.data.database.SQLHelper
import com.netnovelreader.data.database.ChapterSQLManager
import com.netnovelreader.data.network.ParseHtml
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.collections.ArrayList

/**
 * Created by yangbo on 2018/1/16.
 */
class DownloadTask(val tableName: String, val url: String) {
    var chapterName: String? = null

    @Throws(IOException::class)
    fun getRunnables(): ArrayList<DownloadChapterRunnable> {
        val dir = mkdirs(getSavePath() + "/$tableName")
        var runnableDownloads: ArrayList<DownloadChapterRunnable>? = null
        //TODO chapterName != null
        if (chapterName == null) {
            updateSql()
            runnableDownloads = getUnDownloadFromSql(dir, tableName)
        } else {

        }
        return runnableDownloads ?: ArrayList()
    }

    @Throws(IOException::class)
    fun updateSql() {
        val map = ParseHtml().getCatalog(url)
        val sqlManager = ChapterSQLManager()
        sqlManager.createTable(tableName)
        val chapterInSql = sqlManager.getAllChapter(tableName)
        val iterator = map.iterator()
        var entry: MutableMap.MutableEntry<String, String>? = null
        while (iterator.hasNext()) {
            entry = iterator.next()
            if (!chapterInSql.contains(entry.key)) {
                sqlManager.setChapterFinish(tableName, entry.key, entry.value, false)
            }
        }
        if (entry != null) {
            synchronized(SQLHelper) {
                SQLHelper.getDB().execSQL("update ${SQLHelper.TABLE_SHELF} set " +
                        "${SQLHelper.LATESTCHAPTER}='${entry.key}' where " +
                        "${SQLHelper.ID}=${tableName.replace("BOOK", "")}")
            }
        }
    }

    @Throws(IOException::class)
    fun getUnDownloadFromSql(saveDir: String, tableName: String): ArrayList<DownloadChapterRunnable> {
        val sqlManager = ChapterSQLManager()
        val map = sqlManager.getDownloadedOrNot(tableName, 0)
        val runnables = ArrayList<DownloadChapterRunnable>()
        val iterator = map.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            runnables.add(DownloadChapterRunnable(tableName, saveDir, entry.key, entry.value))
        }
        return runnables
    }



    /**
     * 下载保存章节具体执行者，实现runnable接口，线程池执行
     */
    class DownloadChapterRunnable(val tablename: String, val dir: String, val chapterName: String,
                                  val chapterUrl: String) : Runnable {
        lateinit var eON: () -> Unit?
        @Throws(IOException::class)
        override fun run() {
            var fos: FileWriter? = null
            var dbm = ChapterSQLManager()
            try {
                fos = FileWriter(File(dir, chapterName))
                fos.write(ParseHtml().getChapter(chapterUrl))
                fos.flush()
                dbm.setChapterFinish(tablename, chapterName, chapterUrl, true)
            } catch (e: Exception) {
                dbm.setChapterFinish(tablename, chapterName, chapterUrl, false)
            } finally {
                fos?.close()
                eON()
            }
        }

        fun setFun(eON: () -> Unit): Runnable {
            this.eON = eON
            return this
        }
    }
}