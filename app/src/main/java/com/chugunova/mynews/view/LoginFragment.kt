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
import kotlinx.android.synthetic.main.login_fragment.*
import kotlinx.coroutines.launch


class LoginFragment : Fragment() {

    private lateinit var mNewsAllFragmentViewModel: NewsAllFragmentViewModel

    private val layout = R.layout.login_fragment

    companion object {
        fun newInstance() = LoginFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNewsAllFragmentViewModel = ViewModelProvider(requireActivity(),
                NewsAllFragmentFactory(requireActivity().application)
        )[NewsAllFragmentViewModel::class.java]
        mNewsAllFragmentViewModel.userLiveData.observe(this, {
            it.getContentIfNotHandled()?.let {
                requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container, NewsAllFragment.newInstance())
                        .commit()
            }
        })
        mNewsAllFragmentViewModel.toastLiveData.observe(this, { it ->
            it.getContentIfNotHandled()?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
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

        enterNameText.addTextChangedListener(mWatcher)
        enterPasswordText.addTextChangedListener(mWatcher)

        loginButton.setOnClickListener {
            lifecycleScope.launch {
                if (enterNameText.text.toString().isNotEmpty() && enterPasswordText.text.toString().isNotEmpty()) {
                    val authUser = AuthenticationUser(enterNameText.text.toString(), enterPasswordText.text.toString())
                    mNewsAllFragmentViewModel.login(authUser)
                }
            }
        }

        createAccountButton.setOnClickListener {
            requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, CreateAccountFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
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

        val validateName = enterNameText.text.toString().isNotEmpty()
        val validatePassword = enterPasswordText.text.toString().isNotEmpty()

        setError(validateName, enterName, enterNameText, resources.getString(R.string.validate_name))
        setError(validatePassword, enterPassword, enterPasswordText, resources.getString(R.string.validate_password))

        return if (validateName && validatePassword) {
            loginButton.isEnabled = true
            true
        } else {
            loginButton.isEnabled = false
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