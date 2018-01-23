package com.netnovelreader.search

import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.ObservableArrayList
import android.databinding.ObservableList
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import com.netnovelreader.R
import com.netnovelreader.base.IClickEvent
import com.netnovelreader.common.BindingAdapter
import com.netnovelreader.common.NovelItemDecoration
import com.netnovelreader.databinding.ActivitySearchBinding
import com.netnovelreader.service.DownloadService
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.item_search.view.*

class SearchActivity : AppCompatActivity(), ISearchContract.ISearchView {
    var searchViewModel: SearchViewModel? = null
    var arrayListChangeListener = ArrayListChangeListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setViewModel(SearchViewModel())
        init()
    }

    override fun setViewModel(vm: SearchViewModel) {
        searchViewModel = vm
        DataBindingUtil.setContentView<ActivitySearchBinding>(this, R.layout.activity_search)
    }

    override fun init() {
        searchRecycler.layoutManager = LinearLayoutManager(this)
        searchRecycler.adapter = BindingAdapter(searchViewModel?.resultList,
                R.layout.item_search, SearchClickEvent())
        searchRecycler.setItemAnimator(DefaultItemAnimator())
        searchRecycler.addItemDecoration(NovelItemDecoration(this))
        searchViewModel?.resultList?.addOnListChangedCallback(arrayListChangeListener)
        //搜索事件监听
        search_bar.setOnQueryTextListener(QueryListener())
    }

    override fun onDestroy() {
        super.onDestroy()
        searchViewModel?.resultList?.removeOnListChangedCallback(arrayListChangeListener)
        searchViewModel = null
    }

    inner class QueryListener : SearchView.OnQueryTextListener{
        var tmp = ""
        var tmpTime = System.currentTimeMillis()

        override fun onQueryTextSubmit(query: String): Boolean {
            if (tmp == query && System.currentTimeMillis() - tmpTime < 1000) return true  //点击间隔小于1秒，并且搜索书名相同不再搜索
            if (query.length > 0) {
                searchViewModel?.searchBook(query)
                tmp = query
                tmpTime = System.currentTimeMillis()
            }
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            return true
        }
    }

    inner class SearchClickEvent : IClickEvent {
        fun downloadBook(v: View) {
            if (v.resultName.text.toString().length > 0 && v.resultUrl.text.toString().length > 0) {
                val tableName = searchViewModel!!.addBookToShelf(v.resultName.text.toString(), v.resultUrl.text.toString())
                Toast.makeText(this@SearchActivity, R.string.start_download, Toast.LENGTH_SHORT).show()
                val intent = Intent(v.context, DownloadService::class.java)
                intent.putExtra("tableName", tableName)
                intent.putExtra("catalogurl", v.resultUrl.text.toString())
                startService(intent)
            }
        }
    }

    inner class ArrayListChangeListener : ObservableList.OnListChangedCallback<ObservableArrayList<SearchBean>>() {
        override fun onChanged(p0: ObservableArrayList<SearchBean>?) {
            notifyDataSetChanged()
        }

        override fun onItemRangeChanged(p0: ObservableArrayList<SearchBean>?, p1: Int, p2: Int) {
            notifyDataSetChanged()
        }

        override fun onItemRangeInserted(p0: ObservableArrayList<SearchBean>?, p1: Int, p2: Int) {
            notifyDataSetChanged()
        }

        override fun onItemRangeMoved(p0: ObservableArrayList<SearchBean>?, p1: Int, p2: Int, p3: Int) {
            notifyDataSetChanged()
        }

        override fun onItemRangeRemoved(p0: ObservableArrayList<SearchBean>?, p1: Int, p2: Int) {
            notifyDataSetChanged()
        }

        fun notifyDataSetChanged() {
            runOnUiThread { searchRecycler.adapter.notifyDataSetChanged() }
        }
    }
}
