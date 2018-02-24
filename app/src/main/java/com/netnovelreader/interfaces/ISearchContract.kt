package com.netnovelreader.interfaces

import com.netnovelreader.bean.SearchBean
import com.netnovelreader.viewmodel.SearchViewModel
import kotlinx.coroutines.experimental.Job

/**
 * Created by yangbo on 18-1-14.
 */
interface ISearchContract {
    interface ISearchView : IView<SearchViewModel>
    interface ISearchViewModel : IViewModel<SearchBean> {
        suspend fun refreshHotWords()
        suspend fun addBookToShelf(bookname: String, url: String): String
        suspend fun searchBook(bookname: String?): Job
        suspend fun saveBookImage(tableName: String, bookname: String)
        suspend fun delChapterAfterSrc(tableName: String, chapterName: String)
    }
}