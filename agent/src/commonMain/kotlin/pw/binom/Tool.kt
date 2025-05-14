package pw.binom

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object Tool {
    fun toolFunction(e: KSerializer<out Any>): JsonObject = toolFunction(e.descriptor)
    fun toolFunction(e: SerialDescriptor): JsonObject {
        require(e.kind === StructureKind.CLASS || e.kind === StructureKind.OBJECT) { "Invalid function kind: ${e.kind}" }

        val params = HashMap(obj(e))
        params["name"] = JsonPrimitive(e.serialName)
        val result = HashMap<String, JsonElement>()
        result["type"] = JsonPrimitive("function")
        result["function"] = JsonObject(params)
        result["strict"] = JsonPrimitive(true)
        return JsonObject(result)
    }

    fun common(e: SerialDescriptor): JsonObject {
        return when (e.kind) {
            StructureKind.OBJECT,
            StructureKind.CLASS,
                -> obj(e)

            PolymorphicKind.OPEN -> TODO()
            PolymorphicKind.SEALED -> TODO()
            PrimitiveKind.BOOLEAN -> boolean(e)
            PrimitiveKind.SHORT,
            PrimitiveKind.LONG,
            PrimitiveKind.INT,
            PrimitiveKind.FLOAT,
            PrimitiveKind.DOUBLE,
            PrimitiveKind.BYTE,
                -> number(e)

            PrimitiveKind.CHAR -> TODO()
            PrimitiveKind.STRING -> str(e)
            SerialKind.CONTEXTUAL -> TODO()
            SerialKind.ENUM -> enum(e)
            StructureKind.LIST -> list(e)
            StructureKind.MAP -> TODO()
        }
    }

    private fun list(e: SerialDescriptor): JsonObject {
        val result = HashMap<String, JsonElement>()
        result["type"] = if (e.isNullable) {
            JsonArray(listOf(JsonPrimitive("array"), JsonPrimitive("null")))
        } else {
            JsonPrimitive("array")
        }
        result["items"] = common(e.getElementDescriptor(0))
        return JsonObject(result)
    }

    private fun boolean(e: SerialDescriptor): JsonObject {
        val result = HashMap<String, JsonElement>()
        result["type"] = if (e.isNullable) {
            JsonArray(listOf(JsonPrimitive("boolean"), JsonPrimitive("null")))
        } else {
            JsonPrimitive("boolean")
        }
        return JsonObject(result)
    }

    private fun number(e: SerialDescriptor): JsonObject {
        val result = HashMap<String, JsonElement>()
        result["type"] = if (e.isNullable) {
            JsonArray(listOf(JsonPrimitive("number"), JsonPrimitive("null")))
        } else {
            JsonPrimitive("number")
        }
        return JsonObject(result)
    }

    private fun enum(e: SerialDescriptor): JsonObject {
        val result = HashMap<String, JsonElement>()
        result["type"] = if (e.isNullable) {
            JsonArray(listOf(JsonPrimitive("string"), JsonPrimitive("null")))
        } else {
            JsonPrimitive("string")
        }
        result["enum"] = JsonArray(e.elementNames.map { JsonPrimitive(it) })
        return JsonObject(result)
    }

    private fun str(e: SerialDescriptor): JsonObject {
        val result = HashMap<String, JsonElement>()
        result["type"] = if (e.isNullable) {
            JsonArray(listOf(JsonPrimitive("string"), JsonPrimitive("null")))
        } else {
            JsonPrimitive("string")
        }
        return JsonObject(result)
    }

    private fun obj(e: SerialDescriptor): JsonObject {
        val description = e.getAnnotation<Description>()?.description

        val result = HashMap<String, JsonElement>()
        result["type"] = JsonPrimitive("object")
        if (description != null) {
            result["description"] = JsonPrimitive(description)
        }
        val properties = HashMap<String, JsonElement>()
        repeat(e.elementsCount) { index ->
            val desc = e.getElementAnnotation<Description>(index)?.description
            var obj = common(e.getElementDescriptor(index))
            if (desc != null) {
                val ee = HashMap(obj)
                ee["description"] = JsonPrimitive(desc)
                obj = JsonObject(ee)
            }
            properties[e.getElementName(index)] = obj
        }
        result["properties"] = JsonObject(properties)
        val required = ArrayList<JsonPrimitive>()
        repeat(e.elementsCount) { index ->
            if (!e.isElementOptional(index)) {
                required += JsonPrimitive(e.getElementName(index))
            }
        }
        result["required"] = JsonArray(required)
        result["additionalProperties"] = JsonPrimitive(false)
        return JsonObject(result)
    }
}
