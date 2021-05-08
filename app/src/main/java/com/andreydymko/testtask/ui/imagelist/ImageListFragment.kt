package com.andreydymko.testtask.ui.imagelist

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.IdRes
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import androidx.transition.TransitionSet
import com.andreydymko.testtask.R
import com.andreydymko.testtask.serpapi.ImageDescription
import com.andreydymko.testtask.ui.animations.AnimationCompat
import com.andreydymko.testtask.ui.imageview.ImagePagerFragment
import com.andreydymko.testtask.ui.viewmodel.ImageListViewModel
import com.andreydymko.testtask.ui.viewmodel.ViewModelCompat
import com.google.android.material.snackbar.Snackbar

/**
 * A fragment representing a list of Images.
 * Contains [RecyclerView] as main view element.
 *
 * Observes [ImageListViewModel] and setting result in `RecyclerView`.
 *
 * When user clicks on list item, fragment will replace itself with
 * [ImagePagerFragment] to show fullscreen image, while adding itself to backstack
 * and using shared element transition.
 *
 * Will automatically request new pages from `ViewModel` if needed.
 *
 * Will create [Snackbar] with error message if `ViewModel` specified one.
 * And show it when the last row of pictures is visible.
 *
 * Use the [ImageListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImageListFragment : Fragment(), ListItemClickListener, ListItemReadyListener {
    private val viewModel: ImageListViewModel by activityViewModels()

    private var shortAnimationDuration = 0L
    private var columnCount = 0

    private var errorTextContainer: LinearLayout? = null
    private var errorTextView: TextView? = null
    private var buttonRetry: Button? = null
    private var loadingSpinner: ProgressBar? = null

    private var recyclerView: RecyclerView? = null
    private var imageListAdapter: ImageListRecyclerViewAdapter? = null
    private var layoutManager: LinearLayoutManager? = null

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shortAnimationDuration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        prepTransition()
    }

    private fun prepTransition() {
        exitTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.shared_exit_image)

        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>?,
                sharedElements: MutableMap<String, View>?
            ) {
                recyclerView?.let {
                    val selHolder =
                        it.findViewHolderForAdapterPosition(viewModel.currentUserPosition)

                    if (names.isNullOrEmpty() || selHolder == null) {
                        return
                    }

                    sharedElements?.put(
                        names[0],
                        selHolder.itemView.findViewById(R.id.list_image_view)
                    )
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerView?.removeOnScrollListener(onScrollListener)
        imageListAdapter?.removeOnListItemClickListener()
        imageListAdapter?.removeOnListItemReadyListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image_list, container, false)
        errorTextContainer = view.findViewById(R.id.image_list_text_error_container)
        errorTextView = view.findViewById(R.id.image_list_text_error)
        buttonRetry = view.findViewById(R.id.image_list_button_retry)

        loadingSpinner = view.findViewById(R.id.image_list_loading_spinner)
        recyclerView = view.findViewById(R.id.image_list)

        buttonRetry?.setOnClickListener {
            loadingVisibilityState()
            requestNewPage()
        }

        if (viewModel.imagesData.value?.someObject.isNullOrEmpty()) {
            loadingVisibilityState()
        } else {
            showingListVisibilityState(false)
        }

        setupRecyclerView()
        setupViewModel()
        snackbar = ViewModelCompat.setupRetrySnackbar(requireContext(), view, viewModel)

        // if view model data is empty, then we just started fragment, without any data loaded
        // so we will never get to start postponed enter transition,
        // if view models live data is never going to be updated
        // (eg. query didn't return any results)
        if (!viewModel.imagesData.value?.someObject.isNullOrEmpty()) {
            postponeEnterTransition()
        }
        return view
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT, Configuration.ORIENTATION_LANDSCAPE -> {
                saveUserPos()
                updLayoutManager(
                    GridLayoutManager(
                        context,
                        resources.getInteger(R.integer.grid_span_count) // will be different for
                        // different configs
                    )
                )
            }
        }
    }

    private fun setupRecyclerView() {
        with(recyclerView) {
            updLayoutManager(
                GridLayoutManager(
                    context,
                    resources.getInteger(R.integer.grid_span_count)
                )
            )

            imageListAdapter = ImageListRecyclerViewAdapter()
            imageListAdapter?.setOnListItemClickListener(this@ImageListFragment)
            imageListAdapter?.setOnListItemReadyListener(this@ImageListFragment)
            this?.adapter = imageListAdapter

            this?.addOnScrollListener(onScrollListener)
        }
    }

    private fun updLayoutManager(manager: GridLayoutManager) {
        this.recyclerView?.layoutManager = manager
        this.layoutManager = manager
        this.columnCount = manager.spanCount
        scrollIfNeedTo()
    }

    private fun saveUserPos() {
        viewModel.currentUserPosition = layoutManager?.findLastVisibleItemPosition() ?: 0
    }

    private fun setupViewModel() {
        setImages(viewModel.imagesData.value?.someObject)

        viewModel.imagesData.observe(viewLifecycleOwner, { imgDescs ->
            if (imgDescs.someObject.isNullOrEmpty() && imgDescs.exception != null) {
                // spinner -> central error text (list is empty + error != null)
                errorTextView?.text =
                    getString(R.string.fetching_error, imgDescs.exception!!.message)
                errorVisibilityState()
                return@observe
            }

            // spinner -> recycler view (list is not empty)
            showingListVisibilityState(true)

            // recycler view -> recycler view + snackbar (list is not empty + error != null)
            invalidateSnackBar()
            if (imgDescs.exception != null) {
                return@observe
            }
            setImages(imgDescs.someObject)
        })

        // Request a new portion of images if needed
        if (viewModel.imagesData.value?.someObject.isNullOrEmpty()) {
            requestNewPage()
        }
    }

    private fun loadingVisibilityState() {
        if (loadingSpinner?.visibility != View.VISIBLE) {
            loadingSpinner?.visibility = View.VISIBLE
            errorTextContainer?.visibility = View.GONE
            recyclerView?.visibility = View.GONE
        }
    }

    private fun errorVisibilityState() {
        AnimationCompat.crossFade(
            errorTextContainer,
            loadingSpinner,
            shortAnimationDuration
        )
        recyclerView?.visibility = View.GONE
    }

    private fun showingListVisibilityState(animate: Boolean) {
        if (animate) {
            if (recyclerView?.visibility != View.VISIBLE) {
                errorTextContainer?.visibility = View.GONE
                AnimationCompat.crossFade(recyclerView, loadingSpinner, shortAnimationDuration)
            }
        } else {
            loadingSpinner?.visibility = View.GONE
            errorTextContainer?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        }
    }

    private fun invalidateSnackBar() {
        if (checkToRequestPage()) {
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

    private fun setImages(list: List<ImageDescription>?) {
        list?.let {
            imageListAdapter?.setImages(list)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scrollIfNeedTo()
    }

    private fun scrollIfNeedTo() {
        recyclerView?.post {
            layoutManager?.scrollToPosition(viewModel.currentUserPosition)
        }
    }

    private fun requestNewPage() {
        viewModel.loadPageBlocking()
    }

    override fun onItemClicked(view: View?, pos: Int) {
        viewModel.currentUserPosition = pos
        view?.let {
            (exitTransition as? TransitionSet)?.excludeTarget(it, true)
        }
        val fragment = ImagePagerFragment.newInstance(true)
        val imageView = view?.findViewById<ImageView>(R.id.list_image_view)
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            addSharedElement(imageView!!, imageView.transitionName)
            replace(
                getFragmentContainerId(),
                fragment,
                ImagePagerFragment::class.java.simpleName
            )
            addToBackStack(null)
        }
    }

    @IdRes
    private fun getFragmentContainerId(): Int {
        return (view?.parent as ViewGroup).id
    }

    override fun onItemReady(position: Int) {
        if (viewModel.currentUserPosition != position) {
            return
        }
        startPostponedEnterTransition()
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dy > 0) {
                if (checkToRequestPage()) {
                    requestNewPage()
                }
            }
            invalidateSnackBar()
        }
    }

    private fun checkToRequestPage(): Boolean {
        val totalItemCount = layoutManager?.itemCount ?: 0
        val lastVisibleItem = layoutManager?.findLastCompletelyVisibleItemPosition() ?: 0

        // -1 for index to size ratio
        return lastVisibleItem >= (totalItemCount - columnCount - 1)
    }

    companion object {
        private val TAG = ImageListFragment::class.java.simpleName

        /**
         * Use this factory method to create a new instance of
         * this fragment.
         * @return A new instance of fragment [ImageListFragment].
         */
        @JvmStatic
        fun newInstance() = ImageListFragment()
    }
}