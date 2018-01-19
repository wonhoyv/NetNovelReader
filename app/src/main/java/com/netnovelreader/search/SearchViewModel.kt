package com.netnovelreader.search

import android.databinding.ObservableArrayList
import android.databinding.ObservableField
import com.netnovelreader.data.database.SearchSQLManager
import com.netnovelreader.data.database.ShelfSQLManager
import com.netnovelreader.data.network.SearchBook
import com.netnovelreader.utils.id2Bookname
import java.net.URLEncoder

/**
 * Created by yangbo on 18-1-14.
 */
class SearchViewModel : ISearchContract.ISearchViewModel {
    @Volatile
    var searchCode = 0
    var resultList: ObservableArrayList<SearchBean>
    init {
        resultList = ObservableArrayList<SearchBean>()
    }

    /**
     * 添加书到数据库
     */
    override fun addBookToShelf(bookname: String, url: String): String{
        return id2Bookname(ShelfSQLManager().addBookToShelf(bookname, url))
    }

    override fun searchBook(bookname: String?) {
        bookname ?: return
        searchCode++
        resultList.clear()
        //查询所有搜索站点设置，然后逐个搜索
        SearchSQLManager().queryAll().forEach { Thread{ searchBookFromSite(bookname, it, searchCode) }.start() }
    }


    override fun searchBookFromSite(bookname: String, siteinfo: Array<String?>, reqCode: Int) {
        var result: String? = null
        val url = siteinfo[1]!!.replace(SearchSQLManager.SEARCH_NAME, URLEncoder.encode(bookname, siteinfo[7]))
        try{
            if (siteinfo[0].equals("0")) {
                result = SearchBook().search(url, siteinfo[4] ?: "", siteinfo[6] ?: "")
            } else {
                result = SearchBook().search(url, siteinfo[2] ?: "", siteinfo[3] ?: "",
                        siteinfo[4] ?: "", siteinfo[5] ?: "", siteinfo[6] ?: "")
            }
        }catch (e: Exception){
            result ?: return
        }
        result ?: return
        val s = result.split("~~~")
        if(searchCode == reqCode){
            resultList.add(SearchBean(ObservableField(s[0]), ObservableField(s[1])))
        }
    }
}