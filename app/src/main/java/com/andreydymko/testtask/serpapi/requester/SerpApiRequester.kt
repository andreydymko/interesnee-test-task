package com.andreydymko.testtask.serpapi.requester

import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import com.andreydymko.testtask.serpapi.BaseRequester
import com.andreydymko.testtask.serpapi.ImageDescription
import com.andreydymko.testtask.serpapi.requester.jsonparser.AnswerParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.text.MessageFormat
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap

/**
 * Implementation of abstract class [BaseRequester] that will communicate with [http://serpapi.com]
 * Google image search API to fetch [List]s of [ImageDescription]s.
 *
 * The resulting pages will be sent in requested order.
 *
 * This implementation is thread-safe, but not efficient, as it will not execute
 * next request before the last one has finished (either by succeeding or failing).
 *
 * @param reqData object that contains data about desired request.
 */
class SerpApiRequester(reqData: RequestData) : BaseRequester<List<ImageDescription>>(), Callback {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val httpClient: OkHttpClient = OkHttpClient()
    private val uriTemplate: String

    private val requestQueue = LinkedBlockingQueue<QueuedRequest>()
    private var internalBuffer: List<ImageDescription>? = null
    private var currBufferPos = 0

    private var lastReqPage = 0
    private var isCurrentlyFetching = AtomicBoolean(false)

    init {
        val strBuilder = StringBuilder("https://serpapi.com/")
            .append("search.json?")
            .append("engine=google")
            .append("&google_domain=google.com")
            .append("&tbm=isch")
            .append("&q=").append(reqData.query)

        reqData.apiKey?.let {
            strBuilder.append("&api_key=").append(it)
        }

        reqData.country?.let {
            strBuilder.append("&gl=").append(it)
        }
        reqData.language?.let {
            strBuilder.append("&hl=").append(it)
        }
        if (reqData.adultFiltering) {
            strBuilder.append("&safe=active")
        }
        if (reqData.excludeAutoCorrect) {
            strBuilder.append("&nfpr=1")
        }

        uriTemplate = strBuilder.append("&ijn={0}").toString()
    }

    override fun requestNextResults(toGet: Int, requestTag: Int) {
        requestNextResult(toGet, requestTag)
    }

    /**
     * Execute next page fetching or get it from buffer. Will add request to queue
     * if previous request is still in progress.
     *
     * @param toGet number of items to get.
     * @param requestTag integer to distinguish requests from each other.
     * @param isFromQueue whether this request is coming from queue
     */
    private fun requestNextResult(
        toGet: Int,
        requestTag: Int,
        isFromQueue: Boolean = false
    ): Boolean {
        if (isCurrentlyFetching.compareAndSet(false, true)) {
            if (internalBuffer != null && (currBufferPos + toGet) <= internalBuffer!!.size) {
                // we have result in cache
                mainHandler.post {
                    // make it look "async"
                    sendBufferedPart(toGet, requestTag)
                }
            } else {
                // we need to make request
                var toSend = toGet
                internalBuffer?.apply {
                    if (currBufferPos < this.size) {
                        // but first, drain the buffer
                        val remainedInBuff = this.size - currBufferPos
                        mainHandler.post {
                            // make it look "async"
                            sendBufferedPart(remainedInBuff, requestTag)
                        }
                        toSend -= remainedInBuff
                    }
                }

                val tags = HashMap<String, Int>().apply {
                    this[TAGS_NUM_TO_SEND] = toSend
                    this[TAGS_REQUEST_TAG] = requestTag
                }

                val req = Request.Builder()
                    .url(MessageFormat.format(uriTemplate, lastReqPage))
                    .tag(tags)
                    .build()

                httpClient.newCall(req).enqueue(this)
            }
            return true
        } else {
            if (!isFromQueue) {
                requestQueue.offer(
                    QueuedRequest(toGet, requestTag),
                    QUEUE_TO_WAIT,
                    TimeUnit.MILLISECONDS
                )
            }
            return false
        }
    }

