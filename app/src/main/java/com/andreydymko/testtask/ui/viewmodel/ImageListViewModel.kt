package com.andreydymko.testtask.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.andreydymko.testtask.serpapi.ImageDescription
import com.andreydymko.testtask.serpapi.Requester
import com.andreydymko.testtask.serpapi.RequestResultListener
import java.util.concurrent.atomic.AtomicBoolean

class ImageListViewModel : ViewModel(), RequestResultListener<List<ImageDescription>> {
    /**
     * Designates if page requesting is still in progress.
     */
    val isLoading = MutableLiveData(AtomicBoolean(false))

    /**
     * Contains all gained data through all lifetime of
     * [requester].
     *
     * And also exception that could have taken place during page requesting.
     */
    val imagesData = MutableLiveData<ErroneousObjectWrapper<ArrayList<ImageDescription>>>()

    /**
     * Designates last item in [imagesData] that user has seen while interacting with UI.
     */
    var currentUserPosition = 0

    /**
     * Object that supplies [ImageListViewModel] with data. Responsible for server interaction
     * and providing paginated data.
     */
    var requester: Requester<List<ImageDescription>>? = null
        /**
         * Will set [requester] to specified [value]
         * and also reset [currentUserPosition], [imagesData] and [isLoading] variables
         * without notifying observers.
         */
        set(value) {
            requester?.removeOnRequestResultListener()
            currentUserPosition = 0
            imagesData.value?.someObject?.clear()
            imagesData.value?.exception = null
            isLoading.value?.set(false)
            field = value
            requester?.setOnRequestResultListener(this)
        }

    /**
     * Will send request to load a new page, while setting [isLoading] to `true`
     * and blocking itself till request either succeed or failed
     * (calling this function will have no effect during this time).
     *
     * This function is thread-safe.
     */
    fun loadPageBlocking() {
        if (isLoading.value?.compareAndSet(false, true) == true) {
            isLoading.notifyObserver()
            loadPage()
        }
    }

    private fun loadPage() {
        requester?.requestNextResults(PAGE_SIZE)
    }

    override fun onRequestSuccess(result: List<ImageDescription>, requestTag: Int) {
        if (imagesData.value == null) {
            imagesData.value = ErroneousObjectWrapper(null, ArrayList(result))
            loadingFinish()
            return
        }
        imagesData.value?.someObject?.addAll(result)
        imagesData.value?.exception = null
        imagesData.notifyObserver()

        loadingFinish()
    }

    override fun onRequestFail(exception: Exception, requestTag: Int) {
        if (imagesData.value == null) {
            imagesData.value = ErroneousObjectWrapper(exception, ArrayList())
            loadingFinish()
            return
        } else {
            imagesData.value?.exception = exception
            imagesData.notifyObserver()
        }

        loadingFinish()
    }

    private fun loadingFinish() {
        isLoading.value?.set(false)
        isLoading.notifyObserver()
    }

    private fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }

    override fun onCleared() {
        requester?.removeOnRequestResultListener()
    }

    companion object {
        private const val PAGE_SIZE = 20
        private val TAG = ImageListViewModel::class.java.simpleName
    }
}