package com.chugunova.mynews.view

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.*
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chugunova.mynews.R
import com.chugunova.mynews.mainscreenfragment.NewsAdapter
import com.chugunova.mynews.model.Article
import com.chugunova.mynews.view.NewsAllFragment.Companion.SAVED_NEWS
import com.chugunova.mynews.viewmodel.NewsAllFragmentFactory
import com.chugunova.mynews.viewmodel.NewsAllFragmentViewModel
import kotlinx.coroutines.launch

class NewsDetailFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var menuProgressBar: MenuItem
    private lateinit var menuEditNews: MenuItem
    private lateinit var menuDeleteNews: MenuItem

    private lateinit var ad: AlertDialog.Builder

    private lateinit var mNewsAllFragmentViewModel: NewsAllFragmentViewModel

    private val newsAdapter: NewsAdapter = NewsAdapter { }

    private var authorIsEquals: Boolean = false
    private var newsItem: Article? = null

    companion object {
        fun newInstance() = NewsDetailFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        newsItem = arguments?.getParcelable(NewsAllFragment.EDIT_NEWS)
        authorIsEquals = arguments?.getBoolean(NewsAllFragment.AUTHOR_IS_EQUALS) == true
        mNewsAllFragmentViewModel = ViewModelProvider(requireActivity(),
                NewsAllFragmentFactory(requireActivity().application)
        )[NewsAllFragmentViewModel::class.java]
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fullscreen_news_fragment, container, false)
        webView = view.findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val url = newsItem?.url
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webSettings.allowFileAccess = true
        webSettings.databaseEnabled = true
        webSettings.cacheMode =
                if (isOnline()) {
                    WebSettings.LOAD_DEFAULT
                } else {
                    WebSettings.LOAD_CACHE_ELSE_NETWORK
                }
        url?.let {
            webView.loadUrl(it)
        }
    }

    private fun showMenuIcons() {
        menuEditNews.isVisible = true
        menuDeleteNews.isVisible = true
    }

    private fun hideMenuIcons() {
        menuEditNews.isVisible = false
        menuDeleteNews.isVisible = false
    }

    private fun isOnline(): Boolean {
        val cm = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = cm.activeNetwork ?: return false
        val netInfo = cm.getNetworkCapabilities(networkCapabilities)
        return netInfo != null && netInfo.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && netInfo.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_full_screen_news, menu)
        menuProgressBar = menu.findItem(R.id.waiter)
        menuEditNews = menu.findItem(R.id.editNews)
        menuDeleteNews = menu.findItem(R.id.deleteNews)
        menuProgressBar.actionView = ProgressBar(context)
        if (authorIsEquals) {
            showMenuIcons()

            menuEditNews.setOnMenuItemClickListener {
                val bundle = Bundle().apply {
                    putParcelable(SAVED_NEWS, newsItem)
                }
                requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container, AddUserNewsFragment.newInstance().apply { arguments = bundle })
                        .addToBackStack(null)
                        .commit()
                false
            }

            menuDeleteNews.setOnMenuItemClickListener {
                ad = AlertDialog.Builder(requireContext())
                ad.setTitle("Warning")
                ad.setMessage("Do you want to delete this news?")
                ad.setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {
                        newsItem?.let { item -> mNewsAllFragmentViewModel.deleteUserNews(item) }
                        requireActivity().onBackPressed()
                    }
                }
                ad.setNegativeButton("No") { _, _ -> }
                ad.show()
                false
            }
        } else {
            hideMenuIcons()
        }
    }

    inner class WebViewClient : android.webkit.WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            menuProgressBar.isVisible = false
        }
    }
}