package com.netnovelreader.reader

import android.databinding.ObservableArrayList
import com.netnovelreader.data.database.ChapterSQLManager
import com.netnovelreader.data.database.ShelfSQLManager
import com.netnovelreader.utils.getSavePath
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.FileReader
import java.io.IOException
import java.util.Vector

/**
 * Created by yangbo on 18-1-13.
 */
class ReaderViewModel(val bookName: String) : IReaderContract.IReaderViewModel {
    /**
     * 一页显示的内容
     */
    var text = ObservableArrayList<String>()
    /**
     * 一章的所有内容
     */
    var chapterText = Vector<ArrayList<String>>()

    var dirName: String? = null

    /**
     * 获取阅读记录
     */
    override fun getRecord(): IntArray{
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
    override fun getChapterCount(): Int = ChapterSQLManager().getChapterCount("BOOK${ShelfSQLManager().getRecord(bookName)[0]}")

    /**
     * @chapterNum
     * @dirName
     * @isNext  1 下一章,pagenum=1 ， -1 上一章 pagenum=pageIndicator[3] ,0 pagenum不变
     */
    override fun getChapterTxt(pageIndicator: IntArray, isNext: Int, width: Int, height: Int, txtFontSize: Float){
        Observable.create<Vector<ArrayList<String>>> { e ->
            e.onNext(splitChapterTxt(readFile(pageIndicator[0]), width, height, txtFontSize))
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list ->
                    chapterText = list
                    pageIndicator[3] = chapterText.size
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
        val chapterPath = "${getSavePath()}/$dirName/${ChapterSQLManager().getChapterName(dirName!!, chapterNum)}"
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
        text.add("第${pageIndicator[0]}章：${pageIndicator[1]}/${pageIndicator[3]}")
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