package com.netnovelreader.interfaces

import com.netnovelreader.bean.ChapterChangeType

/**
 * Created by yangbo on 18-1-13.
 */
interface IReaderContract {
    interface IReaderView : IView {
        fun showDialog()
    }

    interface IReaderViewModel : IViewModel {
        suspend fun getChapter(
            type: ChapterChangeType,
            chapterName: String?
        )
        suspend fun downloadAndShow()
        suspend fun reloadCurrentChapter()
        suspend fun getCatalog()
        suspend fun setRecord(pageNum: Int)
        suspend fun autoRemove()
    }
}