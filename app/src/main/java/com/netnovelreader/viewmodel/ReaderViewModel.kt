package com.netnovelreader.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.databinding.ObservableArrayList
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import com.netnovelreader.bean.ChapterChangeType
import com.netnovelreader.bean.ReaderBean
import com.netnovelreader.common.NotDeleteNum
import com.netnovelreader.common.getSavePath
import com.netnovelreader.common.replace
import com.netnovelreader.data.db.ReaderDbManager
import com.netnovelreader.data.db.ShelfBean
import com.netnovelreader.data.network.ChapterCache
import com.netnovelreader.data.network.DownloadCatalog
import com.netnovelreader.interfaces.IReaderContract
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by yangbo on 18-1-13.
 */

class ReaderViewModel(val context: Application) : AndroidViewModel(context),
        IReaderContract.IReaderViewModel {

    val catalog by lazy { ObservableArrayList<ReaderBean>() }      //目录
    val text: ObservableField<String> = ObservableField("")  //一页显示的内容
    val isHeadViewShow = ObservableBoolean(false)
    val isFontSettingShow = ObservableBoolean(false)
    val isBackgroundSetingShow = ObservableBoolean(false)
    val isFootViewShow = ObservableBoolean(false)
    var isLoading = ObservableBoolean(true)
    @Volatile
    var chapterName: String? = null
    @Volatile
    var chapterNum = AtomicInteger(1)              //章节数
    @Volatile
    var maxChapterNum = 0                                    //最大章节数
    var chapterCache: ChapterCache? = null

    lateinit var bookName: String
    var CACHE_NUM: Int = 0

    //获取章节内容
    override suspend fun getChapter(type: ChapterChangeType, chapterName: String?) {
        when (type) {
            ChapterChangeType.NEXT -> if (chapterNum.get() >= maxChapterNum) return else chapterNum.incrementAndGet()
            ChapterChangeType.PREVIOUS -> if (chapterNum.get() < 2) return else chapterNum.decrementAndGet()
            ChapterChangeType.BY_CATALOG -> chapterName?.run {
                ReaderDbManager.getChapterId(bookName, chapterName).also { chapterNum.set(it) }
            }
        }
        isLoading.set(true)
        launch { if (chapterNum.get() == maxChapterNum) updateCatalog() }
        val str = chapterCache!!.getChapter(chapterNum.get(), false)
        text.set(str)
        this.chapterName = str.substring(0, str.indexOf("|"))
        if (str.substring(str.indexOf("|") + 1) == ChapterCache.FILENOTFOUND) {
            downloadAndShow()
        } else {
            isLoading.set(false)
        }
    }

    //下载并显示，阅读到未下载章节时调用
    override suspend fun downloadAndShow() {
        chapterCache ?: return
        var str = ChapterCache.FILENOTFOUND
        var times = 0
        while (str == ChapterCache.FILENOTFOUND && times++ < 10) {
            str = chapterCache!!.getChapter(chapterNum.get())
            delay(500)
        }
        if (str != ChapterCache.FILENOTFOUND && str.isNotEmpty()) {
            isLoading.set(false)
            getChapter(ChapterChangeType.BY_CATALOG, null)
        }
    }

    override suspend fun reloadCurrentChapter() {
        chapterCache ?: return
        updateCatalog()
        getCatalog()
        chapterCache!!.clearCache()
        getChapter(ChapterChangeType.BY_CATALOG, null)
    }

    /**
     * 保存阅读记录
     */
    @Synchronized
    override suspend fun setRecord(pageNum: Int) {
        if (chapterNum.get() < 1) return
        ReaderDbManager.getRoomDB().shelfDao().replace(ShelfBean(bookName = bookName,
                 readRecord = "$chapterNum#${if (pageNum < 1) 1 else pageNum}"))
    }

    /**
     * 重新读取目录
     */
    @Synchronized
    override suspend fun getCatalog() {
        catalog.clear()
        catalog.addAll(ReaderDbManager.getAllChapter(bookName).map { ReaderBean(it) })
    }

    /**
     * 自动删除已读章节，但保留最近[NotDeleteNum]章
     */
    override suspend fun autoRemove() {
        val num = getRecord()[0]
        if (num < NotDeleteNum) return
        val id = num - NotDeleteNum
        ReaderDbManager.setReaded(bookName, id)
                .forEach { File("${getSavePath()}/$bookName/$it").delete() }
    }

    fun doDrawPrepare(): Int = runBlocking{
        maxChapterNum = ReaderDbManager.getChapterCount(bookName).takeIf { it != 0 } ?: return@runBlocking 0
        val record = async { getRecord() }.await()
        chapterNum.set(record[0])
        chapterCache = ChapterCache(CACHE_NUM, bookName).apply { init(maxChapterNum, bookName) }
        launch { getChapter(ChapterChangeType.BY_CATALOG, null) }
        record[1]
    }

    fun nextChapter(){
        isHeadViewShow.set(false)
        isFontSettingShow.set(false)
        isBackgroundSetingShow.set(false)
        isFootViewShow.set(false)
        launch { getChapter(ChapterChangeType.NEXT, null) }
    }

    fun previousChapter() {
        isHeadViewShow.set(false)
        isFontSettingShow.set(false)
        isBackgroundSetingShow.set(false)
        isFootViewShow.set(false)
        launch { getChapter(ChapterChangeType.PREVIOUS, null) }
    }

    fun onPageChange(index: Int){
        isHeadViewShow.set(false)
        isFontSettingShow.set(false)
        isBackgroundSetingShow.set(false)
        isFootViewShow.set(false)
        launch { setRecord(index) }
    }

    fun onCenterClick() {
        if(isFootViewShow.get()){
            isHeadViewShow.set(false)
            isFontSettingShow.set(false)
            isBackgroundSetingShow.set(false)
            isFootViewShow.set(false)
        }else{
            isFootViewShow.set(true)
            isHeadViewShow.set(true)
        }
    }


    /**
     * 获取阅读记录
     */
    private fun getRecord(): Array<Int> {

        val queryResult = ReaderDbManager.getRoomDB().shelfDao().getBookInfo(bookName)?.readRecord
                ?.split("#")?.map { it.toInt() }          //阅读记录 3#2 表示第3章第2页
        return arrayOf(queryResult?.get(0) ?: 1, queryResult?.get(1) ?: 1)
    }

    private fun updateCatalog() {
        try {
            DownloadCatalog(bookName, ReaderDbManager.getRoomDB().shelfDao().getBookInfo(bookName)?.downloadUrl
                    ?: "").download()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        maxChapterNum = ReaderDbManager.getChapterCount(bookName)
    }
}