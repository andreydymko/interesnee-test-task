package com.andreydymko.testtask.ui.imageview

import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import com.andreydymko.testtask.R
import com.andreydymko.testtask.ui.animations.AnimationCompat
import com.google.android.material.snackbar.Snackbar

/**
 * A simple [Fragment] subclass with [WebView] as its main [View].
 *
 * Will load Web-page by given URL in it's inner [WebView] and show it to the user.
 * Will also show [title] and [subtitle] in [Toolbar] if corresponding arguments provided.
 *
 * **Fragments inner WebView has `JavaScriptEnabled` set to `true`.**
 *
 * Use the [WebViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WebViewFragment : Fragment() {
    private lateinit var unknownErrorStr: String
    private lateinit var webView: WebView

    private var shortAnimationDuration = 0L

    private var loadingSpinner: ProgressBar? = null
    private var toolbar: Toolbar? = null
    private var urlToLoad: String? = null
    private var title: String? = null
    private var subtitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shortAnimationDuration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        unknownErrorStr = resources.getString(R.string.unknown_error)

        arguments?.let {
            urlToLoad = it.getString(ARG_URL_TO_LOAD)
            title = it.getString(ARG_TITLE_TO_SHOW)
            subtitle = it.getString(ARG_SUBTITLE_TO_SHOW)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_web_view, container, false)

        toolbar = view.findViewById(R.id.web_view_toolbar)
        setupToolbar()

        loadingSpinner = view.findViewById(R.id.web_view_loading_spinner)
        webView = view.findViewById(R.id.image_web_view)
        webView.webViewClient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WebViewClientAPI23Higher()
        } else {
            WebViewClientAPI23Lower()
        }
        with(webView.settings) {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        urlToLoad?.let {
            webView.loadUrl(urlToLoad)
        }
        return view
    }

    private fun setupToolbar() {
        toolbar?.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        toolbar?.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        title?.let {
            toolbar?.title = it
        }
        subtitle?.let {
            toolbar?.subtitle = it
        }
    }

    companion object {
        private const val ARG_URL_TO_LOAD = "ARG_URL_TO_LOAD"
        private const val ARG_TITLE_TO_SHOW = "ARG_TITLE_TO_LOAD"
        private const val ARG_SUBTITLE_TO_SHOW = "ARG_SUBTITLE_TO_LOAD"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param urlToLoad Specifies URL to load into WebView.
         * @param title Specifies title to be shown in toolbar
         * @param subtitle Specifies subtitle to be shown in toolbar
         * @return A new instance of fragment WebViewFragment.
         */
        @JvmStatic
        fun newInstance(urlToLoad: String, title: String?, subtitle: String? = null) =
            WebViewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL_TO_LOAD, urlToLoad)
                    putString(ARG_TITLE_TO_SHOW, title)
                    putString(ARG_SUBTITLE_TO_SHOW, subtitle)
                }
            }
    }

    private open inner class WebViewClientLocal : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (webView.visibility != View.VISIBLE) {
                AnimationCompat.crossFade(webView, loadingSpinner, shortAnimationDuration)
            }
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            super.onReceivedSslError(view, handler, error)
            if (error != null) {
                showErrorSnackbar(getString(R.string.ssl_error, error.primaryError))
            }
        }
    }

    private inner class WebViewClientAPI23Lower : WebViewClientLocal() {
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            showErrorSnackbar("$description ($errorCode)")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private inner class WebViewClientAPI23Higher : WebViewClientLocal() {
        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)

            val addInfo = if (error != null) {
                "${error.description} (${error.errorCode})"
            } else {
                unknownErrorStr
            }
            showErrorSnackbar(addInfo)
        }
    }

    private fun showErrorSnackbar(addInfo: String) {
        if (view == null || context == null) {
            return
        }
        Snackbar.make(
            requireContext(),
            requireView(),
            resources.getString(R.string.web_view_load_error, addInfo),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(getString(R.string.action_dismiss)) {
            // A snackbar is dismissed by default when clicking the action
        }.show()
    }
}