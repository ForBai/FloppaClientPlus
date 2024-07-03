package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.events.ReceivePacketEvent
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.utils.Utils.equalsOneOf
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Module aimed to insta close secret chests in dungeons.
 *
 * @author Aton
 */
object CancelChestOpen : Module(
    "Cancel Chest Open",
    category = Category.DUNGEON,
    description = "Cancels secret chests from opening."
) {

    private val mode = BooleanSetting(
        "Auto close",
        false,
        description = "The mode to use, auto will automatically close the chest, any key will make any key input close the chest."
    )

    init {
        this.addSettings(mode)
    }

    @SubscribeEvent
    fun onOpenWindow(event: ReceivePacketEvent) {
        if (!inDungeons || event.packet !is S2DPacketOpenWindow || !event.packet.windowTitle.unformattedText.equalsOneOf(
                "Chest",
                "Large Chest"
            ) || !mode.enabled
        ) return
        if (event.packet.windowTitle.unformattedText.equalsOneOf("Chest", "Large Chest")) {
            event.isCanceled = true
            mc.netHandler.networkManager.sendPacket(C0DPacketCloseWindow(event.packet.windowId))
        }
    }

    @SubscribeEvent
    fun onInput(event: GuiScreenEvent.KeyboardInputEvent) {
        if (!inDungeons || mode.enabled || event.gui !is GuiChest) return
        if (((event.gui as? GuiChest)?.inventorySlots as? ContainerChest)?.lowerChestInventory?.name.equalsOneOf(
                "Chest",
                "Large Chest"
            )
        ) {
            mc.thePlayer.closeScreen()
        }
    }

    @SubscribeEvent
    fun onMouse(event: GuiScreenEvent.MouseInputEvent) {
        if (!inDungeons || mode.enabled || event.gui !is GuiChest) return
        if (((event.gui as? GuiChest)?.inventorySlots as? ContainerChest)?.lowerChestInventory?.name.equalsOneOf(
                "Chest",
                "Large Chest"
            )
        ) {
            mc.thePlayer.closeScreen()
        }
    }
}