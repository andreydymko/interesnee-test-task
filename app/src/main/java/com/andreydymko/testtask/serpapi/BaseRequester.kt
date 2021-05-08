package com.andreydymko.testtask.serpapi

/**
 * Base implementation of [Requester] interface.
 *
 * If a class don't need its own implementation of the observer pattern,
 * you may want to extend this class. As it's already contains [RequestResultListener] object
 * and corresponding methods to set and remove it.
 */
abstract class BaseRequester<T> : Requester<T> {
    protected var requestResultListener: RequestResultListener<T>? = null

    override fun setOnRequestResultListener(listener: RequestResultListener<T>) {
        requestResultListener = listener
    }

    override fun removeOnRequestResultListener() {
        requestResultListener = null
    }

    abstract override fun requestNextResults(toGet: Int, requestTag: Int)
}