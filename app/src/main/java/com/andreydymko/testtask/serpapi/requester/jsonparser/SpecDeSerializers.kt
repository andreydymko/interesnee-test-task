package com.andreydymko.testtask.serpapi.requester.jsonparser

import android.net.Uri
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.JsonDeserializer
import com.google.gson.JsonParseException
import com.google.gson.JsonDeserializationContext
import java.lang.reflect.Type

/**
 * Contains custom class that extend either [JsonSerializer] or [JsonDeserializer] for [Gson].
 */
object SpecDeSerializers {
    /**
     * Serializes [Uri] to [String].
     */
    class UriSerializer : JsonSerializer<Uri> {
        override fun serialize(
            src: Uri,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            return JsonPrimitive(src.toString())
        }
    }

    /**
     * Deserializes [String] to [Uri].
     */
    class UriDeserializer : JsonDeserializer<Uri> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            src: JsonElement, srcType: Type?,
            context: JsonDeserializationContext?
        ): Uri {
            return Uri.parse(src.asString)
        }
    }
}