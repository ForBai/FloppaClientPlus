package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.KeybindSetting
import floppaclient.module.settings.impl.Keybinding
import floppaclient.util.PriceUtils
import floppaclient.utils.ChatUtils.modMessage
import floppaclient.utils.ChatUtils.stripControlCodes
import floppaclient.utils.LocationManager
import floppaclient.utils.inventory.InventoryUtils
import floppaclient.utils.inventory.ItemUtils.lore
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemSkull
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.common.util.Constants
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import java.util.*

object AutoLooter : Module(
    "Auto Looter",
    category = Category.DUNGEON,
    description = "Automatically finds the best chest at the end of a dungeon and buys it."
) {
    var isScanned = false
    var dungeonChests: Array<DungeonChest> = arrayOf()
    var bestChest: DungeonChest? = null
    var currentPhase: CheckPhase = CheckPhase.NONE
    var currentScanPhase: ScanPhase = ScanPhase.OPEN_CHEST

    //settings
    private val isWoodAllowed = BooleanSetting("Wood Chests", true, description = "Allow Wood Chests")
    private val isGoldAllowed = BooleanSetting("Gold Chests", true, description = "Allow Gold Chests")
    private val isDiamondAllowed = BooleanSetting("Diamond Chests", true, description = "Allow Diamond Chests")
    private val isEmeraldAllowed = BooleanSetting("Emerald Chests", true, description = "Allow Emerald Chests")
    private val isObsidianAllowed = BooleanSetting("Obsidian Chests", true, description = "Allow Obsidian Chests")
    private val isBedrockAllowed = BooleanSetting("Bedrock Chests", true, description = "Allow Bedrock Chests")

    private val isAutoScanEnabled = BooleanSetting("Auto Scan", true, description = "Automatically scan the chests")
    private val onlyAutoScanOnKeyBind = BooleanSetting(
        "Only Scan on Keybind",
        false,
        description = "Only scan the chests when the keybind is pressed"
    )
    private val scanKeyBind =
        KeybindSetting("Scan Bind", Keybinding(0), description = "Keybind to scan the chests").onPress {
            if (isAutoScanEnabled.enabled && onlyAutoScanOnKeyBind.enabled && currentPhase == CheckPhase.WAIT_FOR_SCAN_KEY) {
                currentPhase = CheckPhase.SCAN_WOOD
            }
        }

    private val isAutoBuyEnabled = BooleanSetting("Auto Buy", true, description = "Automatically buy the best chest")
    private val onlyAutoBuyOnKeyBind = BooleanSetting(
        "Only Buy on Keybind",
        false,
        description = "Only buy the best chest when the keybind is pressed"
    )
    private val buyKeyBind =
        KeybindSetting("Buy Bind", Keybinding(0), description = "Keybind to buy the best chest").onPress {
            if (isAutoBuyEnabled.enabled && onlyAutoBuyOnKeyBind.enabled && currentPhase == CheckPhase.WAIT_FOR_BUY_KEY) {
                currentPhase = CheckPhase.BUY
            }
        }

    private val closeChest = BooleanSetting("Close Chest", false, description = "Close the chest after scanning it")
    private val closeLastChest = BooleanSetting("Close Last Chest", true, description = "Close the last chest after scanning it")

    init {
        this.addSettings(
            isWoodAllowed,
            isGoldAllowed,
            isDiamondAllowed,
            isEmeraldAllowed,
            isObsidianAllowed,
            isBedrockAllowed,
            isAutoScanEnabled,
            onlyAutoScanOnKeyBind,
            scanKeyBind,
            isAutoBuyEnabled,
            onlyAutoBuyOnKeyBind,
            buyKeyBind,
            closeChest,
            closeLastChest
        )
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onChat(event: ClientChatReceivedEvent) {
        if (!FloppaClient.inDungeons || event.type.toInt() == 2) return
        if (StringUtils.stripControlCodes(event.message.unformattedText).matches(Regex("^(\\s+) \\☠ Defeated (.*)\$"))
        ) {
            modMessage("Scanning chests... (End of Dungeon)")
            if (isAutoScanEnabled.enabled) {
                if (onlyAutoScanOnKeyBind.enabled) {
                    modMessage("Press ${Keyboard.getKeyName(scanKeyBind.value.key) ?: "Err"} to scan chests")
                    currentPhase = CheckPhase.WAIT_FOR_SCAN_KEY
                } else if (!onlyAutoScanOnKeyBind.enabled) {
                    currentPhase = CheckPhase.SCAN_WOOD
                }
            }
            return
        }
    }

    @SubscribeEvent
    fun onTickEvent(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!LocationManager.inDungeons) return
        when (currentPhase) {
            CheckPhase.SCAN_WOOD -> {
                if (getChestEntity(ChestType.WOOD) == null) {
                    currentScanPhase = ScanPhase.NEXT_CHEST
                }
                when (currentScanPhase) {
                    ScanPhase.OPEN_CHEST -> {
                        if (isWoodAllowed.enabled && dungeonChests.indexOfFirst { it.type == ChestType.WOOD } == -1) {
                            openChest(ChestType.WOOD)
                            currentScanPhase = ScanPhase.SCAN_CHEST
                        }
                    }

                    ScanPhase.SCAN_CHEST -> {
                        if (dungeonChests.indexOfFirst { it.type == ChestType.WOOD } != -1) {
                            if (closeChest.enabled)  mc.thePlayer.closeScreen()
                            currentScanPhase = ScanPhase.NEXT_CHEST
                        }
                    }

                    ScanPhase.NEXT_CHEST -> {
                        currentPhase = CheckPhase.SCAN_GOLD
                        currentScanPhase = ScanPhase.OPEN_CHEST
                    }
                }
            }

            CheckPhase.SCAN_GOLD -> {
                if (getChestEntity(ChestType.GOLD) == null) {
                    currentScanPhase = ScanPhase.NEXT_CHEST
                }
                when (currentScanPhase) {
                    ScanPhase.OPEN_CHEST -> {
                        if (isGoldAllowed.enabled && dungeonChests.indexOfFirst { it.type == ChestType.GOLD } == -1) {
                            openChest(ChestType.GOLD)
                            currentScanPhase = ScanPhase.SCAN_CHEST
                        }
                    }

                    ScanPhase.SCAN_CHEST -> {
                        if (dungeonChests.indexOfFirst { it.type == ChestType.GOLD } != -1) {
                          if (closeChest.enabled) mc.thePlayer.closeScreen()
                            currentScanPhase = ScanPhase.NEXT_CHEST
                        }
                    }

                    ScanPhase.NEXT_CHEST -> {
                        currentPhase = CheckPhase.SCAN_DIAMOND
                        currentScanPhase = ScanPhase.OPEN_CHEST
                    }
                }
            }

            CheckPhase.SCAN_DIAMOND -> {
                if (getChestEntity(ChestType.DIAMOND) == null) {
                    currentScanPhase = ScanPhase.NEXT_CHEST
                }
                when (currentScanPhase) {
                    ScanPhase.OPEN_CHEST -> {
                        if (isDiamondAllowed.enabled && dungeonChests.indexOfFirst { it.type == ChestType.DIAMOND } == -1) {
                            openChest(ChestType.DIAMOND)
                            currentScanPhase = ScanPhase.SCAN_CHEST
                        }
                    }

                    ScanPhase.SCAN_CHEST -> {
                        if (dungeonChests.indexOfFirst { it.type == ChestType.DIAMOND } != -1) {
                            if (closeChest.enabled) mc.thePlayer.closeScreen()
                            currentScanPhase = ScanPhase.NEXT_CHEST
                        }
                    }

                    ScanPhase.NEXT_CHEST -> {
                        currentPhase = CheckPhase.SCAN_EMERALD
                        currentScanPhase = ScanPhase.OPEN_CHEST
                    }
                }
            }

            CheckPhase.SCAN_EMERALD -> {
                if (getChestEntity(ChestType.EMERALD) == null) {
                    currentScanPhase = ScanPhase.NEXT_CHEST
                }
                when (currentScanPhase) {
                    ScanPhase.OPEN_CHEST -> {
                        if (isEmeraldAllowed.enabled && dungeonChests.indexOfFirst { it.type == ChestType.EMERALD } == -1) {
                            openChest(ChestType.EMERALD)
                            currentScanPhase = ScanPhase.SCAN_CHEST
                        }
                    }

                    ScanPhase.SCAN_CHEST -> {
                        if (dungeonChests.indexOfFirst { it.type == ChestType.EMERALD } != -1) {
                            if (closeChest.enabled) mc.thePlayer.closeScreen()
                            currentScanPhase = ScanPhase.NEXT_CHEST
                        }
                    }

                    ScanPhase.NEXT_CHEST -> {
                        currentPhase = CheckPhase.SCAN_OBSIDIAN
                        currentScanPhase = ScanPhase.OPEN_CHEST
                    }
                }
            }

            CheckPhase.SCAN_OBSIDIAN -> {
                if (getChestEntity(ChestType.OBSIDIAN) == null) {
                    currentScanPhase = ScanPhase.NEXT_CHEST
                }
                when (currentScanPhase) {
                    ScanPhase.OPEN_CHEST -> {
                        if (isObsidianAllowed.enabled && dungeonChests.indexOfFirst { it.type == ChestType.OBSIDIAN } == -1) {
                            openChest(ChestType.OBSIDIAN)
                            currentScanPhase = ScanPhase.SCAN_CHEST
                        }
                    }

                    ScanPhase.SCAN_CHEST -> {
                        if (dungeonChests.indexOfFirst { it.type == ChestType.OBSIDIAN } != -1) {
                            if (closeChest.enabled) mc.thePlayer.closeScreen()
                            currentScanPhase = ScanPhase.NEXT_CHEST
                        }
                    }

                    ScanPhase.NEXT_CHEST -> {
                        currentPhase = CheckPhase.SCAN_BEDROCK
                        currentScanPhase = ScanPhase.OPEN_CHEST
                    }
                }
            }

            CheckPhase.SCAN_BEDROCK -> {
                if (getChestEntity(ChestType.BEDROCK) == null) {
                    currentScanPhase = ScanPhase.NEXT_CHEST
                }

                when (currentScanPhase) {
                    ScanPhase.OPEN_CHEST -> {
                        if (isBedrockAllowed.enabled && dungeonChests.indexOfFirst { it.type == ChestType.BEDROCK } == -1) {
                            openChest(ChestType.BEDROCK)
                            currentScanPhase = ScanPhase.SCAN_CHEST
                        }
                    }

                    ScanPhase.SCAN_CHEST -> {
                        if (dungeonChests.indexOfFirst { it.type == ChestType.BEDROCK } != -1) {
                            if (closeChest.enabled) mc.thePlayer.closeScreen()
                            currentScanPhase = ScanPhase.NEXT_CHEST
                        }
                    }

                    ScanPhase.NEXT_CHEST -> {
                        currentScanPhase = ScanPhase.OPEN_CHEST
                        isScanned = true
                        if (!closeChest.enabled && closeLastChest.enabled) mc.thePlayer.closeScreen()
                        if (isAutoBuyEnabled.enabled) {
                            if (onlyAutoBuyOnKeyBind.enabled) {
                                modMessage("Press ${Keyboard.getKeyName(buyKeyBind.value.key) ?: "Err"} to buy the best chest (" + bestChest?.getFormattedName() + ") for a profit of §l§2${bestChest?.profit}§r that cost §l§6${bestChest?.cost}§r")
                                currentPhase = CheckPhase.WAIT_FOR_BUY_KEY
                            } else if (!onlyAutoBuyOnKeyBind.enabled) {
                                currentPhase = CheckPhase.BUY
                            }
                        }
                    }
                }
            }

            CheckPhase.BUY -> {
                if (bestChest != null) {
                    openChest(bestChest?.type ?: return)
                    currentPhase = CheckPhase.BUY_CLICK
                } else {
                    modMessage("No chest to buy")
                    currentPhase = CheckPhase.NONE
                }
            }

            CheckPhase.BUY_CLICK -> {
                val openContainer = mc.thePlayer.openContainer ?: return
                if (openContainer is ContainerChest
                    && openContainer.lowerChestInventory.displayName.unformattedText.matches(Regex("^(\\w+) Chest(.*)\$"))
                ) {
                    val costItem = openContainer.inventory[31] ?: return
//                    if (costItem.item.registryName == "minecraft:chest") {
                    InventoryUtils.windowClick(31, InventoryUtils.ClickType.Left)
                    modMessage("Bought ${bestChest?.getFormattedName()} for a profit of §l§2${bestChest?.profit}§r that cost §l§6${bestChest?.cost}§r thanks to AutoLooter")
                    currentPhase = CheckPhase.NONE
//                    }
                }
            }

            CheckPhase.WAIT_FOR_SCAN_KEY -> return
            CheckPhase.WAIT_FOR_BUY_KEY -> return
            CheckPhase.NONE -> return
        }
    }

    //    @SubscribeEvent
//    fun chestTick(event: TickEvent.ClientTickEvent) {
    @SubscribeEvent
    fun onGuiOpen(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (!LocationManager.inDungeons) return
        if (event.gui !is GuiChest) return
        val container = (event.gui as GuiChest).inventorySlots
        if (container is ContainerChest) {
            val match = container.lowerChestInventory.displayName.unformattedText.matches(Regex("^(\\w+) Chest(.*)\$"))
            if (!match) return
            val costItem = container.inventory[31] ?: return
            var lootItems: MutableList<ItemStack> = mutableListOf(
                container.inventory[9],
                container.inventory[10],
                container.inventory[11],
                container.inventory[12],
                container.inventory[13],
                container.inventory[14],
                container.inventory[15],
                container.inventory[16],
                container.inventory[17]
            )
            lootItems = lootItems.filter { it.item.registryName != "minecraft:stained_glass_pane" }.toMutableList()
            if (lootItems.isEmpty()) return
            val lore = costItem.lore

            val chestTypeMatcher =
                Regex("^(?<type>\\w+) Chest(.*)\$").find(container.lowerChestInventory.displayName.unformattedText)
            val chestTypeString = chestTypeMatcher?.groups?.get("type")?.value ?: return
            val loot: Array<DungeonItemDrop> = arrayOf()
            dungeonChests.forEach { chest ->
                if (chest.type.name == chestTypeString.uppercase()) {
                    return
                }
            }
            val chest = DungeonChest(ChestType.valueOf(chestTypeString.uppercase()), loot, 0f, 0f, 0f)

            if (lore.isNotEmpty() && lore.size >= 6) {
                val coinCheckRegex = Regex("^([\\d,]+) Coins\$")
                val isCoinsCheck = lore[6].stripControlCodes().matches(coinCheckRegex)
                if (isCoinsCheck) chest.cost =
                    coinCheckRegex.find(lore[6].stripControlCodes())?.groups?.get(1)?.value?.replace(
                        ",",
                        ""
                    )?.toFloat() ?: 0f
            }


            lootItems.forEach { item ->
                val drop = DungeonItemDrop(item, "", 1, "")
                chest.items += drop
            }
//            chest.items = lootItems.map { DungeonItemDrop(it ?: return, "", 0, "") }.toTypedArray()
            chest.calcValueAndProfit()

            val exisingInd = dungeonChests.indexOfFirst { it.type == chest.type }
            if (exisingInd != -1) dungeonChests.sliceArray(IntRange(exisingInd, 1))

            dungeonChests += chest
            bestChest = dungeonChests.maxByOrNull { it.profit }
            modMessage("Scanned ${chest.getFormattedName()} | ${chest.cost} | ${chest.value} | ${chest.profit}")
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        isScanned = false
        dungeonChests = arrayOf()
        bestChest = null
        currentPhase = CheckPhase.NONE
        currentScanPhase = ScanPhase.OPEN_CHEST
    }

    fun openChest(chestType: ChestType) {
        mc.thePlayer.sendQueue.addToSendQueue(
            C02PacketUseEntity(
                getChestEntity(chestType),
                C02PacketUseEntity.Action.INTERACT
            )
        )
    }

    fun getChestEntity(chestType: ChestType): Entity? {
        return mc.theWorld.loadedEntityList.stream().filter { entity -> entity is EntityArmorStand }
            .filter { entity ->
                matchSkullTexture(
                    entity as EntityArmorStand,
                    chestType.getTexture()
                )
            }.findFirst().orElseGet { null }
    }

    fun matchSkullTexture(entity: EntityArmorStand, vararg skullTextures: String): Boolean {
        val helmetItemStack = entity.getCurrentArmor(3)
        if (helmetItemStack != null && helmetItemStack.item is ItemSkull) {
            val textures = helmetItemStack.serializeNBT().getCompoundTag("tag").getCompoundTag("SkullOwner")
                .getCompoundTag("Properties").getTagList("textures", Constants.NBT.TAG_COMPOUND)
            for (i in 0 until textures.tagCount()) {
                if (Arrays.stream(skullTextures).anyMatch { s: String ->
                        textures.getCompoundTagAt(
                            i
                        ).getString("Value") == s
                    }) {
                    return true
                }
            }
        }

        return false
    }

    fun getSkyblockItemID(item: ItemStack): String? {
        val extraAttributes = item.getSubCompound("ExtraAttributes", false)
        val itemID = extraAttributes?.getString("id")
        if (itemID != "ENCHANTED_BOOK") return itemID
        val enchantments = extraAttributes.getCompoundTag("enchantments")
        val enchants = enchantments.keySet
        if (enchants.isEmpty() || enchants == null) return null
        val enchantment = enchants.first()
        val level = enchantments.getInteger(enchants.first())
        return "ENCHANTMENT_" + enchantment.uppercase() + "_" + level
    }

    enum class CheckPhase {
        SCAN_WOOD,
        SCAN_GOLD,
        SCAN_DIAMOND,
        SCAN_EMERALD,
        SCAN_OBSIDIAN,
        SCAN_BEDROCK,
        BUY,
        BUY_CLICK,
        WAIT_FOR_SCAN_KEY,
        WAIT_FOR_BUY_KEY,
        NONE
    }

    enum class ScanPhase {
        OPEN_CHEST,
        SCAN_CHEST,
        NEXT_CHEST,
//        NONE
    }

    private var idsToBuyAlways: Set<String> = setOf(
        "NECRON_HANDLE",
        "DARK_CLAYMORE",
        "SHADOW_WARP_SCROLL",
        "IMPLOSION_SCROLL",
        "WITHER_SHIELD_SCROLL",
        "FIRST_MASTER_STAR",
        "SECOND_MASTER_STAR",
        "THIRD_MASTER_STAR",
        "FOURTH_MASTER_STAR",
        "FIFTH_MASTER_STAR",
        "SHADOW_FURY",
        "ENCHANTMENT_ULTIMATE_ONE_FOR_ALL_1"
    )
    private var neverBuy: Set<String> = setOf(
        "MAXOR_THE_FISH",
        "GOLDOR_THE_FISH",
        "DUNGEON_DISC_5",
        "STORM_THE_FISH"
    )

    enum class ChestType(val textureID: String, val color: String) {
        WOOD(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWE3MzZlYjFhN2NlNWE1ZjVkNWIxNjg5MzFmYTMxMzk2Mzg2YzE2MDU2OGI0MTk2NGJhODZjZGI5ZWQ2YmUifX19",
            "&f"
        ),
        GOLD(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjM3Y2FlNWM1MWViMTU1OGVhODI4ZjU4ZTBkZmY4ZTZiN2IwYjFhMTgzZDczN2VlY2Y3MTQ2NjE3NjEifX19",
            "&6"
        ),
        DIAMOND(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmJiN2IzODVlMGFhNzQ4YTdjNmVmZGYzYmNmMjA5OTI4ZTgyNDcxY2MzNmU5NTQzMzc3NjdjNWUzZDJlMTUwZSJ9fX0K",
            "&b"
        ),
        EMERALD(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTgyNGQ0NTRlMDUwOGM1N2UzZmU4MzM3MDMwYTBiYzUyZTIwNjVlMTlmMDJlMzBiNDU3ZDM1MmQ0YTMxMjUzNyJ9fX0K",
            "&2"
        ),
        OBSIDIAN(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODkzM2VlZmFiOGZjYjNmYTBmNDdiYjAzOTlhNTA4ZWY2YzkxMWRhZTRiMTE0NTU3ZjkwNjg5N2FlY2VkZjg1YSJ9fX0K",
            "&5"
        ),
        BEDROCK(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzRhYzY0NjMzMjQ5ZjEzZjFiMGM5NTI1YzZlZmE0NGNkZTk2YWJjZDY0N2UwOTVhMTcxZmUyNDRjMWEyNDRlNSJ9fX0K",
            "&8"
        );

        fun getTexture(): String {
            return this.textureID
        }

        fun getName(): String {
            return this.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    data class DungeonChest(
        var type: ChestType,
        var items: Array<DungeonItemDrop>,
        var value: Float,
        var cost: Float,
        var profit: Float
    ) {
        fun getChestProfitStr(): String {
            val color = if (this.profit > 0) "&a+" else "&c"
            return "${getFormattedName()} &6(${this.cost})&f: $color${this.profit}"
        }

        fun getChestStr(): String {
            val str = this.items.fold("") { a, b -> a + "  ${b.getPriceStr()}\n" }
            return "${getChestProfitStr()}\n$str"
        }

        fun getFormattedName(): String {
            return "${type.color}&l${type.getName()} Chest&r"
        }

        fun calcValueAndProfit() {
            this.value = this.items.fold(value) { a, b -> a + b.value }
            this.profit = this.value - this.cost
        }
    }

    data class DungeonItemDrop(val item: ItemStack, var name: String, var quantity: Int, var itemID: String) {
        var value = 0

        init {
            val name = item.displayName
            val match = Regex("^(\\w+) Essence x(\\d+)\$").find(name.stripControlCodes())
            itemID = getSkyblockItemID(item) ?: ""
            if (match != null) {
                val (type, amt) = match.destructured
                itemID = "ESSENCE_${type.uppercase()}"
                quantity = amt.toInt()
            }
            if (itemID.startsWith("ENCHANTMENT")) {
                val lore = item.lore
                if (lore.size < 2) {
                    this.name = "Unknown Enchantment"
                } else {
                    this.name = lore[1]
                }
            }
            calcValue()
        }

        fun calcValue() {
            if (neverBuy.contains(itemID)) {
                value = 0
                return
            }
//            modMessage("Calculating value for $itemID")
            val sellPrice = PriceUtils.getSellPrice(itemID.uppercase())
            value = if (sellPrice != null && sellPrice.isNotEmpty()) {
                sellPrice[0]?.toInt() ?: 0
            } else {
                0
            } * this.quantity
        }

        fun getPriceStr(): String {
            val color = when {
                value == 0 -> "&e"
                value < 0 -> "&c-"
                else -> "&a+"
            }

            return "${this.name}&f: ${color}${value}"
        }

        override fun toString(): String {
            return "ChestItem[$itemID, qty=$quantity, value=$value]"
        }
    }


}