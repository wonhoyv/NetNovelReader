package com.netnovelreader.common

import android.databinding.ObservableArrayList
import android.databinding.ObservableList
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

/**
 * Created by yangbo on 2018/1/24.
 */
class ArrayListChangeListener<T>(private val adapter: BindingAdapter<T>) :
    ObservableList.OnListChangedCallback<ObservableArrayList<T>>() {

    override fun onChanged(p0: ObservableArrayList<T>?) {
        notifyDataSetChanged()
    }

    override fun onItemRangeChanged(p0: ObservableArrayList<T>?, p1: Int, p2: Int) {
        notifyDataSetChanged()
    }

    override fun onItemRangeInserted(p0: ObservableArrayList<T>?, p1: Int, p2: Int) {
        notifyDataSetChanged()
    }

    override fun onItemRangeMoved(p0: ObservableArrayList<T>?, p1: Int, p2: Int, p3: Int) {
        notifyDataSetChanged()
    }

    override fun onItemRangeRemoved(p0: ObservableArrayList<T>?, p1: Int, p2: Int) {
        notifyDataSetChanged()
    }

    fun notifyDataSetChanged() {
        launch(UI){
            adapter.notifyDataSetChanged()
        }
    }
}