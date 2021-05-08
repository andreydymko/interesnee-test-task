package com.andreydymko.testtask.ui.imageview

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import androidx.viewpager2.widget.ViewPager2
import com.andreydymko.testtask.R
import com.andreydymko.testtask.serpapi.ImageDescription
import com.andreydymko.testtask.ui.abstracts.FullscreenContainerManagingFragment
import com.andreydymko.testtask.ui.animations.AnimationCompat
import com.andreydymko.testtask.ui.viewmodel.ImageListViewModel
import com.andreydymko.testtask.ui.viewmodel.ViewModelCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar

/**
 * A [FullscreenContainerManagingFragment] subclass,
 * contains [ViewPager2] as main view element.
 *
 * Observes [ImageListViewModel] and caching results in inner [ArrayList].
 * Then using this `ArrayList` as `ViewPager2`'s data container.
 * Also displays toolbar with [ImageDescription.title] as title
 * and [ImageDescription.sourceWebsite] as subtitle.
 *
 * Toolbar contains "Open in browser" button. When clicked - fragment will replace itself
 * with [WebViewFragment] while adding itself to backstack.
 *
 * Utilizes [FragmentStateAdapter] and [ImageViewFragment] to show pages.
 *
 * Supports fragments shared element transition
 * ([link](https://developer.android.com/guide/fragments/animate#shared)).
 *
 * Will automatically request new pages from `ViewModel` if needed.
 *
 * Will create [Snackbar] with error message if `ViewModel` specified one.
 * And show it when the last page is visible.
 *
 * Use the [ImagePagerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImagePagerFragment : FullscreenContainerManagingFragment() {
    private val viewModel: ImageListViewModel by activityViewModels()
    private val hideHandler = Handler()
    private val imageDescs: ArrayList<ImageDescription> = ArrayList()

    private var isNavigationVisible = false
    private var shortAnimationDuration = 0L
    private var navigationAnimationDelay = 0L

    private var toolbarProgressBar: ProgressBar? = null
    private var viewPager: ViewPager2? = null
    private var pagerAdapter: ScreenSlidePagerAdapter? = null
    private var toolbar: Toolbar? = null
    private var toolbarContainer: AppBarLayout? = null
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shortAnimationDuration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        navigationAnimationDelay =
            resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()

        arguments?.getBoolean(ARG_IS_FRAGMENT_MANAGING_TOOLBAR, false)?.let {
            isManagingToolbar = it
            isManagingContainer = it
        }

        setPages(viewModel.imagesData.value?.someObject)
        prepTransition()
    }

    private fun prepTransition() {
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.shared_enter_image)

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>?,
                sharedElements: MutableMap<String, View>?
            ) {
                viewPager?.let {
                    // getChildAt(0) returns inner RecyclerView of ViewPager2
                    // dirty trick, probably
                    // will throw cast exception, if it isn't working anymore
                    val currViewHolder = (it.getChildAt(0) as RecyclerView)
                        .findViewHolderForAdapterPosition(viewModel.currentUserPosition)
                    if (names.isNullOrEmpty() || currViewHolder == null) {
                        return
                    }

                    sharedElements?.put(
                        names[0],
                        currViewHolder.itemView.findViewById(R.id.pager_image_view)
                    )
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        viewPager?.unregisterOnPageChangeCallback(onPageChangeCallback)
    }

    override fun onResume() {
        super.onResume()
        // Request a new portion of images if needed, even if user hasn't swiped any page yet
        if (checkToRequestPage(viewModel.currentUserPosition)) {
            requestNewPage()
        }
    }

    override fun onPause() {
        super.onPause()
        snackbar?.dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image_pager, container, false)
        viewPager = view.findViewById(R.id.image_view_pager)
        setupPager()

        toolbarProgressBar = view.findViewById(R.id.image_pager_progressbar)
        setupViewModel()
        updateProgressBarState(viewModel.isLoading.value?.get())

        // Avoid a postponeEnterTransition on orientation change, and postpone only on first creation.
        if (savedInstanceState == null) {
            postponeEnterTransition()
        }
        toolbar = view.findViewById(R.id.image_pager_toolbar)
        setupToolbar()
        toolbarContainer = view.findViewById(R.id.image_pager_toolbar_container)

        snackbar = ViewModelCompat.setupRetrySnackbar(requireContext(), view, viewModel)

        return view
    }

    private fun setupPager() {
        viewPager?.apply {
            pagerAdapter = ScreenSlidePagerAdapter(this@ImagePagerFragment)

            adapter = pagerAdapter
            setCurrentItem(viewModel.currentUserPosition, false)

            registerOnPageChangeCallback(onPageChangeCallback)
        }
    }

    private fun setupViewModel() {
        viewModel.imagesData.observe(viewLifecycleOwner) { imgDescs ->
            setPages(imgDescs.someObject)
            invalidateSnackBar()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            updateProgressBarState(isLoading.get())
        }
    }

    private fun updateProgressBarState(state: Boolean?) {
        // show progress bar only on the last page
        if (state == true && isLastPageReached()) {
            if (toolbarProgressBar?.visibility != View.VISIBLE) {
                AnimationCompat.showByAlpha(toolbarProgressBar, shortAnimationDuration)
            }
        } else {
            if (toolbarProgressBar?.visibility != View.GONE) {
                AnimationCompat.hideByAlpha(toolbarProgressBar, shortAnimationDuration)
            }
        }
    }



    private fun invalidateSnackBar() {
        // show SnackBar only on the last page
        if (isLastPageReached()) {
            val excMsg = viewModel.imagesData.value?.exception?.message ?: return
            snackbar?.setText(excMsg)
            if (snackbar?.isShownOrQueued == false) {
                snackbar?.show()
            }
        } else {
            if (snackbar?.isShownOrQueued == true) {
                snackbar?.dismiss()
            }
        }
    }

    private fun isLastPageReached(): Boolean {
        return viewPager?.currentItem == (imageDescs.size - 1)
    }

    private fun setupToolbar() {
        toolbar?.inflateMenu(R.menu.image_view_menu)
        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_image_open_browser -> {
                    openWebView()
                    true
                }
                else -> false
            }
        }

        toolbar?.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        toolbar?.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        updateToolbarInfo(viewPager?.currentItem ?: 0)
    }

    private fun openWebView() {
        val imgDesc = imageDescs[viewPager!!.currentItem]
        val fragment = WebViewFragment
            .newInstance(
                imgDesc.sourcePage.toString(),
                imgDesc.sourceWebsite.toString(),
                imgDesc.title
            )
        parentFragmentManager.commit {
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            replace(getFragmentContainerId(), fragment)
            addToBackStack(null)
        }
    }

    @IdRes
    private fun getFragmentContainerId(): Int {
        return (view?.parent as ViewGroup).id
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showNavigationDelayed()
    }

    private fun toggleNavigationVisibility() {
        if (isNavigationVisible) {
            hideNavigationDelayed()
        } else {
            showNavigationDelayed()
        }
    }

    private val showRunnable = Runnable { showNavigation() }

    private fun showNavigationDelayed() {
        hideHandler.removeCallbacks(showRunnable)
        hideHandler.postDelayed(showRunnable, navigationAnimationDelay)
    }

    private val hideRunnable = Runnable { hideNavigation() }

    private fun hideNavigationDelayed() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, navigationAnimationDelay)
    }

    private fun showNavigation(animate: Boolean = true) {
        isNavigationVisible = true
        if (animate) {
            AnimationCompat.showByAlpha(toolbarContainer, shortAnimationDuration)
        } else {
            toolbarContainer?.visibility = View.VISIBLE
        }
    }

    private fun hideNavigation(animate: Boolean = true) {
        isNavigationVisible = false
        if (animate) {
            AnimationCompat.hideByAlpha(toolbarContainer, shortAnimationDuration)
        } else {
            toolbarContainer?.visibility = View.GONE
        }
    }

    private fun setPages(list: List<ImageDescription>?) {
        list?.let {
            imageDescs.clear()
            imageDescs.addAll(it)
        }
    }

    private fun checkToRequestPage(position: Int): Boolean {
        return position + PAGES_BEFORE_START_LOADING >= imageDescs.size
    }

    private fun requestNewPage() {
        viewModel.loadPageBlocking()
    }

    private inner class ScreenSlidePagerAdapter(fa: Fragment) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = imageDescs.size

        override fun createFragment(position: Int): Fragment =
            ImageViewFragment.newInstance(imageDescs[position])

        override fun onBindViewHolder(
            holder: FragmentViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            // we can't just invoke ViewPager2.setOnClickListener(), because clicks will be
            // intercepted by ViewPager's GestureDetector. So, we setting click listener on
            // every view of ViewPager.
            // We don't actually need this amount of ClickListeners, for simple toolbar visibility
            // 'toggler', but here we are.
            holder.itemView.setOnClickListener {
                toggleNavigationVisibility()
            }
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)

            updateToolbarInfo(position)
            viewModel.currentUserPosition = position
            if (checkToRequestPage(position)) {
                requestNewPage()
            }
        }
    }

    private fun updateToolbarInfo(currPos: Int) {
        imageDescs[currPos].let {
            toolbar?.title = it.title
            toolbar?.subtitle = it.sourceWebsite.toString()
        }
    }

    companion object {
        private val TAG = ImagePagerFragment::class.java.simpleName

        /**
         * Number of pages left before new pages should be requested.
         *
         * Takes part in this formula:
         *
         * `userPosition + PAGES_BEFORE_START_LOADING >= imageDescs.size'
         */
        private const val PAGES_BEFORE_START_LOADING = 5

        private const val ARG_IS_FRAGMENT_MANAGING_TOOLBAR = "ARG_IS_FRAGMENT_MANAGING_TOOLBAR"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param isManagingToolbar Should this fragment manage toolbar state or not.
         * @return A new instance of fragment [ImageViewFragment].
         * @see [FullscreenContainerManagingFragment]
         */
        @JvmStatic
        fun newInstance(isManagingToolbar: Boolean) =
            ImagePagerFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_FRAGMENT_MANAGING_TOOLBAR, isManagingToolbar)
                }
            }
    }
}