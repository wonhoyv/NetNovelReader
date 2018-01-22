package com.netnovelreader.reader

import android.app.AlertDialog
import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import com.netnovelreader.BR
import com.netnovelreader.R
import com.netnovelreader.base.IClickEvent
import com.netnovelreader.common.BindingAdapter
import com.netnovelreader.common.NovelItemDecoration
import com.netnovelreader.data.database.SQLHelper
import com.netnovelreader.databinding.ActivityReaderBinding
import kotlinx.android.synthetic.main.activity_reader.*
import kotlinx.android.synthetic.main.item_catalog.view.*

class ReaderActivity : AppCompatActivity(), IReaderContract.IReaderView, GestureDetector.OnGestureListener,
        ReaderView.FirstDrawListener, IClickEvent {
    var mViewModel: ReaderViewModel? = null
    var detector: GestureDetector? = null
    var dialog: AlertDialog? = null
    val MIN_MOVE = 80F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setViewModel(ReaderViewModel(intent.getStringExtra("bookname")))
        init()
    }

    override fun init() {
        readerView.background = getDrawable(R.drawable.bg_readbook_yellow)
        readerView.setFirstDrawListener(this)
        readerView.txtFontSize = 50f
        detector = GestureDetector(this, this)
        catalogButton.setOnClickListener {
            footView.visibility = View.INVISIBLE
            showDialog()
        }
        fontSizeButton.setOnClickListener {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel = null
        SQLHelper.closeDB()
    }

    override fun setViewModel(vm: ReaderViewModel) {
        mViewModel = vm
        val binding = DataBindingUtil.setContentView<ActivityReaderBinding>(this, R.layout.activity_reader)
        binding.setVariable(BR.chapter, mViewModel)
    }

    /**
     * readerview第一次绘制时调用
     */
    override fun doDrawPrepare() {
        mViewModel?.initData(readerView.width, readerView.height, readerView.txtFontSize)
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (footView.visibility == View.VISIBLE) {
            footView.visibility = View.INVISIBLE
            return false
        }
        val beginX = e1.x
        val endX = e2.x
        if (Math.abs(beginX - endX) < MIN_MOVE) {
            return false
        } else {
            if (beginX > endX) {
                mViewModel!!.pageToNext(readerView.width, readerView.height, readerView.txtFontSize)
            } else {
                mViewModel!!.pageToPrevious(readerView.width, readerView.height, readerView.txtFontSize)
            }
        }
        return false
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (footView.visibility == View.VISIBLE) {
            footView.visibility = View.INVISIBLE
            return false
        }
        val x = e.x
        val y = e.y
        if (y < readerView.height * 2 / 5 || y > readerView.height * 3 / 5) {
            if (x > readerView.width / 2) {
                mViewModel!!.pageToNext(readerView.width, readerView.height, readerView.txtFontSize)
            } else {
                mViewModel!!.pageToPrevious(readerView.width, readerView.height, readerView.txtFontSize)
            }
        } else {
            if (x > readerView.width * 3 / 5) {
                mViewModel!!.pageToNext(readerView.width, readerView.height, readerView.txtFontSize)
            } else if (x < readerView.width * 2 / 5) {
                mViewModel!!.pageToPrevious(readerView.width, readerView.height, readerView.txtFontSize)
            } else {
                if (footView.visibility == View.INVISIBLE) {
                    footView.visibility = View.VISIBLE
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return detector!!.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun showDialog() {
        var catalogView: RecyclerView? = null
        if (dialog == null) {
            val builder = AlertDialog.Builder(this)
            val view = LayoutInflater.from(this).inflate(R.layout.fragment_catalog, null)
            catalogView = view.findViewById<RecyclerView>(R.id.catalogView)
            catalogView.layoutManager = LinearLayoutManager(this)
            catalogView.addItemDecoration(NovelItemDecoration(this))
            catalogView.setItemAnimator(DefaultItemAnimator())
            catalogView.adapter = BindingAdapter(mViewModel?.catalog, R.layout.item_catalog,
                    CatalogItemClickListener())
            dialog = builder.setView(view).create()
        }
        mViewModel?.updateCatalog()
        catalogView?.adapter?.notifyDataSetChanged()
        dialog?.show()
    }

    inner class CatalogItemClickListener : IClickEvent {
        fun onChapterClick(v: View) {
            mViewModel?.pageByCatalog(v.itemChapter.text.toString(), readerView.width, readerView.height,
                    readerView.txtFontSize)
            dialog?.dismiss()
        }
    }
}
