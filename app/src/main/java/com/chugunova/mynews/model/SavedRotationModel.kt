package com.chugunova.mynews.model

import android.os.Parcelable
import com.chugunova.mynews.utils.FilterVariants
import com.chugunova.mynews.utils.LayoutVariants
import com.chugunova.mynews.utils.SortVariants
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class SavedRotationModel(
    var articles: @RawValue ArrayList<Article>,
    var currentCountryPage: Int,
    var currentSearchPage: Int,
    var savedQuery: String,
    var isSearch: Boolean,
    var isFilter: Boolean,
    var savedSortByParameter: SortVariants,
    var savedFilterParameter: FilterVariants,
    var currentLayoutVariant: LayoutVariants
) : Parcelable