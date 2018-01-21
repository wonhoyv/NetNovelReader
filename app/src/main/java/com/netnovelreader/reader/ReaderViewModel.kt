package com.netnovelreader.reader

import android.databinding.ObservableArrayList
import android.util.Log
import com.netnovelreader.data.database.ChapterSQLManager
import com.netnovelreader.data.database.ShelfSQLManager
import com.netnovelreader.common.getSavePath
import com.netnovelreader.common.id2TableName
import com.netnovelreader.data.database.BaseSQLManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jsoup.Connection
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.Vector

/**
 * Created by yangbo on 18-1-13.
 */
class ReaderViewModel(val bookName: String) : IReaderContract.IReaderViewModel {
    var catalog  = ObservableArrayList<ReaderBean.Catalog>()
    /**
     * 一页显示的内容
     */
    @Volatile
    var text = ObservableArrayList<String>()
    /**
     * 一章的所有内容
     */
    @Volatile
    var chapterText = Vector<ArrayList<String>>()

    @Volatile
    var dirName: String? = null

    @Volatile
    var chapterName: String? = null
    /**
     * 章节数，页码，最大章节数，最大页码，例如1,1,4,5 == 第一章第一页,总共4章，这一章有5页
     */
    var pageIndicator = IntArray(4){ it -> 1 }

    override fun initData(width: Int, height: Int, txtFontSize: Float){
        pageIndicator[2] = getChapterCount()
        val array = getRecord()
        pageIndicator[0] = array[0]
        pageIndicator[1]  = array[1]
        getChapterTxt(pageIndicator,0, width, height, txtFontSize)
    }

    override fun pageNext(width: Int, height: Int, txtFontSize: Float) {
        if (pageIndicator[1] == pageIndicator[3] || pageIndicator[3] < 2){
            if(pageIndicator[0] == pageIndicator[2]){
                return
            }else{
                //下一章
                pageIndicator[0] += 1
                getChapterTxt(pageIndicator,1, width, height, txtFontSize)
            }
        }else{
            //同一章下一页
            pageIndicator[1] += 1
            updateTextAndRecord(pageIndicator)
        }
    }

    override fun pagePrevious(width: Int, height: Int, txtFontSize: Float) {
        if (pageIndicator[1] < 2){
            if(pageIndicator[0] == 1){
                return
            }else{
                //上一章
                pageIndicator[0] -= 1
                getChapterTxt(pageIndicator,-1, width, height, txtFontSize)
            }
        }else{
            //同一章上一页
            pageIndicator[1] -= 1
            updateTextAndRecord(pageIndicator)
        }
    }

    override fun pageByCatalog(chapterName: String, width: Int, height: Int, txtFontSize: Float) {
        var tableName: String? = null
        var sqlManager = ChapterSQLManager()
        val cursor = sqlManager.getDB().rawQuery("select ${BaseSQLManager.ID} from ${BaseSQLManager.TABLE_SHELF} " +
                "where ${BaseSQLManager.BOOKNAME}='$bookName';", null)
        if(cursor.moveToFirst()){
            tableName = id2TableName(cursor.getInt(0))
        }
        cursor.close()
        pageIndicator[0] = sqlManager.getChapterId(tableName ?: "", chapterName)
        pageIndicator[1]  = 1
        getChapterTxt(pageIndicator,0, width, height, txtFontSize)
    }

    override fun updateCatalog(): ObservableArrayList<ReaderBean.Catalog> {
        catalog.clear()
        val sqlManager = BaseSQLManager()
        val cursor = sqlManager.getDB().rawQuery("select ${BaseSQLManager.ID} from ${BaseSQLManager.TABLE_SHELF} " +
                "where ${BaseSQLManager.BOOKNAME}='$bookName';", null)
        if(!cursor.moveToFirst()) return catalog
        val catalogCursor = sqlManager.getDB().rawQuery("select ${BaseSQLManager.CHAPTERNAME} " +
                "from ${id2TableName(cursor.getInt(0))}", null)
        while (catalogCursor.moveToNext()){
            catalog.add(ReaderBean.Catalog(catalogCursor.getString(0)))
        }
        cursor.close()
        catalogCursor.close()
        sqlManager.closeDB()
        return catalog
    }

