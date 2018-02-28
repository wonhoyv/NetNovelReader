package com.netnovelreader.ui

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.widget.CursorAdapter
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.netnovelreader.R
import com.netnovelreader.common.PreferenceManager
import com.netnovelreader.common.RecyclerAdapter
import com.netnovelreader.common.init
import com.netnovelreader.common.toast
import com.netnovelreader.data.network.CatalogCache
import com.netnovelreader.databinding.ActivitySearchBinding
import com.netnovelreader.interfaces.ISearchContract
import com.netnovelreader.service.DownloadService
import com.netnovelreader.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class SearchActivity : AppCompatActivity(), ISearchContract.ISearchView {
    var searchViewModel: SearchViewModel? = null
    private var job: Job? = null
    private var suggestCursor: Cursor? = null
    private var chapterName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.getThemeId(this).also { setTheme(it) }
        super.onCreate(savedInstanceState)
        initViewModel()
        initView()
        initData()
        initLiveData()
    }

    override fun initViewModel() {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        searchViewModel = ViewModelProviders.of(this, factory).get(SearchViewModel::class.java)
        DataBindingUtil.setContentView<ActivitySearchBinding>(this, R.layout.activity_search)
                .apply { viewModel = searchViewModel }
    }

    override fun initView() {
        searchRecycler.init(
                RecyclerAdapter(searchViewModel?.resultList, R.layout.item_search, searchViewModel)
        )
        searchViewBar.setOnQueryTextListener(QueryListener())
        searchViewBar.onActionViewExpanded()
        searchViewBar.suggestionsAdapter = SearchViewAdapter(this, null)
        searchViewBar.setOnSuggestionListener(SuggestionListener())
    }

    fun initData(){
        chapterName = intent.getStringExtra("chapterName")
        searchViewModel?.isChangeSource?.set(!chapterName.isNullOrEmpty())
        if (!chapterName.isNullOrEmpty()) {
            searchViewModel?.isChangeSource?.set(true)
            val bookname = intent.getStringExtra("bookname")
            searchViewText.text = bookname
            launch {
                searchViewModel?.searchBook(bookname, intent.getStringExtra("chapterName"))
            }
        } else {
            launch { searchViewModel?.refreshHotWords() }
        }
    }

    override fun initLiveData() {
        searchViewModel?.toastMessage?.observe(this, Observer { it?.let { toast(it) } })
        searchViewModel?.exitCommand?.observe(this, Observer { finish() })
        searchViewModel?.selectHotWordCommand?.observe(
                this, Observer { searchViewBar.setQuery(it, false) })
        searchViewModel?.showBookDetailCommand?.observe(this, Observer {
            it ?: return@Observer
            val intent = Intent(this@SearchActivity, NovelDetailActivity::class.java)
            intent.putExtra("data", it)
            this@SearchActivity.startActivity(intent)
        })
        searchViewModel?.downLoadChapterCommand?.observe(this, Observer {
            it ?: return@Observer
            val intent = Intent(this@SearchActivity, DownloadService::class.java)
            intent.putExtra("tableName", it[0])
            intent.putExtra("catalogurl", it[1])
            startService(intent)
        })
        searchViewModel?.showDialogCommand?.observe(this, Observer {
            it ?: return@Observer
            val listener = DialogInterface.OnClickListener { _, which ->
                searchViewModel!!.downloadBook(it.bookname.get()!!, it.url.get()!!, chapterName, which)
                finish()
            }
            AlertDialog.Builder(this@SearchActivity).setTitle(getString(R.string.downloadAllBook))
                    .setPositiveButton(R.string.yes, listener)
                    .setNegativeButton(getString(R.string.no), listener)
                    .setNeutralButton(getString(R.string.cancel), null)
                    .create().show()
        })
    }

    @Suppress("UNCHECKED_CAST")
    override fun onDestroy() {
        super.onDestroy()
        (searchRecycler.adapter as RecyclerAdapter<Any, Any>).removeDataChangeListener()
        CatalogCache.clearCache()
        job?.cancel()
    }

    inner class QueryListener : android.support.v7.widget.SearchView.OnQueryTextListener {
        var deffered: Deferred<Cursor?>? = null

        override fun onQueryTextSubmit(query: String): Boolean {
            launch {
                job?.cancel()
                job = launch { searchViewModel?.searchBook(query, null) }
            }
            searchViewBar.clearFocus()                    //提交搜索commit后收起键盘
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            launch(UI) {
                deffered?.cancel()
                deffered = async { searchViewModel?.onQueryTextChange(newText) }
                suggestCursor = deffered?.await()
                searchViewBar.suggestionsAdapter.changeCursor(suggestCursor)
            }
            return true
        }
    }

    class SearchViewAdapter(context: Context, cursor: Cursor?) : CursorAdapter(
            context, cursor,
            true
    ) {
        override fun newView(context: Context, cursor: Cursor?, parent: ViewGroup?): View {
            return LayoutInflater.from(context).inflate(R.layout.item_search_suggest, parent, false)
        }

        override fun bindView(view: View, context: Context?, cursor: Cursor?) {
            (view as TextView).text = cursor?.getString(0)
        }
    }

    inner class SuggestionListener : SearchView.OnSuggestionListener {
        override fun onSuggestionClick(position: Int): Boolean {
            if (suggestCursor?.moveToPosition(position) == true) {
                searchViewBar.setQuery(suggestCursor?.getString(0), true)
                job?.cancel()
                job = launch { searchViewModel?.searchBook(suggestCursor?.getString(0), null) }
            }
            return true
        }

        override fun onSuggestionSelect(position: Int): Boolean {
            return true
        }
    }
}