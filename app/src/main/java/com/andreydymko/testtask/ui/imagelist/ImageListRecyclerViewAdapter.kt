package com.andreydymko.testtask.ui.imagelist

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.andreydymko.testtask.R
import com.andreydymko.testtask.serpapi.ImageDescription
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


/**
 * [RecyclerView.Adapter] that can display the list of [ImageDescription].
 */
class ImageListRecyclerViewAdapter
    : RecyclerView.Adapter<ImageListRecyclerViewAdapter.ImageListViewHolder>() {

    private var imageDescs: ArrayList<ImageDescription> = ArrayList()
    private var listItemClickListener: ListItemClickListener? = null
    private var listItemReadyListener: ListItemReadyListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_list_view_item, parent, false)

        return ImageListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageListViewHolder, position: Int) {
        imageDescs[position].let { imgDesc ->
            ViewCompat.setTransitionName(holder.imageView, imgDesc.toString())

            holder.imageView.contentDescription = imgDesc.title
            Glide.with(holder.itemView)
                .load(imgDesc.thumbnailImg)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        listItemReadyListener?.onItemReady(position)
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        listItemReadyListener?.onItemReady(position)
                        return false
                    }
                })
                .fallback(R.drawable.ic_baseline_no_photography_24)
                .error(R.drawable.ic_baseline_broken_image_24)
                .into(holder.imageView)
        }
    }

    override fun getItemCount(): Int = imageDescs.size

    /**
     * Will set images to the [RecyclerView]s inner buffer, while effectively notifying its `View`
     * about changes using [DiffUtil].
     *
     * **All previously added items will be deleted and new items set**.
     * But items that has already been on their places will not be re-drawn.
     *
     * @param imageDescriptions items to be set in this `RecyclerView`
     */
    fun setImages(imageDescriptions: List<ImageDescription>) {
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return imageDescs.size
            }

            override fun getNewListSize(): Int {
                return imageDescriptions.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return imageDescs[oldItemPosition] === imageDescriptions[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return imageDescs[oldItemPosition] == imageDescriptions[newItemPosition]
            }
        }, false)
        imageDescs.clear()
        imageDescs.addAll(imageDescriptions)
        result.dispatchUpdatesTo(this)
    }

    /**
     * Set a [ListItemClickListener] that will be notified of any click events on items of this
     * [RecyclerView]. This will overwrite any listeners that has been set.
     *
     * You may call [removeOnListItemClickListener] to remove previously attached listener.
     *
     * @param itemClickListener listener to set
     */
    fun setOnListItemClickListener(itemClickListener: ListItemClickListener) {
        this.listItemClickListener = itemClickListener
    }

    /**
     * Remove a [ListItemClickListener] that was notified of any click events on items of this
     * [RecyclerView].
     */
    fun removeOnListItemClickListener() {
        this.listItemClickListener = null
    }

    /**
     * Set a [ListItemReadyListener] that will be notified when image
     * in this [RecyclerView] is ready to be shown -
     * image thumbnail is either downloaded and set to the corresponding [View]
     * or error happened and thumbnail will never be shown until re-creation of this `View`.
     * This will overwrite any listeners that has been set.
     *
     * You may call [removeOnListItemReadyListener] to remove previously attached listener.
     *
     * @param itemReadyListener listener to set
     */
    fun setOnListItemReadyListener(itemReadyListener: ListItemReadyListener) {
        this.listItemReadyListener = itemReadyListener
    }

    /**
     * Remove a [ListItemReadyListener] that was notified of any item ready events on items of this
     * [RecyclerView].
     *
     * @see [setOnListItemReadyListener]
     */
    fun removeOnListItemReadyListener() {
        this.listItemReadyListener = null
    }

    inner class ImageListViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        val imageView: ImageView = view.findViewById(R.id.list_image_view)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            listItemClickListener?.onItemClicked(v, bindingAdapterPosition)
        }
    }
}