package com.andreydymko.testtask

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.andreydymko.testtask.serpapi.ImageDescription
import com.andreydymko.testtask.serpapi.requester.jsonparser.AnswerParser
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.andreydymko.testtask", appContext.packageName)
    }


    @Test
    fun bodyToImgDescParserTest() {
        val thumbnail1 = "https://example.com/thumbnail1.png"
        val thumbnail2 = "https://example.com/thumbnail2.png"
        val thumbnail3 = "https://example.com/thumbnail3.png"

        val source1 = "example1.com"
        val source2 = "example2.com"
        val source3 = "example3.com"

        val title1 = "title 1"
        val title2 = "title 2"
        val title3 = "title 3"

        val link1 = "https://example.com/images1.html"
        val link2 = "https://example.com/images2.html"
        val link3 = "https://example.com/images3.html"

        val original1 = "https://example.com/original1.png"
        val original2 = "https://example.com/original2.png"
        val original3 = "https://example.com/original3.png"

        val jsonToParse = """{
  "search_metadata": {
    "id": "607adad00574f53415bc48a4",
    "status": "Success",
    "json_endpoint": "https://serpapi.com/searches/dab725cc386c4696/607adad00574f53415bc48a4.json",
    "created_at": "2021-04-17 12:55:44 UTC",
    "processed_at": "2021-04-17 12:55:44 UTC",
    "google_url": "https://www.google.com/search?q=apple&oq=apple&uule=w+CAIQICINVW5pdGVkIFN0YXRlcw&hl=en&gl=us&tbm=isch&filter=0&sourceid=chrome&ie=UTF-8",
    "raw_html_file": "https://serpapi.com/searches/dab725cc386c4696/607adad00574f53415bc48a4.html",
    "total_time_taken": 0.94
  },
  "search_parameters": {
    "engine": "google",
    "q": "apple",
    "location_requested": "United States",
    "location_used": "United States",
    "google_domain": "google.com",
    "hl": "en",
    "gl": "us",
    "ijn": "0",
    "device": "desktop",
    "tbm": "isch",
    "filter": "0"
  },
  "search_information": {
    "image_results_state": "Results for exact spelling",
    "query_displayed": "apple"
  },
  "suggested_searches": [
    {
      "name": "red",
      "link": "https://www.google.com/search?q=apple&tbm=isch&chips=q:apple,g_1:red:KScHOMyohEg%3D&hl=en-US&sa=X&ved=2ahUKEwiwqNXqqYXwAhUSJKwKHbwXAKcQ4lYoAHoECAEQGA",
      "chips": "q:apple,g_1:red:KScHOMyohEg%3D",
      "serpapi_link": "https://serpapi.com/search.json?chips=q%3Aapple%2Cg_1%3Ared%3AKScHOMyohEg%253D&device=desktop&engine=google&filter=0&gl=us&google_domain=google.com&hl=en&ijn=0&location=United+States&q=apple&tbm=isch",
      "thumbnail": "https://serpapi.com/searches/607adad00574f53415bc48a4/images/26feecc1aadb63f874d39eb31c6e0ed3384abd14cd6a440124793ac47d65fb69.jpeg"
    },
    {
      "name": "logo",
      "link": "https://www.google.com/search?q=apple&tbm=isch&chips=q:apple,g_1:logo:iWbYUrLD8zo%3D&hl=en-US&sa=X&ved=2ahUKEwiwqNXqqYXwAhUSJKwKHbwXAKcQ4lYoAXoECAEQGg",
      "chips": "q:apple,g_1:logo:iWbYUrLD8zo%3D",
      "serpapi_link": "https://serpapi.com/search.json?chips=q%3Aapple%2Cg_1%3Alogo%3AiWbYUrLD8zo%253D&device=desktop&engine=google&filter=0&gl=us&google_domain=google.com&hl=en&ijn=0&location=United+States&q=apple&tbm=isch",
      "thumbnail": "https://serpapi.com/searches/607adad00574f53415bc48a4/images/26feecc1aadb63f874d39eb31c6e0ed345c1b6f8b7674bb7786c80832701cae0.png"
    }
  ],
  "images_results": [
    {
      "position": 1,
      "thumbnail": "$thumbnail1",
      "source": "$source1",
      "title": "$title1",
      "link": "$link1",
      "original": "$original1"
    },
    {
      "position": 2,
      "thumbnail": "$thumbnail2",
      "source": "$source2",
      "title": "$title2",
      "link": "$link2",
      "original": "$original2"
    },
    {
      "position": 3,
      "thumbnail": "$thumbnail3",
      "source": "$source3",
      "title": "$title3",
      "link": "$link3",
      "original": "$original3"
    }
  ]
}
"""

        val parsingResult = AnswerParser.getImgDescriptions(jsonToParse)

        val manualResult = listOf(
            ImageDescription(
                Uri.parse(thumbnail1),
                Uri.parse(source1),
                title1,
                Uri.parse(link1),
                Uri.parse(original1)
            ),
            ImageDescription(
                Uri.parse(thumbnail2),
                Uri.parse(source2),
                title2,
                Uri.parse(link2),
                Uri.parse(original2)
            ),
            ImageDescription(
                Uri.parse(thumbnail3),
                Uri.parse(source3),
                title3,
                Uri.parse(link3),
                Uri.parse(original3)
            )
        )

        assertEquals(parsingResult, manualResult)
    }
}