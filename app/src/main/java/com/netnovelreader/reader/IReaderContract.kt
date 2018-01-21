package com.netnovelreader.reader

import android.databinding.ObservableArrayList
import com.netnovelreader.base.IView
import com.netnovelreader.base.IViewModel

/**
 * Created by yangbo on 18-1-13.
 */
interface IReaderContract {
    interface IReaderView: IView<ReaderViewModel>{
    }
    interface IReaderViewModel: IViewModel<ReaderBean> {
        fun initData(width: Int, height: Int, txtFontSize: Float)
        fun pageNext(width: Int, height: Int, txtFontSize: Float)
        fun pagePrevious(width: Int, height: Int, txtFontSize: Float)
        fun pageByCatalog(chapterName: String, width: Int, height: Int, txtFontSize: Float)
        fun updateCatalog(): ObservableArrayList<ReaderBean.Catalog>
    }
}