package com.chugunova.mynews.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chugunova.mynews.R
import com.chugunova.mynews.model.AuthenticationUser
import com.chugunova.mynews.viewmodel.NewsAllFragmentFactory
import com.chugunova.mynews.viewmodel.NewsAllFragmentViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.create_account_fragment.*
import kotlinx.coroutines.launch


class CreateAccountFragment : Fragment() {

    private lateinit var mNewsAllFragmentViewModel: NewsAllFragmentViewModel

    private val layout = R.layout.create_account_fragment

    companion object {
        fun newInstance() = CreateAccountFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNewsAllFragmentViewModel = ViewModelProvider(requireActivity(),
                NewsAllFragmentFactory(requireActivity().application)
        )[NewsAllFragmentViewModel::class.java]
        mNewsAllFragmentViewModel.toastLiveData.observe(this, { it ->
            it.getContentIfNotHandled()?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        })
        mNewsAllFragmentViewModel.userLiveData.observe(this, {
            it.getContentIfNotHandled()?.let {
                requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container, NewsAllFragment.newInstance())
                        .commit()
            }
        })
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createNameText.addTextChangedListener(mWatcher)
        createPasswordText.addTextChangedListener(mWatcher)

        saveAccount.setOnClickListener {
            lifecycleScope.launch {
                if (createNameText.text.toString().isNotEmpty() && createPasswordText.text.toString().isNotEmpty()) {
                    val authUser = AuthenticationUser(createNameText.text.toString(), createPasswordText.text.toString())
                    mNewsAllFragmentViewModel.saveAccount(authUser)
                }

            }
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

        val validateName = createNameText.text.toString().isNotEmpty()
        val validatePassword = createPasswordText.text.toString().isNotEmpty()

        setError(validateName, createName, createNameText, resources.getString(R.string.validate_name))
        setError(validatePassword, createPassword, createPasswordText, resources.getString(R.string.validate_password))

        return if (validateName && validatePassword) {
            saveAccount.isEnabled = true
            true
        } else {
            saveAccount.isEnabled = false
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