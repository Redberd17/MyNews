package com.chugunova.mynews.data.model

import android.os.Parcelable
import com.chugunova.mynews.utils.FilterVariants
import com.chugunova.mynews.utils.LayoutVariants
import com.chugunova.mynews.utils.SortVariants
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class SavedRotationModel(
    val articles: @RawValue ArrayList<Article>,
    val availablePages: Int,
    val currentCountryPage: Int,
    val currentSearchPage: Int,
    val savedQuery: String,
    val savedSortByParameter: SortVariants,
    val isSearch: Boolean,
    val isFilter: Boolean,
    val savedFilterParameter: FilterVariants?,
    val currentLayoutVariant: LayoutVariants
) : Parcelable