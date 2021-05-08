package com.andreydymko.testtask.ui.imagelist

import android.view.View

/**
 * Simple interface that may be used by various list-displaying views to get callback when
 * list item on a certain position has been clicked.
 */
interface ListItemClickListener {
    /**
     * Invoked when list item on [pos] has been clicked. [view] is representing [View] that either
     * has been clicked or is a parent view of [View] that has been clicked.
     */
    fun onItemClicked(view: View?, pos: Int)
}