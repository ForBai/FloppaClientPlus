package floppaclient.config.jsonutils

import com.google.gson.*
import net.minecraft.util.BlockPos
import java.lang.reflect.Type

class SetBlockPosSerializer : JsonSerializer<MutableSet<BlockPos>> {
    override fun serialize(
        src: MutableSet<BlockPos>?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val jsonArray = JsonArray()

        src?.forEach {
            jsonArray.add(JsonPrimitive("${it.x}, ${it.y}, ${it.z}"))
        }

        return jsonArray
    }
}