    override fun onResponse(call: Call, response: Response) {
        if (!response.isSuccessful) {
            onFailure(
                call, IOException(
                    MessageFormat.format(
                        EXC_MSG_REQUEST_CODE_NOT_SUCCESS,
                        response.code
                    )
                )
            )
            return
        }
        // body is accessed in "onResponse" callback, thus it will never be null
        val responseString: String
        try {
            responseString = response.body!!.string()
            response.body!!.close()
        } catch (e: IOException) {
            onFailure(call, e)
            return
        }

        mainHandler.post {
            val tags = call.request().tag() as HashMap<*, *>

            try {
                internalBuffer = AnswerParser.getImgDescriptions(responseString)
            } catch (e: IOException) {
                onFailure(call, e)
                return@post
            }
            // change last requested page only on request success
            lastReqPage++
            currBufferPos = 0

            sendBufferedPart(tags[TAGS_NUM_TO_SEND] as Int, tags[TAGS_REQUEST_TAG] as Int)

            isCurrentlyFetching.set(false)
            getNextFromQueue()
        }
    }

    override fun onFailure(call: Call, e: IOException) {
        mainHandler.post {
            val tags = call.request().tag() as HashMap<*, *>
            requestResultListener?.onRequestFail(e, tags[TAGS_REQUEST_TAG] as Int)

            isCurrentlyFetching.set(false)
            getNextFromQueue()
        }
    }

    private fun sendBufferedPart(toSend: Int, tag: Int) {
        val buffBound = if ((currBufferPos + toSend) < internalBuffer?.size ?: 0) {
            currBufferPos + toSend
        } else {
            internalBuffer?.size ?: 0
        }

        requestResultListener?.onRequestSuccess(
            internalBuffer!!.subList(
                currBufferPos,
                buffBound
            ),
            tag
        )

        currBufferPos = buffBound
        isCurrentlyFetching.set(false)
    }

    /**
     * Start next queued up request and remove it from queue if it has successfully started.
     */
    private fun getNextFromQueue() {
        if (!requestQueue.isEmpty()) {
            if (requestNextResult(requestQueue.peek())) {
                requestQueue.poll(QUEUE_TO_WAIT, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun requestNextResult(queuedRequest: QueuedRequest?): Boolean {
        queuedRequest?.let {
            return requestNextResult(queuedRequest.toGet, queuedRequest.tag, true)
        }
        return false
    }

    private data class QueuedRequest(val toGet: Int, val tag: Int)


    /**
     * Data class that has all the needed data for [SerpApiRequester] to make requests.
     *
     * @param query any text that should be sent as query string.
     * @param apiKey API-key that you should get on [serpapi.com](http://serpapi.com).
     * @param country short name of desired search country (eg. "us" for the "United States",
     * "uk" for "United Kingdom"). Head to the [Google countries](https://serpapi.com/google-countries)
     * for a full list of supported Google countries.
     * @param language short name of desired search language (eg. "en" for English).
     * @param adultFiltering level of filtering for adult content - active or off.
     * @param excludeAutoCorrect exclusion of results from an auto-corrected query
     * that is spelled wrong.
     */
    // a good place for the builder pattern probably. But as many people said,
    // if a language support named arguments and default values for them (e.g. python)
    // then builder pattern loses it's purpose
    data class RequestData(
        val query: String,
        val apiKey: String? = null,
        var country: String? = null,
        var language: String? = null,
        var adultFiltering: Boolean = false,
        var excludeAutoCorrect: Boolean = false
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()
        ) {
            country = parcel.readString()
            language = parcel.readString()
            adultFiltering = parcel.readInt() != 0
            excludeAutoCorrect = parcel.readInt() != 0
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel?, flags: Int) {
            dest?.apply {
                writeString(query)
                writeString(apiKey)
                writeString(country)
                writeString(language)
                writeInt(if (adultFiltering) 1 else 0)
                writeInt(if (excludeAutoCorrect) 1 else 0)
            }
        }

        companion object CREATOR : Parcelable.Creator<RequestData> {
            override fun createFromParcel(parcel: Parcel): RequestData {
                return RequestData(parcel)
            }

            override fun newArray(size: Int): Array<RequestData?> {
                return arrayOfNulls(size)
            }
        }
    }

    private companion object {
        private const val EXC_MSG_REQUEST_CODE_NOT_SUCCESS = "Request failed. Status code: {0}"

        private const val TAGS_NUM_TO_SEND = "TAGS_NUM_TO_SEND"
        private const val TAGS_REQUEST_TAG = "TAGS_REQUEST_TAG"
        private const val QUEUE_TO_WAIT = 50L
    }
}