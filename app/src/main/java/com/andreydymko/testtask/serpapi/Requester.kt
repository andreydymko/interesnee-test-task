package com.andreydymko.testtask.serpapi

import androidx.annotation.IntRange

const val REQUEST_PAGE_SIZE = 100L

/**
 * Data provider that supports pagination and utilizes observer pattern.
 * Page size is limited to value of [REQUEST_PAGE_SIZE].
 * @param T The type of result data.
 */
interface Requester<T> {
    /**
     * Should be called to request new page from data provider.
     * Page size is limited by value of [REQUEST_PAGE_SIZE].
     * @param toGet Number of items to request.
     * @param requestTag Any integer that might be used to distinguish requests from each other.
     */
    fun requestNextResults(
        @IntRange(from = 0, to = REQUEST_PAGE_SIZE) toGet: Int,
        requestTag: Int = 0
    )

    /**
     * Set a [RequestResultListener] that will be notified of any results that this
     * [Requester] has made. This will overwrite any listeners that has been set.
     */
    fun setOnRequestResultListener(listener: RequestResultListener<T>)

    /**
     * Remove a [RequestResultListener] that was notified of any results that this
     * [Requester] has made.
     */
    fun removeOnRequestResultListener()
}