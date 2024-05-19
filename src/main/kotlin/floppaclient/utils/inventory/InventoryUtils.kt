package floppaclient.utils.inventory

import floppaclient.FloppaClient.Companion.mc
import floppaclient.utils.inventory.ItemUtils.itemID
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack

/**
 * # A collection of methods for finding items in inventories.
 *
 * ## About slot numbering
 * Minecraft uses different numbering systems for slots in inventories.
 * Depending on which methods are used each slot can have different indices.
 * There are two main systems for numbering the slots in an inventory.
 * One is used by the [Container][net.minecraft.inventory.Container] in [GuiContainer] classes and the other system is
 * used when the inventory is accessed directly such as in [InventoryPlayer].
 *
 * The indices for the inventory slots of the [GuiInventory] do not match the indices of [InventoryPlayer]!
 *
 * The difference mostly affects the numbering of the players hotbar.
 *
 * ### Container style numbering
 * When the players inventory is accessed through the corresponding Container like when [GuiInventory.inventorySlots]
 * is used, the crafting and armor slots are included. Numbering starts at the inventory crafting slot with numbers 0..4.
 * Next come the armor slots taking numbers 5..8 starting with 5 at the helmet and then going down.
 * And finally the main inventory uses the indices 9..44 starting top left, where the hot-bar comes last.
 *
 * ### InventoryPlayer style numbering.
 * [InventoryPlayer] treats the armor slots separately from the main inventory and does not support the crafting slots
 * (since no items can be stored there).
 * Methods such as [InventoryPlayer.getStackInSlot] use the field [InventoryPlayer.mainInventory] which only contains
 * the 36 slots of the players main inventory.
 * Here the numbering starts at the hot-bar, which uses indices 0..8 left to right.
 * After that comes the rest of the inventory again starting top left.
 *
 * The indices for the non-visible inventory slots are the same in both numbering systems: 9..35.
 *
 * ### Numbering in the Chest gui.
 *
 * Chests use the Container style numbering system. Numbering starts at 0 at the top left slot of the chest inventory.
 *
 *
 *
 * @author Aton
 * @see SkyblockItem
 */
object InventoryUtils {
    //<editor-fold desc="findItem">
    /**
     * Returns the first slot where an item with one of the specified [attributes] is found.
     * Returns null if no matches were found.
     *
     * [SkyblockItem] is used to determine whether an item meets an attribute.
     *
     * @param inInv Will also search in the inventory and not only in the hotbar
     */
    fun findItem(vararg attributes: SkyblockItem.Attribute, inInv: Boolean = false): Int? {
        val items = SkyblockItem.values().filter { attributes.any { attribute -> it.hasAttribute(attribute) } }
        val regex = Regex(items.joinToString("|") { it.itemID })
        return findItem(regex, inInv, 2)
    }

    /**
     * Returns the first slot where the specified [item] is found.
     * Returns null if no matches were found.
     *
     * @param inInv Will also search in the inventory and not only in the hotbar
     */
    fun findItem(item: SkyblockItem, inInv: Boolean = false): Int? {
        val regex = Regex(item.itemID)
        return findItem(regex, inInv, 2)
    }

    /**
     * Returns the first slot where the item name or id passes a check for [name].
     * The item name is checked to contain [name].
     * The item id is checked for a full match with [name].
     * Returns null if no matches were found.
     * @param name The name or item ID to find.
     * @param ignoreCase Applies for the item name check.
     * @param inInv Will also search in the inventory and not only in the hotbar
     * @param mode Specify what to check. 0: display name and item id. 1: only display name. 2: only itemID.
     */
    fun findItem(name: String, ignoreCase: Boolean = false, inInv: Boolean = false, mode: Int = 0): Int? {
        val regex = Regex("${if (ignoreCase) "(?i)" else ""}$name")
        return findItem(regex, inInv, mode)
    }

    /**
     * Returns the first slot where the item name or id passes a check for the regex.
     * The item name is checked to contain the regex.
     * The item id is checked for a full match.
     * Returns null if no matches were found.
     *
     * For case insensitivity use the flag "(?i)":
     *
     *     val regex = Regex("(?i)item name")
     * @param regex regex that has to be matched.
     * @param inInv Will also search in the inventory and not only in the hotbar
     * @param mode Specify what to check. 0: display name and item id. 1: only display name. 2: only itemID.
     */
    fun findItem(regex: Regex, inInv: Boolean = false, mode: Int = 0): Int? {
        return findItem(inInv) {
            it?.run {
                when (mode) {
                    0 -> displayName.contains(regex) || itemID.matches(regex)
                    1 -> displayName.contains(regex)
                    2 -> itemID.matches(regex)
                    else -> false
                }
            } == true
        }
    }

