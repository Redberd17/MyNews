package com.chugunova.mynews.model

import com.chugunova.mynews.utils.FilterVariants
import com.chugunova.mynews.utils.LayoutVariants
import com.chugunova.mynews.utils.SortVariants
import java.io.Serializable

data class SavedRotationModel(
    val articles: ArrayList<Article>,
    val availablePages: Int,
    val currentCountryPage: Int,
    val currentSearchPage: Int,
    val savedQuery: String,
    val savedSortByParameter: SortVariants,
    val isSearch: Boolean,
    val isFilter: Boolean,
    val savedFilterParameter: FilterVariants?,
    val currentLayoutVariant: LayoutVariants
) : Serializable