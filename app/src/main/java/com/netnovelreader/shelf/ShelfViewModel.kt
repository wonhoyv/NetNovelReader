package com.netnovelreader.shelf

import android.databinding.ObservableArrayList
import android.databinding.ObservableField
import android.databinding.ObservableInt
import com.netnovelreader.common.DownloadTask
import com.netnovelreader.common.getSavePath
import com.netnovelreader.common.id2TableName
import com.netnovelreader.data.database.SQLHelper
import com.netnovelreader.data.database.ChapterSQLManager
import com.netnovelreader.data.database.ShelfSQLManager
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Created by yangbo on 2018/1/12.
 */
class ShelfViewModel : IShelfContract.IShelfViewModel {

    var bookList: ObservableArrayList<ShelfBean>
    init {
        bookList = ObservableArrayList<ShelfBean>()
    }

    override fun updateBooks(): Boolean{
        var threadPoolExecutor = Executors.newFixedThreadPool(5)
        bookList.forEach {
            threadPoolExecutor.execute {
                try {
                    DownloadTask(id2TableName(it.bookid.get()), it.downloadURL.get()).updateSql()
                    refreshBookList()
                }catch (e : IOException){
                }
            }
        }
        return true
    }

    /**
     * 更新书架
     */
    override fun refreshBookList(){
        val dbManager = ShelfSQLManager()
        bookList.clear()
        val cursor = dbManager.queryBookList()
        while (cursor != null && cursor.moveToNext()){
            val bookBean = ShelfBean(ObservableInt(cursor.getInt(cursor.getColumnIndex(SQLHelper.ID))),
                    ObservableField(cursor.getString(cursor.getColumnIndex(SQLHelper.BOOKNAME))),
                    ObservableField(cursor.getString(cursor.getColumnIndex(SQLHelper.LATESTCHAPTER)) ?: ""),
                    ObservableField(cursor.getString(cursor.getColumnIndex(SQLHelper.DOWNLOADURL))))
            bookList.add(bookBean)
        }
        cursor?.close()
    }

    override fun deleteBook(bookname: String){
        val id = ShelfSQLManager().removeBookFromShelf(bookname)
        if(id == 0) return
        ChapterSQLManager().dropTable(id2TableName(id))
        deleteDirs(File(getSavePath(), id2TableName(id)))
    }

    fun deleteDirs(file: File){
        if(!file.exists()) return
        if(file.isFile){
            file.delete()
        }else{
            val fileArray = file.listFiles()
            if(fileArray.size > 0){
                for(i in 0..fileArray.size - 1){
                    fileArray[i].delete()
                }
                file.delete()
            }else{
                file.delete()
            }
        }
    }
}