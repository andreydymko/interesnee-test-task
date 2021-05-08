package com.andreydymko.testtask.serpapi.requester.jsonparser

import android.net.Uri
import com.andreydymko.testtask.serpapi.ImageDescription
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.lang.reflect.Type


object AnswerParser {
    /**
     * Parses resultant JSON of request to serpapi.com (Google image search API)
     * into [List] of Java-objects.
     *
     * @param responseBody String representation of resultant JSON.
     * @return Result of parsing valid JSON.
     * @throws IOException If JSON contains key "error" on its first level.
     */
    @Throws(IOException::class)
    fun getImgDescriptions(responseBody: String): List<ImageDescription> {
        val jsonObj = JsonParser.parseString(responseBody).asJsonObject

        val errorStr: String? = jsonObj.get("error")?.asString
        if (errorStr != null) {
            throw IOException(errorStr)
        }

        val imgDescriptions = jsonObj.get("images_results").asJsonArray

        val collectionType: Type = object : TypeToken<List<ImageDescription>>() {}.type

        return GsonBuilder()
            .registerTypeAdapter(Uri::class.java, SpecDeSerializers.UriDeserializer())
            .create()
            .fromJson(imgDescriptions, collectionType)
    }
}