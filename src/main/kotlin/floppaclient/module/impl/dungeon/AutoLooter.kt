package floppaclient.module.impl.dungeon

import floppaclient.FloppaClient
import floppaclient.FloppaClient.Companion.mc
import floppaclient.module.Category
import floppaclient.module.Module
import floppaclient.module.settings.impl.BooleanSetting
import floppaclient.module.settings.impl.KeybindSetting
import floppaclient.module.settings.impl.Keybinding
import floppaclient.util.PriceUtils
import floppaclient.utils.inventory.ItemUtils.itemID
import floppaclient.utils.inventory.ItemUtils.lore
import net.minecraft.item.ItemStack
import net.minecraft.util.StringUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*
import kotlin.math.floor

object AutoLooter : Module(
    "Auto Looter",
    category = Category.DUNGEON,
    description = "Automatically finds the best chest at the end of a dungeon and buys it."
) {
    var isScanned = false
    var dungeonChests: Array<DungeonChest> = arrayOf()
    var currentPhase: CheckPhase = CheckPhase.NONE

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
        KeybindSetting("Scan Bind", Keybinding(0), description = "Keybind to scan the chests")

    private val isAutoBuyEnabled = BooleanSetting("Auto Buy", true, description = "Automatically buy the best chest")
    private val onlyAutoBuyOnKeyBind = BooleanSetting(
        "Only Buy on Keybind",
        false,
        description = "Only buy the best chest when the keybind is pressed"
    )
    private val keyBind =
        KeybindSetting("Buy Bind", Keybinding(0), description = "Keybind to buy the best chest")

    init {
        this.addSettings(
            isWoodAllowed,
            isGoldAllowed,
            isDiamondAllowed,
            isEmeraldAllowed,
            isObsidianAllowed,
            isBedrockAllowed,
            isAutoBuyEnabled,
            onlyAutoBuyOnKeyBind,
            keyBind
        )
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onChat(event: ClientChatReceivedEvent) {
        if (!FloppaClient.inDungeons || event.type.toInt() == 2) return
        if (StringUtils.stripControlCodes(event.message.unformattedText) == "                             > EXTRA STATS <") {
            if (isAutoScanEnabled.enabled) {
                if (onlyAutoScanOnKeyBind.enabled) {
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
        if (mc.currentScreen != null) return
        when(currentPhase){
            CheckPhase.SCAN_WOOD -> TODO()
            CheckPhase.SCAN_GOLD -> TODO()
            CheckPhase.SCAN_DIAMOND -> TODO()
            CheckPhase.SCAN_EMERALD -> TODO()
            CheckPhase.SCAN_OBSIDIAN -> TODO()
            CheckPhase.SCAN_BEDROCK -> TODO()
            CheckPhase.BUY -> TODO()
            CheckPhase.WAIT_FOR_SCAN_KEY -> TODO()
            CheckPhase.WAIT_FOR_BUY_KEY -> TODO()
            CheckPhase.NONE -> TODO()
        }
    }

    enum class CheckPhase {
        SCAN_WOOD,
        SCAN_GOLD,
        SCAN_DIAMOND,
        SCAN_EMERALD,
        SCAN_OBSIDIAN,
        SCAN_BEDROCK,
        BUY,
        WAIT_FOR_SCAN_KEY,
        WAIT_FOR_BUY_KEY,
        NONE
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
        val type: ChestType,
        val items: Array<DungeonItemDrop>,
        var value: Float,
        val cost: Float,
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
            val match = Regex("/^(\\w+) Essence x(\\d+)\$/").find(name)
            itemID = item.itemID
            if (match != null) {
                val (_, type, amt) = match.destructured
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
            value = floor(((PriceUtils.getSellPrice(this.itemID)[0] ?: 0).toInt() * this.quantity).toFloat()).toInt()
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