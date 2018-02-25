package com.netnovelreader.ui

import android.app.AlertDialog
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.netnovelreader.R
import com.netnovelreader.common.PreferenceManager
import com.netnovelreader.common.RecyclerAdapter
import com.netnovelreader.common.init
import com.netnovelreader.common.toast
import com.netnovelreader.data.network.CatalogCache
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

class SearchActivity : AppCompatActivity(), ISearchContract.ISearchView {
    var searchViewModel: SearchViewModel? = null
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.getThemeId(this).also { setTheme(it) }
        super.onCreate(savedInstanceState)
        initViewModel()
        initView()

        val isChangeSource = !intent.getStringExtra("bookname").isNullOrEmpty()
        searchViewModel?.isChangeSource?.set(isChangeSource)
        if (isChangeSource) {
            changeSource()
        } else {
            launch { searchViewModel?.refreshHotWords() }
        }
    }

    override fun initViewModel() {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        searchViewModel = ViewModelProviders.of(this, factory).get(SearchViewModel::class.java)
        val binding =
            DataBindingUtil.setContentView<ActivitySearchBinding>(this, R.layout.activity_search)
        binding.clickEvent = BackClickEvent()
        binding.viewModel = searchViewModel
    }

    override fun initView() {
        searchRecycler.init(
            RecyclerAdapter(
                searchViewModel?.resultList,
                R.layout.item_search,
                SearchItemClickEvent()
            )
        )

        searchViewBar.setOnQueryTextListener(QueryListener())
        searchViewBar.onActionViewExpanded()

        searchSuggestRecycler.init(
            RecyclerAdapter(
                searchViewModel?.suggestList, R.layout.item_search_suggest,
                SuggestSearchItemClickEvent()
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun onDestroy() {
        super.onDestroy()
        (searchRecycler.adapter as RecyclerAdapter<Any>).removeDataChangeListener()
        (searchSuggestRecycler.adapter as RecyclerAdapter<Any>).removeDataChangeListener()
        CatalogCache.clearCache()
        job?.cancel()
    }

    private fun changeSource() {
        val bookname = intent.getStringExtra("bookname")
        searchViewText.text = bookname
        launch {
            searchViewModel?.searchBook(bookname)
        }
    }

    inner class QueryListener : android.support.v7.widget.SearchView.OnQueryTextListener {

        override fun onQueryTextSubmit(query: String): Boolean {
            launch {
                job?.cancel()
                job = launch { searchViewModel?.searchBook(query) }
            }
            searchViewBar.clearFocus()                    //提交搜索commit后收起键盘
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            searchViewModel?.onQueryTextChange(newText)
            return true
        }
    }

    //backbutton点击事件
    inner class BackClickEvent : IClickEvent {
        fun onBackClick() {
            finish()
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
            launch(UI) {
                val novelIntroduce = async { searchViewModel?.detailClick(itemText) }.await()
                if (novelIntroduce == null) {
                    toast("没有搜索到相关小说的介绍")
                } else {
                    val intent = Intent(v.context, NovelDetailActivity::class.java)
                    intent.putExtra("data", novelIntroduce)
                    v.context.startActivity(intent)
                }
            }
        }

        //搜索列表item下载事件
        fun onClickDownload(v: View) {
            val container = v.parent as View      //获取下载按钮的父元素 即ItemView
            val listener = DialogInterface.OnClickListener { _, which ->
                val catalogUrl = container.resultUrl.text.toString()
                launch(UI) {
                    val str = async {
                        searchViewModel!!.downloadCatalog(
                            container.resultName.text.toString(),
                            catalogUrl, intent.getStringExtra("chapterName"), which
                        )
                    }.await()
                    if (str == "0") {
                        toast(getString(R.string.downloadFailed))
                        return@launch
                    }
                    toast(getString(R.string.catalog_finish))
                    if (str != "1") {
                        val intent = Intent(v.context, DownloadService::class.java)
                        intent.putExtra("tableName", str)
                        intent.putExtra("catalogurl", catalogUrl)
                        v.context.startService(intent)
                    }
                    this@SearchActivity.finish()
                }
            }
            AlertDialog.Builder(this@SearchActivity).setTitle(getString(R.string.downloadAllBook))
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(getString(R.string.no), listener)
                .setNeutralButton(getString(R.string.cancel), null)
                .create().show()
        }
    }
}