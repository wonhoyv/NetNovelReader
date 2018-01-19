package com.netnovelreader.reader

import android.view.GestureDetector
import android.view.MotionEvent

/**
 * Created by yangbo on 2018/1/19.
 */
class ReaderViewListener(val mViewModel: ReaderViewModel) :
        GestureDetector.OnGestureListener, ReaderView.FirstDrawListener{
    /**
     * 最小滑动距离
     */
    val MIN_MOVE = 80F

    var pageIndicator = IntArray(4){ it -> 1 } //例如1,1,4,5 == 第一章第一页,总共4章，这一章有5页

    var width = 0
    var height = 0
    var txtFontSize = 0F

    /**
     * 初始化数据，在第一次ReaderView.onDraw（）时调用
     */
    override fun doDrawPrepare(width: Int, height: Int, txtFontSize: Float) {
        this.width = width
        this.height = height
        this.txtFontSize = txtFontSize
        pageIndicator[2] = mViewModel.getChapterCount()
        val array = mViewModel.getRecord()
        pageIndicator[0] = array[0]
        pageIndicator[1]  = array[1]
        mViewModel.getChapterTxt(pageIndicator,0, width, height, txtFontSize)
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    @Synchronized
    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val beginX = e1.getX()
        val endX = e2.getX()
        if(Math.abs(beginX - endX) < MIN_MOVE) {
            return false
        }else{
            if (beginX > endX) {
                if(!pageNext(pageIndicator)) return false
            } else {
                if(!pagePrevious(pageIndicator)) return false
            }
        }
        return false
    }

    override fun onLongPress(e: MotionEvent) {

    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {

        return false
    }

    override fun onShowPress(e: MotionEvent) {

    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {

        return false
    }

    /**
     * 向下翻页
     */
    fun pageNext(pageIndicator: IntArray): Boolean{
        if (pageIndicator[1] == pageIndicator[3] || pageIndicator[3] < 2){
            if(pageIndicator[0] == pageIndicator[2]){
                return false
            }else{
                //下一章
                pageIndicator[0] += 1
                mViewModel.getChapterTxt(pageIndicator,1, width, height, txtFontSize)
            }
        }else{
            //同一章下一页
            pageIndicator[1] += 1
            mViewModel.updateTextAndRecord(pageIndicator)
        }
        return true
    }

    /**
     * 向上翻页
     */
    fun pagePrevious(pageIndicator: IntArray): Boolean{
        if (pageIndicator[1] < 2){
            if(pageIndicator[0] == 1){
                return false
            }else{
                //上一章
                pageIndicator[0] -= 1
                mViewModel.getChapterTxt(pageIndicator,-1, width, height, txtFontSize)
            }
        }else{
            //同一章上一页
            pageIndicator[1] -= 1
            mViewModel.updateTextAndRecord(pageIndicator)
        }
        return true
    }

}