    /**
     * 获取阅读记录
     */
    fun getRecord(): IntArray{
        val queryResult = ShelfSQLManager().getRecord(bookName) //阅读记录 3#2 表示第3章第2页
        dirName = "BOOK${queryResult[0]}"
        var readRecord = queryResult[1]
        if(readRecord.length < 1){
            readRecord = "1#1"
        }
        val array = readRecord.split("#")
        return IntArray(2){i -> array[i].toInt()}
    }

    /**
     * 获取章节总数
     */
    fun getChapterCount(): Int = ChapterSQLManager().getChapterCount("BOOK${ShelfSQLManager().getRecord(bookName)[0]}")

    /**
     * @chapterNum
     * @dirName
     * @isNext  1 下一章,pagenum=1 ， -1 上一章 pagenum=pageIndicator[3] ,0 pagenum不变
     */
    fun getChapterTxt(pageIndicator: IntArray, isNext: Int, width: Int, height: Int, txtFontSize: Float){
        Observable.create<Vector<ArrayList<String>>> { e ->
            e.onNext(splitChapterTxt(readFile(pageIndicator[0]), width, height, txtFontSize))
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list ->
                    chapterText = list
                    pageIndicator[3] = list.size
                    if(isNext == 1){
                        if(pageIndicator[3] != 0){
                            pageIndicator[1] = 1
                        }else{
                            pageIndicator[1] = 0
                        }
                    }else if(isNext == -1){
                        pageIndicator[1] = pageIndicator[3]
                    }
                    updateTextAndRecord(pageIndicator)
                }
    }

    /**
     * 获取小说章节内容
     * @chapterNum:Int 章节数
     */
    @Throws(IOException::class)
    fun readFile(chapterNum: Int): String {
        val sb = StringBuilder()
        chapterName = ChapterSQLManager().getChapterName(dirName!!, chapterNum)
        val chapterPath = "${getSavePath()}/$dirName/$chapterName"
        if(!File(chapterPath).exists()) return ""
        val fr = FileReader(chapterPath)
        fr.forEachLine { sb.append(it + "\n") }
        fr.close()
        return sb.toString()
    }

    /**
     * @isNext  1 翻页下一章,pagenum=1 ， -1 翻页上一章 pagenum=pageIndicator[3] ,0 pagenum不变
     */
    fun updateTextAndRecord(pageIndicator: IntArray){
        text.clear()
        text.add("${chapterName}：${pageIndicator[1]}/${pageIndicator[3]}")
        if(chapterText.size < 1) return
        chapterText.get(pageIndicator[1] - 1).forEach { text.add(it) }
        ShelfSQLManager().setRecord(bookName, "${pageIndicator[0]}#${pageIndicator[1]}")
    }

    /**
     * 将一章分割成Vector<ObservableArrayList<String>>，表示：一共Vector每一项表示一页，ArrayList每一项表示一行
     * @width 屏幕宽
     * @height 屏幕高
     * @txtFontSize 字体大小
     */
    fun splitChapterTxt(chapter: String, width: Int, height: Int, txtFontSize: Float)
            : Vector<ArrayList<String>>{
        if(chapter.length < 1) return Vector()
        val tmpArray = chapter.split("\n")
        val tmplist = ArrayList<String>()
        tmpArray.forEach{
            val tmp = "  " + it.trim()
            val totalCount = width / txtFontSize.toInt() - 1
            if(tmp.length > totalCount){
                val count = tmp.length / totalCount
                for(i in 0..count - 1){
                    tmplist.add(tmp.substring(i * totalCount, (i + 1) * totalCount))
                }
                if(it.length % totalCount != 0){
                    tmplist.add(tmp.substring(count * totalCount))
                }
            }else {
                tmplist.add(tmp)
            }
        }
        val arrayList = Vector<ArrayList<String>>()
        val totalCount = height / txtFontSize.toInt() - 2
        if( tmplist.size > totalCount){
            val count = tmplist.size / totalCount
            for(i in 0..count -1){
                val a = ObservableArrayList<String>()
                tmplist.subList(i * totalCount, (i + 1) * totalCount).forEach{ a.add(it)}
                arrayList.add(a)
            }
            if(tmplist.size % totalCount != 0){
                val b = ObservableArrayList<String>()
                tmplist.subList(count * totalCount, tmplist.size - 1).forEach{ b.add(it)}
                arrayList.add(b)
            }
        }
        return arrayList
    }
}