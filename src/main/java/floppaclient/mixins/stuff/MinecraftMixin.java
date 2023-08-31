package floppaclient.mixins.stuff;

import com.google.common.collect.Maps;
import floppaclient.util.SessionUtil;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Mixin(value = {Minecraft.class}, priority = 9999)
public abstract class MinecraftMixin {
    /**
     * @author forbai
     * @reason sbe thingy
     */
    @Overwrite
    public static Map<String, String> getSessionInfo() {
        HashMap<String, String> map = Maps.newHashMap();
        map.put("X-Minecraft-Username", Minecraft.getMinecraft().getSession().getUsername());
        if (!Objects.equals(SessionUtil.cachedUUID, Minecraft.getMinecraft().getSession().getUsername())) {
            map.put("X-Minecraft-UUID", SessionUtil.cachedUUID);
        } else {
            map.put("X-Minecraft-UUID", Minecraft.getMinecraft().getSession().getPlayerID());
        }
        map.put("X-Minecraft-Version", "1.8.9");
        return map;
    }
}
