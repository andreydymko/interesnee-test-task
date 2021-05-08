package com.andreydymko.testtask.ui.imagelist

/**
 * Simple interface that may be used by various list-displaying views to get callback when
 * list item on a certain position is ready for something.
 */
interface ListItemReadyListener {
    /**
     * Invoked when list item on [position] is ready for something.
     */
    fun onItemReady(position: Int)
}