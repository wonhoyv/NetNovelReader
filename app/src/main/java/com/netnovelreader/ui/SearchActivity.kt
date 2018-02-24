package com.netnovelreader.ui

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.netnovelreader.R
import com.netnovelreader.common.*
import com.netnovelreader.data.db.ReaderDbManager
import com.netnovelreader.data.network.ApiManager
import com.netnovelreader.data.network.CatalogCache
import com.netnovelreader.data.network.DownloadCatalog
import com.netnovelreader.data.network.DownloadChapter
import com.netnovelreader.databinding.ActivitySearchBinding
import com.netnovelreader.interfaces.IClickEvent
import com.netnovelreader.interfaces.ISearchContract
import com.netnovelreader.service.DownloadService
import com.netnovelreader.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.item_search.view.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.IOException

class SearchActivity : AppCompatActivity(), ISearchContract.ISearchView {
    var searchViewModel: SearchViewModel? = null
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.getThemeId(this).also { setTheme(it) }
        super.onCreate(savedInstanceState)
        setViewModel()
        init()
    }

    override fun setViewModel() {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        searchViewModel = ViewModelProviders.of(this, factory).get(SearchViewModel::class.java)
        val binding =
                DataBindingUtil.setContentView<ActivitySearchBinding>(this, R.layout.activity_search)
        binding.clickEvent = BackClickEvent()
        binding.viewModel = searchViewModel
    }

    override fun init() {
        searchRecycler.init(
                RecyclerAdapter(searchViewModel?.resultList, R.layout.item_search, SearchItemClickEvent())
        )

        searchViewBar.setOnQueryTextListener(QueryListener())
        searchViewBar.onActionViewExpanded()

        searchSuggestRecycler.init(
                RecyclerAdapter(searchViewModel?.suggestList, R.layout.item_search_suggest,
                        SuggestSearchItemClickEvent())
        )

        if (intent.getStringExtra("bookname").isNullOrEmpty()) {
            launch { searchViewModel?.refreshHotWords() }
        } else {
            changeSource()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (searchRecycler.adapter as RecyclerAdapter<Any>).removeDataChangeListener()
        (searchSuggestRecycler.adapter as RecyclerAdapter<Any>).removeDataChangeListener()
        CatalogCache.clearCache()
        job?.cancel()
    }

    override fun onBackPressed() {
        if (searchloadingbar.isShown) return
        super.onBackPressed()
    }

    private fun changeSource() {
        val bookname = intent.getStringExtra("bookname")
        if (bookname.isNullOrEmpty()) return
        searchViewBar.visibility = View.INVISIBLE
        searchViewText.visibility = View.VISIBLE
        searchViewText.text = bookname
        hideHotWords()
        launch {
            searchViewModel?.searchBook(bookname)
        }
    }

    inner class QueryListener : android.support.v7.widget.SearchView.OnQueryTextListener {
        private var tmp = ""
        private var tmpTime = System.currentTimeMillis()

        override fun onQueryTextSubmit(query: String): Boolean {
            searchloadingbar.hide()
            if (tmp == query && System.currentTimeMillis() - tmpTime < 1000) return true  //点击间隔小于1秒，并且搜索书名相同不再搜索
            if (query.isNotEmpty()) {
                launch {
                    job?.cancel()
                    job = searchViewModel?.searchBook(query)
                }
                tmp = query
                tmpTime = System.currentTimeMillis()
                searchViewBar.clearFocus()                    //提交搜索commit后收起键盘
                searchSuggestRecycler.visibility = View.GONE
            }
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            if (newText!!.isEmpty() && searchViewModel?.resultList!!.isEmpty()) {
                showHotWords()   //搜索框里文字为空并且仍未搜索过  -->显示热门搜索标签
                searchSuggestRecycler.visibility = View.GONE
            } else {
                hideHotWords()   //搜索框里文字不为空时  -->隐藏热门搜索标签
                searchSuggestRecycler.visibility = View.VISIBLE
                searchViewModel?.searchBookSuggest(newText)
            }
            return true
        }
    }

    /**
     * 显示搜索热词的相关UI界面
     */
    private fun showHotWords() {
        tvSearchLabel.visibility = View.VISIBLE
        tvRefreshHotWord.visibility = View.VISIBLE
        linearLayout.visibility = View.VISIBLE
    }

    /**
     * 隐藏搜索热词的相关UI界面
     */
    private fun hideHotWords() {
        tvSearchLabel.visibility = View.GONE
        tvRefreshHotWord.visibility = View.GONE
        linearLayout.visibility = View.GONE
    }

    //backbutton点击事件
    inner class BackClickEvent : IClickEvent {
        fun onBackClick() {
            finish()
        }

        //换一批热门搜索词
        fun refreshHotWords() {
            launch { searchViewModel?.refreshHotWords() }
        }

        //将搜索热词填充到searchView上但是不触发网络请求
        fun submitHotWord(v: View) {
            v as TextView
            searchViewBar.setQuery(v.text, false)
        }
    }

    //建议搜索列表item点击事件
    inner class SuggestSearchItemClickEvent : IClickEvent {
        fun onClick(v: View) {
            val textView = v.findViewById<TextView>(R.id.tvSearchSuggest)
            searchViewBar.setQuery(textView.text, true)
            searchSuggestRecycler.visibility = View.GONE
        }
    }


    //搜索列表item点击事件
    inner class SearchItemClickEvent : IClickEvent {

        fun onClickDetail(v: View) {

            val itemText = v.findViewById<TextView>(R.id.resultName).text.toString()
            ApiManager.mAPI.searchBook(itemText).enqueueCall {
                val first = it?.books?.firstOrNull { it.title == itemText }
                if (first?.title != itemText) {
                    Snackbar.make(searchRoot, "没有搜索到相关小说的介绍", Snackbar.LENGTH_SHORT).show()
                    return@enqueueCall
                }
                ApiManager.mAPI.getNovelIntroduce(first._id ?: "").enqueueCall {
                    when (it?._id) {
                        null -> launch(UI) {
                            Snackbar.make(searchRoot, "没有搜索到相关小说的介绍", Snackbar.LENGTH_SHORT).show()
                        }
                        else -> {
                            val intent = Intent(this@SearchActivity, NovelDetailActivity::class.java)
                            intent.putExtra("data", it)
                            this@SearchActivity.startActivity(intent)
                        }
                    }
                }
            }
        }

        //搜索列表item下载事件
        fun onClickDownload(v: View) {

            val container = v.parent as View      //获取下载按钮的父元素 即ItemView
            if (searchloadingbar.isShown) return     //如果正在下载中，return

            val listener = DialogInterface.OnClickListener { _, which ->
                searchloadingbar.show()
                launch(UI) {
                    //在UI线程中执行一些操作
                    val catalogUrl = container.resultUrl.text.toString()
                    val bookname = container.resultName.text.toString()
                    val tableName = searchViewModel!!.addBookToShelf(
                            bookname,
                            catalogUrl
                    )           //addBookToShelf方法需要被suspend修饰？
                    when (which) {
                        Dialog.BUTTON_POSITIVE -> download(bookname, catalogUrl) {
                            downloadBook(v.context, tableName, catalogUrl)
                        }
                        Dialog.BUTTON_NEGATIVE -> download(bookname, catalogUrl) {
                            launch { downNowChapter(tableName) }                     //新启动一个线程执行下载任务
                        }
                    }
                }
            }
            AlertDialog.Builder(this@SearchActivity).setTitle(getString(R.string.downloadAllBook))
                    .setPositiveButton(R.string.yes, listener)
                    .setNegativeButton(getString(R.string.no), listener)
                    .setNeutralButton(getString(R.string.cancel), null)
                    .create().show()
        }

        /**
         * 下载，调用[downloadBook]或[downNowChapter]
         */
        private fun download(
                bookname: String,
                catalogUrl: String,
                method: suspend () -> Unit
        ) {         //函数A作为函数B的参数传递进来，然后在函数B里执行函数A
            async {
                searchViewModel!!.addBookToShelf(bookname, catalogUrl).apply {
                    searchViewModel?.saveBookImage(this, bookname)
                    try {
                        DownloadCatalog(this, catalogUrl).download()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }.invokeOnCompletion {
                launch(UI) {
                    it?.apply { toast(getString(R.string.downloadFailed)) }
                            ?: kotlin.run {
                                toast(getString(R.string.catalog_finish))
                                method()
                                this@SearchActivity.finish()
                            }
                    searchloadingbar.hide()
                }
            }
        }

        //下载全书，若该书已存在，则下载所有未读章节
        private suspend fun downloadBook(
                context: Context,
                tableName: String,
                catalogUrl: String
        ) {
            val chapterName = intent.getStringExtra("chapterName")
            if (!chapterName.isNullOrEmpty()) {
                searchViewModel?.delChapterAfterSrc(tableName, chapterName)
                launch {
                    try {
                        DownloadCatalog(tableName, catalogUrl).download()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }.join()
            }
            val intent = Intent(context, DownloadService::class.java)
            intent.putExtra("tableName", tableName)
            intent.putExtra("catalogurl", catalogUrl)
            startService(intent)
        }

        //换源下载，只下载当前章节
        private suspend fun downNowChapter(tableName: String) {
            val chapterName = intent.getStringExtra("chapterName")
            if (!chapterName.isNullOrEmpty()) {
                searchViewModel?.delChapterAfterSrc(tableName, chapterName)
                DownloadChapter(
                        tableName, "${getSavePath()}/$tableName",
                        chapterName, ReaderDbManager.getChapterUrl(tableName, chapterName)
                )
            }
        }
    }
}