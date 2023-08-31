package floppaclient.mixins.stuff;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import floppaclient.util.SessionUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Session;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;

@Mixin(value = {Session.class}, priority = 9999)
public abstract class MixinSession {

    @Shadow
    @Final
    private String token;

    @Shadow
    private PropertyMap properties;

    @Shadow
    public abstract String getUsername();

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private static String editPlayerID(String playerIDIn) {
        SessionUtil.cachedUUID = playerIDIn;
        return "5e58a7fa8d1f4d54a0e519ae06dce0c7";
    }

    /**
     * @author forbai
     * @reason sbe thingy
     */
    @Overwrite
    public String getSessionID() {
        return "token:" + this.token + ":" + SessionUtil.cachedUUID;
    }

    /**
     * @author forbai
     * @reason sbe thingy
     */
    @Overwrite
    public GameProfile getProfile() {
        try {
            UUID uuid = UUIDTypeAdapter.fromString(SessionUtil.cachedUUID);
            GameProfile ret = new GameProfile(uuid, this.getUsername());
            if (this.properties != null) {
                ret.getProperties().putAll(this.properties);
            }
            return ret;
        } catch (IllegalArgumentException var2) {
            return new GameProfile(EntityPlayer.getUUID(new GameProfile(null, this.getUsername())), this.getUsername());
        }
    }
}
