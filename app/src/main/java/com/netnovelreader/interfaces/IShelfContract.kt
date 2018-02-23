package com.netnovelreader.interfaces

import com.netnovelreader.bean.BookBean
import com.netnovelreader.viewmodel.ShelfViewModel

/**
 * Created by yangbo on 18-1-13.
 */
interface IShelfContract {
    interface IShelfView : IView<ShelfViewModel> {
        fun updateShelf()
        fun checkPermission(permission: String): Boolean
        fun requirePermission(permission: String, reqCode: Int)
    }

    interface IShelfViewModel : IViewModel<BookBean> {
        suspend fun updateBooks()
        suspend fun refreshBookList()
        suspend fun cancelUpdateFlag(bookname: String)
        suspend fun deleteBook(bookname: String)
    }
}