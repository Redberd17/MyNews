package com.chugunova.mynews.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chugunova.mynews.R
import com.chugunova.mynews.viewmodel.NewsAllFragmentFactory
import com.chugunova.mynews.viewmodel.NewsAllFragmentViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.add_user_news_fragment.*
import kotlinx.coroutines.launch


class AddUserNewsFragment : Fragment() {

    private lateinit var mNewsAllFragmentViewModel: NewsAllFragmentViewModel

    private val layout = R.layout.add_user_news_fragment

    companion object {
        fun newInstance() = AddUserNewsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNewsAllFragmentViewModel = ViewModelProvider(requireActivity(),
                NewsAllFragmentFactory(requireActivity().application)
        )[NewsAllFragmentViewModel::class.java]
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enterTitleText.addTextChangedListener(mWatcher)
        enterDescriptionText.addTextChangedListener(mWatcher)
        enterUrlText.addTextChangedListener(mWatcher)
        enterUrlToImageText.addTextChangedListener(mWatcher)

        saveButton.setOnClickListener {
            lifecycleScope.launch {
                mNewsAllFragmentViewModel.saveUserNews(
                        enterTitleText.text.toString(),
                        enterDescriptionText.text.toString(),
                        enterUrlText.text.toString(),
                        enterUrlToImageText.text.toString())
            }
            requireActivity().onBackPressed()
        }

    }

    private var mWatcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            enableOrDisableButton()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable) {}
    }

    fun enableOrDisableButton(): Boolean {

        val validateTitle = enterTitleText.text.toString().isNotEmpty()
        val validateDescription = enterDescriptionText.text.toString().isNotEmpty()
        val validateUrl = enterUrlText.text.toString().isNotEmpty()
        val validateUrlToImage = enterUrlToImageText.text.toString().isNotEmpty()

        setError(validateTitle, enterTitle, enterTitleText, resources.getString(R.string.validate_title))
        setError(validateDescription, enterDescription, enterDescriptionText, resources.getString(R.string.validate_description))
        setError(validateUrl, enterUrl, enterUrlText, resources.getString(R.string.validate_url))
        setError(validateUrlToImage, enterUrlToImage, enterUrlToImageText, resources.getString(R.string.validate_url_to_image))

        return if (validateTitle && validateDescription && validateUrl && validateUrlToImage) {
            saveButton.isEnabled = true
            true
        } else {
            saveButton.isEnabled = false
            false
        }
    }

    private fun setError(validate: Boolean, layout: TextInputLayout, editText: TextInputEditText, textError: String?) {
        if (validate) {
            layout.error = null
        } else if (!validate && editText.isFocused) {
            layout.error = textError
        }
    }
}