    /**
     * Returns the first slot where the ItemStack matches the [predicate] or null if no matches were found.
     *
     * @param inInv Will also search in the inventory and not only in the hotbar
     */
    fun findItem(inInv: Boolean = false, predicate: (ItemStack?) -> Boolean): Int? {
        for (i in 0..if (inInv) 35 else 8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i)
            if (predicate(stack)) {
                return i
            }
        }
        return null
    }
    //</editor-fold>

    //<editor-fold desc="isHolding">
    /**
     * Check whether the player is holding an item with one of the specified [attributes].
     *
     * [SkyblockItem] is used to determine whether an item meets an attribute.
     */
    fun EntityPlayerSP?.isHolding(vararg attributes: SkyblockItem.Attribute): Boolean {
        val items = SkyblockItem.values().filter { attributes.any { attribute -> it.hasAttribute(attribute) } }
        val regex = Regex(items.joinToString("|") { it.itemID })
        return this.isHolding(regex, 2)
    }

    /**
     * Check whether the player is holding one of the given [items].
     * Returns null if no matches were found.
     *
     */
    fun EntityPlayerSP?.isHolding(vararg items: SkyblockItem): Boolean {
        val regex = Regex(items.joinToString("|") { it.itemID })
        return this.isHolding(regex, 2)
    }

    /**
     * Check whether the player is holding one of the given items.
     * Can check both the name and item ID.
     * @param names The name or item ID.
     * @param ignoreCase Applies for the item name check.
     * @param mode Specify what to check. 0: display name and item id. 1: only display name. 2: only itemID.
     */
    fun EntityPlayerSP?.isHolding(vararg names: String, ignoreCase: Boolean = false, mode: Int = 0): Boolean {
        val regex = Regex("${if (ignoreCase) "(?i)" else ""}${names.joinToString("|")}")
        return this.isHolding(regex, mode)
    }

    /**
     * Check whether the player is holding the given item.
     * Can check both the name and item ID.
     * @param regex regex that has to be matched.
     * @param mode Specify what to check. 0: display name and item id. 1: only display name. 2: only itemID.
     */
    fun EntityPlayerSP?.isHolding(regex: Regex, mode: Int = 0): Boolean {
        return this.isHolding {
            it?.run {
                when (mode) {
                    0 -> displayName.contains(regex) || itemID.matches(regex)
                    1 -> displayName.contains(regex)
                    2 -> itemID.matches(regex)
                    else -> false
                }
            } == true
        }
    }

    /**
     * Check whether the held item is matching the [predicate].
     */
    fun EntityPlayerSP?.isHolding(predicate: (ItemStack?) -> Boolean): Boolean {
        if (this == null) return false
        return predicate(this.heldItem)
    }

    //</editor-folding>
    sealed class ClickType {
        data object Left : ClickType()
        data object Right : ClickType()
        data object Middle : ClickType()
        data object Shift : ClickType()
    }

    private data class WindowClick(val slotId: Int, val button: Int, val mode: Int)

    private val windowClickQueue = mutableListOf<WindowClick>()

    /*init {
        // Used to clear the click queue every 500ms, to make sure it isn't getting filled up.
        Executor(delay = 500) { windowClickQueue.clear() }.register()
    }*/

    fun windowClick(slotId: Int, button: Int, mode: Int /*instant: Boolean = false*/) {
        /* if (instant)*/ sendWindowClick(slotId, button, mode)
//        else windowClickQueue.add(WindowClick(slotId, button, mode))
    }

    /*fun handleWindowClickQueue() {
        if (mc.thePlayer?.openContainer == null) return windowClickQueue.clear()
        if (windowClickQueue.isEmpty()) return
        windowClickQueue.first().apply {
            try {
                sendWindowClick(slotId, button, mode)
            } catch (e: Exception) {
                println("Error sending window click: $this")
                e.printStackTrace()
                windowClickQueue.clear()
            }
        }
        windowClickQueue.removeFirstOrNull()
    }*/

    private fun sendWindowClick(slotId: Int, button: Int, mode: Int) {
        mc.thePlayer.openContainer?.let {
            if (it !is ContainerChest) return@let
            mc.playerController.windowClick(it.windowId, slotId, button, mode, mc.thePlayer)
        }
    }

    /**private fun middleClickWindow(slot: Int) {
    windowClick(slot, 2, 2)
    }*/

    fun windowClick(slotId: Int, clickType: ClickType, /*instant: Boolean = false*/) {
        when (clickType) {
            is ClickType.Left -> windowClick(slotId, 0, 0 /*instant*/)
            is ClickType.Right -> windowClick(slotId, 1, 0 /*instant*/)
            is ClickType.Middle -> windowClick(slotId, 2, 3 /*instant*/)
            is ClickType.Shift -> windowClick(slotId, 0, 1 /*instant*/)
        }
    }

}