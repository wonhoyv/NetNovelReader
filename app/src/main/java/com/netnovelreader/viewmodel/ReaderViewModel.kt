package com.netnovelreader.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.databinding.ObservableArrayList
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import com.netnovelreader.bean.ReaderBean
import com.netnovelreader.common.NotDeleteNum
import com.netnovelreader.common.getSavePath
import com.netnovelreader.common.id2TableName
import com.netnovelreader.data.db.ReaderDbManager
import com.netnovelreader.data.network.ChapterCache
import com.netnovelreader.data.network.DownloadCatalog
import com.netnovelreader.interfaces.IReaderContract
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.io.File

/**
 * Created by yangbo on 18-1-13.
 */

class ReaderViewModel(context: Application) : AndroidViewModel(context), IReaderContract.IReaderViewModel {

    enum class CHAPTERCHANGE {
        NEXT,                       //下一章
        PREVIOUS,                   //上一章
        BY_CATALOG                  //通过目录翻页
    }

    val catalog by lazy {
        MutableLiveData<ObservableArrayList<ReaderBean>>().run { value = ObservableArrayList(); value!! }
    }
    /**
     * 一页显示的内容
     */
    var text: ObservableField<String> = ObservableField("")

    var isLoading = ObservableBoolean(false)

    @Volatile
    var dirName: String? = null

    @Volatile
    var chapterName: String? = null
    /**
     * 章节数,最大章节数
     */
    @Volatile
    var chapterNum = 1
    @Volatile
    var maxChapterNum = 0

    private var tableName = ""
    lateinit var chapterCache: ChapterCache

    lateinit private var bookName: String
    private var CACHE_NUM: Int = 0
    /**
     * readerView第一次绘制时执行, 返还阅读记录页数
     */
    override suspend fun initData(bookName: String, CACHE_NUM: Int): Int = async {
        this@ReaderViewModel.bookName = bookName
        this@ReaderViewModel.CACHE_NUM = CACHE_NUM
        tableName = id2TableName(ReaderDbManager.getBookId(bookName))
        maxChapterNum = ReaderDbManager.getChapterCount(tableName).takeIf { it != 0 } ?: return@async 0
        val record = getRecord()
        chapterNum = record[0]
        dirName = id2TableName(record[2])
        chapterCache = ChapterCache(CACHE_NUM, tableName).apply { init(maxChapterNum, dirName!!) }
        return@async record[1]
    }.await()

    //获取章节内容
    override suspend fun getChapter(type: CHAPTERCHANGE, chapterName: String?) {
        isLoading.set(true)
        when (type) {
            CHAPTERCHANGE.NEXT -> if (chapterNum >= maxChapterNum) return else chapterNum++
            CHAPTERCHANGE.PREVIOUS -> if (chapterNum < 2) return else chapterNum--
            CHAPTERCHANGE.BY_CATALOG -> chapterName?.run {
                chapterNum = ReaderDbManager.getChapterId(tableName, chapterName)
            }
        }
        launch { if (chapterNum == maxChapterNum) updateCatalog() }
        val str = chapterCache.getChapter(chapterNum, false)
        text.set(str)
        this@ReaderViewModel.chapterName = str.substring(0, str.indexOf("|"))
        if (str.substring(str.indexOf("|") + 1) == ChapterCache.FILENOTFOUND) {
            downloadAndShow()
        } else {
            isLoading.set(false)
        }
    }

    //下载并显示，阅读到未下载章节时调用
    override suspend fun downloadAndShow() {
        var str = ChapterCache.FILENOTFOUND
        var times = 0
        while (str == ChapterCache.FILENOTFOUND && times++ < 10) {
            str = chapterCache.getChapter(chapterNum)
            delay(500)
        }
        if (str != ChapterCache.FILENOTFOUND && str.isNotEmpty()) {
            isLoading.set(false)
            getChapter(ReaderViewModel.CHAPTERCHANGE.BY_CATALOG, null)
        }
    }

    /**
     * 保存阅读记录
     */
    @Synchronized
    override suspend fun setRecord(chapterNum: Int, pageNum: Int) {
        if (chapterNum < 1) return
        ReaderDbManager.setRecord(bookName, "$chapterNum#${if (pageNum < 1) 1 else pageNum}")
    }

    /**
     * 重新读取目录
     */
    @Synchronized
    override suspend fun getCatalog() {
        catalog.clear()
        catalog.addAll(ReaderDbManager.getAllChapter(tableName).map { ReaderBean(it) })
    }

    /**
     * 自动删除已读章节，但保留最近[NotDeleteNum]章
     */
    override suspend fun autoRemove() {
        val num = getRecord()[0]
        if (num < NotDeleteNum) return
        val id = num - NotDeleteNum
        ReaderDbManager.setReaded(tableName, id)
                .forEach { File("${getSavePath()}/$tableName/$it").delete() }
    }

    /**
     * 获取阅读记录
     */
    private suspend fun getRecord(): Array<Int> {
        val queryResult = ReaderDbManager.getRecord(bookName) //阅读记录 3#2 表示第3章第2页
        val array = queryResult[1]
                .let { if (it.length < 1) "1#1" else it }
                .split("#")
        return arrayOf(array[0].toInt(), array[1].toInt(), queryResult[0].toInt())
    }

    private fun updateCatalog() {
        DownloadCatalog(tableName, ReaderDbManager.getCatalogUrl(bookName)).download()
        maxChapterNum = ReaderDbManager.getChapterCount(tableName)
    }
}