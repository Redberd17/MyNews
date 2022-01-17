package com.chugunova.mynews.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chugunova.mynews.model.SavedRotationModel

class NewsAllFragmentFactory(
    private val application: Application,
    private var savedRotationModel: SavedRotationModel
) : ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NewsAllFragmentViewModel(
            application
        ) as T
    }
}