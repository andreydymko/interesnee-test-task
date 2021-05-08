package com.andreydymko.testtask.serpapi

/**
 * Simple interface that can be used by [Requester] results observer to get callback when
 * [Requester] completed or failed request.
 */
interface RequestResultListener<T> {
    /**
     * Invoked when [Requester] successfully completed request.
     */
    fun onRequestSuccess(result: T, requestTag: Int)

    /**
     * Invoked when [Requester] failed to complete request.
     */
    fun onRequestFail(exception: Exception, requestTag: Int)
}