package com.netnovelreader.data

import com.netnovelreader.common.TIMEOUT
import com.netnovelreader.common.UA
import com.netnovelreader.common.getHeaders
import com.netnovelreader.common.url2Hostname
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by yangbo on 18-1-14.
 */
class SearchBook : Cloneable {

    /**
     * @url
     * @redirectFileld
     * @redirectSelector    目录地址selector
     * @noRedirectSelector
     * @redirectName        书名selector
     * @noRedirectName
     * @redirectImage imageurl selector
     * @noRedirectImage
     * 有些网站搜索到书名后，响应头例如Location：http://www.yunlaige.com/book/19984.html，然后跳转到书籍页,redirectFileld表示响应头跳转链接
     * 有些网站搜索到书名后，显示搜索列表,
     * Selector  jsoup选择结果页目录url
     * Name  jsoup选择结果页书名
     */
    @Throws(ConnectException::class)
    fun search(url: String, redirectFileld: String, redirectSelector: String, noRedirectSelector: String,
               redirectName: String, noRedirectName: String, redirectImage: String, noRedirectImage: String)
            : Array<String> {
        var result: Array<String>
        if (redirectFileld.equals("")) {
            search(url, noRedirectSelector, noRedirectName, noRedirectImage)
        }
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("accept", "indicator/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        conn.setRequestProperty("user-agent", UA)
        conn.setRequestProperty("Upgrade-Insecure-Requests", "1")
        conn.setRequestProperty("Connection", "keep-alive")
        conn.setRequestProperty("Referer", "http://www.${url2Hostname(url)}/")
        val redirect_url = conn.getHeaderField(redirectFileld)
        conn.disconnect()
        if (redirect_url != null && redirect_url.length > 5) {
            result = search(url, redirectSelector, redirectName, redirectImage)
        } else {
            result = search(url, noRedirectSelector, noRedirectName, noRedirectImage)
        }
        return result
    }

    @Throws(ConnectException::class)
    fun search(url: String, catalogSelector: String, nameSelector: String, imageSelector: String)
            : Array<String> {
        val doc: Element = Jsoup.connect(url).headers(getHeaders(url))
                .timeout(TIMEOUT).get()
        return arrayOf(parseCatalogUrl(doc, url, catalogSelector), parseBookname(doc, nameSelector),
                parseImageUrl(doc, imageSelector))
    }

    @Throws(ConnectException::class)
    fun parseCatalogUrl(doc: Element, url: String, urlSelector: String): String {
        var result = doc.select(urlSelector).select("a").attr("href")
        if (!result!!.contains("//")) {
            result = url.substring(0, url.lastIndexOf('/') + 1) + result
        } else if (result.startsWith("//")) {
            result = "http:" + result
        }
        if (result.contains("qidian.com")) {
            result += "#Catalog"
        }
        return result
    }

    @Throws(ConnectException::class)
    fun parseBookname(doc: Element, nameSelector: String): String{
        if(nameSelector.equals("")) return ""
        return doc.select(nameSelector).text()
    }

    @Throws(ConnectException::class)
    fun parseImageUrl(doc: Element, imageSelector: String): String{
        if(imageSelector.equals("")) return ""
        var url = doc.select(imageSelector).attr("src")
        if (url.startsWith("//")) {
            url = "http:" + url
        }
        return url
    }

    override fun clone(): SearchBook {
        return super.clone() as SearchBook
    }
}