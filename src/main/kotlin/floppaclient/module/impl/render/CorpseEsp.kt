package floppaclient.module.impl.render

import floppaclient.FloppaClient
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.ColorSetting
import floppaclient.module.settings.impl.NumberSetting
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object CorpseEsp : Module(
    "Corpse ESP",
    category = Category.RENDER,
    description = "Draws an esp around corpses."
){
    private val boxWidth =
        NumberSetting("Box Width", 0.9, 0.1, 2.0, 0.05, description = "Width of the esp box in units of blocks.")
    private val deaultLineWidth =
        NumberSetting("Default LW", 1.0, 0.1, 10.0, 0.1, description = "Default line width of the esp box.")

    private val colorLapis = ColorSetting("Bat Color", Color(0, 255, 0), false, description = "ESP color for bats.")
    private val colorSkeleton = ColorSetting("Bat Color", Color(0, 255, 0), false, description = "ESP color for bats.")
    private val colorUmber = ColorSetting("Bat Color", Color(0, 255, 0), false, description = "ESP color for bats.")
    private val colorTungsten = ColorSetting("Bat Color", Color(0, 255, 0), false, description = "ESP color for bats.")

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!this.enabled || !FloppaClient.inDungeons) return
        FloppaClient.mc.theWorld.loadedEntityList.stream()
            .forEach { entity ->
                if (entity is EntityArmorStand){

                }
            }
    }
}