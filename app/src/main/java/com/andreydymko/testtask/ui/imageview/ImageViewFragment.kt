package com.andreydymko.testtask.ui.imageview

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.andreydymko.testtask.R
import com.andreydymko.testtask.serpapi.ImageDescription
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


/**
 * A simple [Fragment] subclass.
 *
 * Will load image by [android.net.Uri] specified in [ImageDescription.originalImg],
 * showing thumbnail specified in [ImageDescription.thumbnailImg] while it is loading.
 * When loading is finished image will be shown to the user.
 *
 * Supports fragments shared element transition
 * ([link](https://developer.android.com/guide/fragments/animate#shared))
 * by invoking [Fragment.startPostponedEnterTransition] on [mParentFragment]
 * when either image or thumbnail has been downloaded and ready to be shown.
 *
 * Use the [ImageViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImageViewFragment : Fragment() {
    private lateinit var imageDesc: ImageDescription
    private var imageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            imageDesc = it.getParcelable(ARG_IMAGE_DESC)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_image_pager_item, container, false)

        imageView = view.findViewById(R.id.pager_image_view)

        ViewCompat.setTransitionName(imageView!!, imageDesc.toString())
        setImageDesc(imageDesc)

        return view
    }

    private fun setImageDesc(imageDesc: ImageDescription) {
        imageView?.contentDescription = imageDesc.title

        val thumbnailReq = Glide.with(this)
            .load(imageDesc.thumbnailImg)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .listener(glideCallback)
            .error(R.drawable.ic_baseline_broken_image_24)

        Glide.with(this)
            .load(imageDesc.originalImg)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .thumbnail(thumbnailReq)
            .fallback(R.drawable.ic_baseline_no_photography_24)
            .listener(glideCallback) // thumbnail may not even load, so we need to listen here too
            .into(imageView!!)
            .clearOnDetach() // if user scrolls pager fast enough, then we might end up with
        // a lot of queued up download tasks, that will slow down downloading of
        // currently visible item
        // do we want to avoid it? Idk, I think we do (c) Andrey Dymko, 2021
        // On the other hand, it would be nice to not to cancel downloading image if user is waiting
        // for it to complete, while viewing other images, to "brighten up the waiting"
    }

    private val glideCallback = object : RequestListener<Drawable> {
        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            parentFragment?.startPostponedEnterTransition()
            return false
        }

        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            parentFragment?.startPostponedEnterTransition()
            return false
        }
    }

    companion object {
        private val TAG = ImageViewFragment::class.java.simpleName
        private const val ARG_IMAGE_DESC = "ARG_IMAGE_DESC"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param imageDesc Object that is describing image to be shown.
         * @return A new instance of fragment ImageViewFragment.
         */
        @JvmStatic
        fun newInstance(imageDesc: ImageDescription) =
            ImageViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_IMAGE_DESC, imageDesc)
                }
            }
    }
}