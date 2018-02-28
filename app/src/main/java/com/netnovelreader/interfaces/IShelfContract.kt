package com.netnovelreader.interfaces

/**
 * Created by yangbo on 18-1-13.
 */
interface IShelfContract {
    interface IShelfView : IView {
        fun checkPermission(permission: String): Boolean
        fun requirePermission(permission: String, reqCode: Int)
    }

    interface IShelfViewModel : IViewModel {
        fun readBookEvent(bookname: String)
        fun deleteBookEvent(bookname: String): Boolean
        suspend fun updateBooks()
        suspend fun refreshBookList()
        suspend fun deleteBook(bookname: String)
    }
}