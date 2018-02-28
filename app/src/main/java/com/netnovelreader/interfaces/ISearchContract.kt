package com.netnovelreader.interfaces

import android.database.Cursor
import com.netnovelreader.bean.SearchBean
import kotlinx.coroutines.experimental.Job

/**
 * Created by yangbo on 18-1-14.
 */
interface ISearchContract {
    interface ISearchView : IView
    interface ISearchViewModel : IViewModel {
        fun onQueryTextChange(newText: String?): Cursor?
        fun refreshHotWords(): Job
        suspend fun searchBook(bookname: String?, chapterName: String?)
        fun detailClickTask(itemText: String)
        fun showDialogTask(itemDetail: SearchBean)
        fun downloadBook(bookname: String, catalogUrl: String, chapterName: String?, which: Int): Job
        fun activityExitTask()
        fun selectHotWordTask(word: String)
    }
}