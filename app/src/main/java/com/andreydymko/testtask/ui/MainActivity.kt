package com.andreydymko.testtask.ui

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.andreydymko.testtask.BuildConfig
import com.andreydymko.testtask.R
import com.andreydymko.testtask.serpapi.requester.SerpApiRequester
import com.andreydymko.testtask.ui.animations.AnimationCompat
import com.andreydymko.testtask.ui.imagelist.ImageListFragment
import com.andreydymko.testtask.ui.viewmodel.ImageListViewModel
import java.util.Locale

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    private val imgListViewModel: ImageListViewModel by viewModels()

    private lateinit var toolbarProgressBar: ProgressBar
    private var shortAnimationDuration = 0L

    private var lastQuery: SerpApiRequester.RequestData? = null
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbarProgressBar = findViewById(R.id.main_activity_progressbar)
        shortAnimationDuration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        setSupportActionBar(findViewById(R.id.main_activity_toolbar)).apply {
            title = ""
        }

        setupViewModel()
        createSuggestFragment()
    }

    private fun createSuggestFragment() {
        supportFragmentManager.commit {
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            replace(
                R.id.main_activity_child,
                SuggestSearchFragment.newInstance(),
                SuggestSearchFragment::class.java.simpleName
            )
        }
    }

    private fun setupViewModel() {
        imgListViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading.get()) {
                if (toolbarProgressBar.visibility != View.VISIBLE) {
                    AnimationCompat.showByAlpha(toolbarProgressBar, shortAnimationDuration)
                }
            } else {
                if (toolbarProgressBar.visibility != View.GONE) {
                    AnimationCompat.hideByAlpha(toolbarProgressBar, shortAnimationDuration)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_toolbar_menu, menu)

        val searchView = menu?.findItem(R.id.toolbar_action_search)?.actionView as? SearchView
        this.searchView = searchView?.apply {
            maxWidth = Int.MAX_VALUE // for search view to take up the whole toolbar
            setIconifiedByDefault(false)
            setOnQueryTextListener(this@MainActivity)
            clearFocus()
        }

        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        hideSoftKeyboard()
        // to stop focusing on searchView
        findViewById<View>(R.id.main_activity_main)?.requestFocus()

        if (query == null || query.trim().isEmpty()) {
            return true
        }

        // TODO: set api-key as second parameter
        //  SerpApiRequester.RequestData(query, BuildConfig.SERPAPI_API_KEY)
        val serpQuery = SerpApiRequester.RequestData(query, BuildConfig.SERPAPI_API_KEY)

        if (ConfigurationCompat.getLocales(Resources.getSystem().configuration)
                .get(0) == Locale("ru", "RU")
        ) {
            serpQuery.language = "ru"
            serpQuery.country = "ru"
        }

        if (serpQuery != lastQuery) {
            makeNewSearch(serpQuery)
        }

        return true
    }

    private fun makeNewSearch(query: SerpApiRequester.RequestData) {
        lastQuery = query
        imgListViewModel.requester = SerpApiRequester(query)

        supportFragmentManager.commit {
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            replace(
                R.id.main_activity_child,
                ImageListFragment.newInstance(),
                ImageListFragment::class.java.simpleName
            )
        }
    }

    private fun hideSoftKeyboard() {
        currentFocus?.let { view ->
            val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}