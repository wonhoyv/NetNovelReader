package com.netnovelreader.search

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import com.netnovelreader.R
import com.netnovelreader.base.IClickEvent
import com.netnovelreader.common.ArrayListChangeListener
import com.netnovelreader.common.BindingAdapter
import com.netnovelreader.common.NovelItemDecoration
import com.netnovelreader.databinding.ActivitySearchBinding
import com.netnovelreader.service.DownloadService
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.item_search.view.*

class SearchActivity : AppCompatActivity(), ISearchContract.ISearchView {
    var searchViewModel: SearchViewModel? = null
    var arrayListChangeListener: ArrayListChangeListener<SearchBean>?= null

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
//        searchToolbar.navigationIcon = getDrawable(R.drawable.icon_back)
//        setSupportActionBar(searchToolbar)
//        searchToolbar.setNavigationOnClickListener { this@SearchActivity.finish() }
        searchRecycler.layoutManager = LinearLayoutManager(this)
        var mAdapter = BindingAdapter(searchViewModel?.resultList, R.layout.item_search, SearchClickEvent())
        searchRecycler.adapter = mAdapter
        searchRecycler.setItemAnimator(DefaultItemAnimator())
        searchRecycler.addItemDecoration(NovelItemDecoration(this))
        arrayListChangeListener = ArrayListChangeListener(mAdapter)
        searchViewModel?.resultList?.addOnListChangedCallback(arrayListChangeListener)
        searchViewBar.setOnQueryTextListener(QueryListener())
        searchViewBar.isIconified = false
        searchViewBar.onActionViewExpanded()
    }

    override fun onDestroy() {
        super.onDestroy()
        searchViewModel?.resultList?.removeOnListChangedCallback(arrayListChangeListener)
        searchViewModel = null
    }
//
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        var searchMenu = menuInflater.inflate(R.menu.menu_search, menu)
////        //搜索事件监听
//        var searchViewBar = findViewById<SearchView>(R.id.searchViewBar)
//        return true
//    }
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.search_button -> {
//                startActivity(Intent(this, SearchActivity::class.java))
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }


    inner class QueryListener : android.support.v7.widget.SearchView.OnQueryTextListener{
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
}
