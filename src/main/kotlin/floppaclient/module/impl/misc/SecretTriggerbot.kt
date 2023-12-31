package floppaclient.module.impl.misc

import floppaclient.FloppaClient.Companion.inDungeons
import floppaclient.FloppaClient.Companion.mc
import floppaclient.floppamap.dungeon.Dungeon
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.impl.dungeon.SecretChime
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.NumberSetting
import floppaclient.utils.fakeactions.FakeActionUtils
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

/**
 * This module is made to automatically click chests when looking at them in Dungeons (Pairs nicely with Stonk Delay),
 * Might be nice to make this a mod in secret aura
 * @author Stivais
 */
object SecretTriggerbot : Module(
    "Secret Triggerbot",
    category = Category.MISC,
    description = "Automatically clicks on a secret when looking at it"
) {
    private val cooldown =
        NumberSetting("Cooldown", 1000.0, 250.0, 2000.0, 10.0, description = "Cooldown for clicking secrets")
    private val keyHelper = BooleanSetting("Redstone Key", false, description = "Also picks up key")
    private val trappedChests = BooleanSetting("Trapped Chests", true, description = "Also clicks trapped chests.")

    init {
        this.addSettings(
            cooldown,
            trappedChests,
            keyHelper,
        )
    }

    private var nextAction: Long = System.currentTimeMillis()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!inDungeons || mc.thePlayer == null || mc.objectMouseOver?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return
        if (mc.currentScreen is GuiContainer) return

        val room = Dungeon.currentRoom?.data?.name
        if (room == "Water Board" || room == "Three Weirdos" || room == "Blaze" || room == "Blaze 2") return

        val objectPos = mc.objectMouseOver.blockPos
        val block = mc.theWorld.getBlockState(objectPos).block

        if (System.currentTimeMillis() < nextAction) return
        if (block == Blocks.chest || block == Blocks.lever || (block == Blocks.trapped_chest && trappedChests.enabled)) {
            interactBlock(objectPos)
        } else if (block == Blocks.skull) {

            val tileEntity: TileEntitySkull = mc.theWorld.getTileEntity(objectPos) as TileEntitySkull
            val id = tileEntity.playerProfile?.id?.toString()
            if (id == "26bb1a8d-7c66-31c6-82d5-a9c04c94fb02" || (id == "edb0155f-379c-395a-9c7d-1b6005987ac8" && keyHelper.enabled)) {
                interactBlock(objectPos)
            }
        }
    }

    private fun interactBlock(blockPos: BlockPos) {
        nextAction = System.currentTimeMillis() + cooldown.value.toLong()

        if (!mc.thePlayer.isSneaking) mc.thePlayer.swingItem()
        FakeActionUtils.clickBlock(blockPos)
        if (SecretChime.enabled) SecretChime.playSecretSound()
    }

    /*    @SubscribeEvent
        fun onRightClick(event: ClickEvent.RightClickEvent) {
            if (mc.thePlayer == null || !keyHelper.enabled || mc.objectMouseOver?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return

            val position = mc.objectMouseOver.blockPos
            if (mc.theWorld.getBlockState(position).block == Blocks.redstone_block && Utils.findItem("Redstone Key", inInv = true) != null) {
                event.isCanceled = true
                FakeActionUtils.clickBlockWithItem(position, mc.thePlayer.inventory.currentItem, "Redstone Key", 10.0, fromInv = true, abortIfNotFound = true)
            }
        }
    */
}