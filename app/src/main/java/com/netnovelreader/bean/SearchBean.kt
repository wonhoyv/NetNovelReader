package com.netnovelreader.bean

import android.databinding.BaseObservable
import android.databinding.ObservableField

/**
 * Created by yangbo on 18-1-14.
 */
data class SearchBean(
    val bookname: ObservableField<String>,
    val url: ObservableField<String>,
    val latestChapter: ObservableField<String?>,
    val catalogMap: LinkedHashMap<String, String>
) : BaseObservable()