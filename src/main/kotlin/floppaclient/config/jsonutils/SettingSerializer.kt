package floppaclient.config.jsonutils

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import floppaclient.module.settings.Setting
import floppaclient.module.settings.impl.*
import java.lang.reflect.Type

class SettingSerializer : JsonSerializer<Setting<*>> {
    override fun serialize(src: Setting<*>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonObject().apply {
            when (src) {
                is BooleanSetting -> this.addProperty(src.name, src.enabled)
                is NumberSetting -> this.addProperty(src.name, src.value)
                is StringSelectorSetting -> this.addProperty(src.name, src.selected)
                is SelectorSetting -> this.addProperty(src.name, src.selected)
                is StringSetting -> this.addProperty(src.name, src.text)
                is ColorSetting -> this.addProperty(src.name, src.value.rgb)
                is ActionSetting -> this.addProperty(src.name, "Action Setting")
                is KeybindSetting -> this.addProperty(src.name, src.value.key)
            }
        }
    }

}