package com.netnovelreader.reader

import com.netnovelreader.common.getSavePath
import com.netnovelreader.data.database.ChapterSQLManager
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*

/**
 * Created by yangbo on 18-1-15.
 */
class ChapterCache(val cacheNum: Int) {
    /**
     * @cacheNum 向后缓存章节数
     */

    /**
     * hashTablb<第几章，章节内容>
     */
    val chapterTxtTable = Hashtable<Int, String>()
    /**
     * 最大章节数
     */
    var maxChapterNum = 0
    var dirName: String? = null
    fun prepare(chapterNum: Int, maxChapterNum: Int, dirName: String){
        this.maxChapterNum = maxChapterNum
        this.dirName = dirName
        Thread{ getChapter(chapterNum) }.start()

    }

    fun getChapter(chapterNum: Int): String {
        var result: String?
        if(chapterTxtTable.containsKey(chapterNum)){
            result =  chapterTxtTable.get(chapterNum)
        }else {
            try {
                result = getText(chapterNum, true)
                synchronized(this){
                    chapterTxtTable.put(chapterNum, result)
                }
                Thread{ readToCache(chapterNum) }.start()
            }catch (e: IOException){
                result = ""
            }
        }
        return result ?: ""
    }
    /**
     * 获取小说章节内容
     * @chapterNum:Int 章节数
     * @isCurrentChapter 是否是将要阅读的章节，如果不是，从网络下载，如果是，让主线程进行处理
     */
    @Throws(IOException::class)
    fun getText(chapterNum: Int, isCurrentChapter: Boolean): String {
        val sb = StringBuilder()
        val chapterName = ChapterSQLManager().getChapterName(dirName!!, chapterNum)
        sb.append(chapterName + "|")
        val chapterPath = "${getSavePath()}/$dirName/$chapterName"
        if(!File(chapterPath).exists()){
            if(!isCurrentChapter){
                sb.append(getFromNet())
            }
        }else{
            sb.append(getFromFile(chapterPath))
        }
        return sb.toString()
    }

    @Throws(IOException::class)
    fun getFromFile(chapterPath: String): String {
        val sb = StringBuilder()
        val fr = FileReader(chapterPath)
        fr.forEachLine { sb.append(it + "\n") }
        fr.close()
        return sb.toString()
    }

    @Throws(IOException::class)
    fun getFromNet(): String {
        //TODO nnnnnnnnnnnnnnnn
        return ""
    }

    fun readToCache(chapterNum: Int){
        val iterator = chapterTxtTable.iterator()
        val arrayList = ArrayList<Int>(0)
        while (iterator.hasNext()){
            val entry = iterator.next()
            if(entry.key + 1 < chapterNum || entry.key - cacheNum > chapterNum){
                arrayList.add((entry.key))
            }
        }
        arrayList.forEach {
            chapterTxtTable.remove(it)
        }
        if(chapterNum > 1){
            if(!chapterTxtTable.contains(chapterNum - 1)){
                chapterTxtTable.put(chapterNum, getText(chapterNum, false))
            }
        }
        for(i in 1..cacheNum){
            if(chapterNum + i <= maxChapterNum && !chapterTxtTable.contains(chapterNum + i)){
                chapterTxtTable.put(chapterNum, getText(chapterNum, false))
            }
        }
    }
}