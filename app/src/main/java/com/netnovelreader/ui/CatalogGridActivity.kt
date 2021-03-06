package com.netnovelreader.ui

import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.ObservableArrayList
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView.VERTICAL
import android.view.MenuItem
import com.netnovelreader.R
import com.netnovelreader.bean.NovelCatalog
import com.netnovelreader.common.*
import com.netnovelreader.data.network.ApiManager
import com.netnovelreader.databinding.ActivityCatalogGridBinding
import kotlinx.android.synthetic.main.activity_catalog_grid.*

/**
 * 文件： CatalogGridActivity
 * 描述：
 * 作者： YangJunQuan   2018-2-9.
 */
class CatalogGridActivity : AppCompatActivity() {
    private var resultList: ObservableArrayList<NovelCatalog.Bean> = ObservableArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.getThemeId(this).also { setTheme(it) }
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityCatalogGridBinding>(
            this,
            R.layout.activity_catalog_grid
        )

        setSupportActionBar({ toolbar.title = "分类";toolbar }())
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initView()
        initData()
    }

    private fun initView() {
        maleRecyclerView.init(
            RecyclerAdapter(resultList, R.layout.grid_item_novel_classfy, CatalogItemClick()),
            GridDivider(this, 1, Color.BLACK),
            GridLayoutManager(this, 3, VERTICAL, false)
        )
    }

    private fun initData() {
        ApiManager.mAPI.getNovelCatalogData()
            .enqueueCall {
                it?.let {
                    resultList.clear()
                    resultList.addAll(it.male!!)
                    //resultList.addAll(it.female!!)   女生向小说，我想了一下还是屏蔽掉好了
                }
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            android.R.id.home -> finish()   //点击回退按钮结束当前Activity
        }
        return super.onOptionsItemSelected(item)
    }

    inner class CatalogItemClick {
        fun onItemClick(name: String) {
            val intent = Intent(this@CatalogGridActivity, NovelCatalogDetailActivity::class.java)
            intent.putExtra("major", name)
            this@CatalogGridActivity.startActivity(intent)
        }

    }

}