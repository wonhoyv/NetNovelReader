package com.netnovelreader.data.network

import com.netnovelreader.data.database.BaseSQLManager
import com.netnovelreader.data.database.ParseSQLManager
import com.netnovelreader.common.TIMEOUT
import com.netnovelreader.common.getHeaders
import com.netnovelreader.common.url2Hostname
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Created by yangbo on 18-1-14.
 */
class ParseHtml {
    /**
     * 解析章节
     */
    @Throws(IOException::class)
    fun getChapter(url: String): String {
        var txt: String?
        val selector = ParseSQLManager().getChapterRule(url2Hostname(url), BaseSQLManager.CHAPTER_RULE)
        if(selector == null || selector.length < 2){
            txt = getChapterWithSelector(url)
        }else{
            txt = Jsoup.connect(url).headers(getHeaders(url))
                    .timeout(TIMEOUT).get().select(selector).text()

        }
        txt = "    " + txt!!.replace(" ", "\n\n  ")
        return txt
    }

    /**
     * 解析目录
     */
    @Throws(IOException::class)
    fun getCatalog(url: String): LinkedHashMap<String, String> {
        val selector = ParseSQLManager().getChapterRule(url2Hostname(url), BaseSQLManager.CATALOG_RULE)
        val catalog = LinkedHashMap<String, String>()
        selector ?: return catalog
        val list = Jsoup.connect(url).headers(getHeaders(url))
                .timeout(TIMEOUT).get().select(selector).select("a")
        list.forEach {
            if(!it.text().contains("分卷阅读")){
                var link = it.attr("href")
                if(!link.contains("//")){
                    link = url.substring(0, url.lastIndexOf('/') + 1) + link
                }else if(link.startsWith("//")){
                    link = "http:" + link
                }
                catalog.put(it.text(), link)
            }
        }
        return catalog
    }

    @Throws(IOException::class)
    fun getChapterWithSelector(url: String): String {
        val elements = Jsoup.connect(url).get().allElements
        val indexList = ArrayList<Element>()
        if(elements.size > 1){
            for(i in 1.. elements.size - 1){
                if(elements[0].text().length > elements.get(i).text().length * 2){
                    indexList.add(elements[i])
                }
            }
        }
        elements.removeAll(indexList)
        return elements.last().text()
    }
}