package com.netnovelreader.shelf

import android.databinding.ObservableField
import android.databinding.ObservableInt
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.netnovelreader.common.*
import com.netnovelreader.common.data.SQLHelper
import com.netnovelreader.common.download.DownloadCatalog
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors

/**
 * Created by yangbo on 2018/1/12.
 */
class ShelfViewModel : IShelfContract.IShelfViewModel {

    var bookList: ObservableSyncArrayList<BookBean>

    init {
        bookList = ObservableSyncArrayList()
    }

    //检查书籍是否有更新
    override fun updateBooks(): Boolean {
        val threadPoolExecutor = Executors.newFixedThreadPool(5)
        var i = 0
        var tmp = 0
        Observable.fromIterable(bookList).flatMap { bean ->
            Observable.create<Int> { emitter ->
                try {
                    DownloadCatalog(
                            id2TableName(bean.bookid.get()),
                            bean.downloadURL.get() ?: ""
                    ).download()
                } catch (e: IOException) {
                } finally {
                    emitter.onNext(++i)
                }
            }.subscribeOn(Schedulers.from(threadPoolExecutor))
        }.observeOn(Schedulers.single()).subscribe {
            if (i > tmp && (i % 3 == 0 || i == bookList.size)) {  //避免刷新太频繁导致recyclerview崩溃
                refreshBookList()
                tmp = i
            }
        }
        return true
    }

    //取消书籍更新标志"●",设为最近阅读
    override fun cancelUpdateFlag(bookname: String) {
        SQLHelper.cancelUpdateFlag(bookname)
        SQLHelper.setLatestRead(bookname)
    }

    /**
     * 刷新书架，从数据库重新获取
     */
    @Synchronized
    override fun refreshBookList() {
        launch {
            val arrayList = ArrayList<BookBean>()
            val bookDirList = dirBookList()
            SQLHelper.queryShelfBookList().forEach {
                val bookBean = BookBean(ObservableInt(it.key), ObservableField(it.value[0]), ObservableField(it.value[1]),
                        ObservableField(it.value[2]), ObservableField(getBitmap(it.key)), ObservableField(it.value[3]))
                if (bookDirList.contains(id2TableName(bookBean.bookid.get()))) {
                    arrayList.add(bookBean)
                    updateCatalog(bookBean, false)
                } else {
                    launch { deleteBook(bookBean.bookname.get() ?: "") }
                }
            }
            bookList.clear()
            bookList.addAll(arrayList)
        }
    }

    //删除书籍
    override fun deleteBook(bookname: String) {
        launch {
            SQLHelper.removeBookFromShelf(bookname).takeIf { it > -1 }?.apply {
                SQLHelper.dropTable(id2TableName(this))
                File(getSavePath(), id2TableName(this)).deleteRecursively()
            }
        }
    }

    //获取文件夹里面的书列表
    private suspend fun dirBookList(): ArrayList<String> {
        return async {
            val list = ArrayList<String>()
            File(getSavePath()).takeIf { it.exists() }?.list()?.forEach { list.add(it) }
            list
        }.await()
    }

    //更新目录
    private suspend fun updateCatalog(bookBean: BookBean, must: Boolean): Int {
        return async {
            val tableName = id2TableName(bookBean.bookid.get())
            if (must || SQLHelper.getChapterCount(tableName) == 0) {
                try {
                    DownloadCatalog(tableName, bookBean.downloadURL.get() ?: "").download()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            1
        }.await()
    }

    //书架将要显示的书籍封面图片
    private suspend fun getBitmap(bookId: Int): Bitmap = async {
        File("${getSavePath()}/${id2TableName(bookId)}", IMAGENAME)
                .takeIf { it.exists() }
                ?.let { BitmapFactory.decodeFile(it.path) }
                ?: getDefaultCover()
    }.await()
}