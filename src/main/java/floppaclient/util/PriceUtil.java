package floppaclient.util;


import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import floppaclient.utils.ChatUtils;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static floppaclient.FloppaClient.mc;

public class PriceUtils {
    private Map<String, Float> bzBuyPrices = new HashMap<>();
    private Map<String, Float> bzSellPrices = new HashMap<>();
    private Map<String, Float> bins = new HashMap<>();
    public static final int AUCTION = 0;
    public static final int BAZAAR = 1;
    private File bzFile;
    private File binFile;
    private long lastUpdated = System.currentTimeMillis();

    public PriceUtils() {
        bzFile = new File(mc.mcDataDir, "config/data/bz.json");
        binFile = new File(mc.mcDataDir, "config/data/bins.json");

        if (!bzFile.exists()) {
            try {
                bzFile.getParentFile().mkdirs();
                bzFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!binFile.exists()) {
            try {
                binFile.getParentFile().mkdirs();
                binFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        loadFromBZFile();
        loadFromBinFile();
    }

    /**
     * Loads the last saved bazaar buy/sell prices from file.
     */
    private void loadFromBZFile() {
        if (bzFile == null || !bzFile.exists()) return;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(bzFile));
            String bzData = reader.lines().toString();
            JsonElement json = new JsonParser().parse(bzData);
            JsonObject obj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonObject value = entry.getValue().getAsJsonObject();
                bzBuyPrices.put(key, value.get("buy").getAsFloat());
                bzSellPrices.put(key, value.get("sell").getAsFloat());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the last saved BINs from file.
     */
    private void loadFromBinFile() {
        if (binFile == null || !binFile.exists()) return;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(binFile));
            String binData = reader.lines().toString();
            JsonElement json = new JsonParser().parse(binData);
            JsonObject obj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                bins.put(key, value.getAsFloat());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Requests Hypixel's Bazaar API and Moulberry's lowestbin.json file and updates the price data.
     */
    public void update() {
        // Update Bazaar Prices
        OkHttpClient client = new OkHttpClient();
        Request bzRequest = new Request.Builder()
                .url("https://api.hypixel.net/skyblock/bazaar")
                .build();
        client.newCall(bzRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                if (response.body() == null) return;
                JsonElement json = new JsonParser().parse(response.body().string());
                JsonObject obj = json.getAsJsonObject();
                if (!obj.has("success") || !obj.get("success").getAsBoolean()) return;
                JsonObject products = obj.getAsJsonObject("products");
                Map<String, Double> prices = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
                    String key = entry.getKey();
                    JsonObject product = entry.getValue().getAsJsonObject();
                    prices.put(key, product.get("quick_status").getAsJsonObject().get("buyPrice").getAsDouble());
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter(bzFile));
                writer.write(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(prices));
                loadFromBZFile();
            }
        });
        // Update BINs
        Request binRequest = new Request.Builder()
                .url("https://moulberry.codes/lowestbin.json")
                .build();
        client.newCall(binRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                ChatComponentText chatComponentText = new ChatComponentText("Failed to update BINs. Click [here] to copy error to clipboard.");
                chatComponentText.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/floppaclient copy " + e.getMessage()));
                ChatUtils.INSTANCE.modMessage(chatComponentText);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                if (response.body() == null) return;
                JsonElement json = new JsonParser().parse(response.body().string());
                BufferedWriter writer = new BufferedWriter(new FileWriter(binFile));
                writer.write(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json));
                loadFromBinFile();
            }
        });
    }

    /**
     * Gets the buy price or BIN of a Skyblock item.
     * If includeLocation is true, it will return something like [ITEM_PRICE, PriceUtils.locations.BAZAAR] or null if no price is found
     *
     * @param skyblockID
     * @returns {Number | [Number, Integer] | null}
     */
    public Float[] getPrice(String skyblockID) {
        if (bzBuyPrices.containsKey(skyblockID)) {
            return new Float[]{bzBuyPrices.get(skyblockID), (float) BAZAAR};
        }
        if (bins.containsKey(skyblockID)) {
            return new Float[]{bins.get(skyblockID), (float) AUCTION};
        }
        return null;
    }

    /**
     * Gets the sell price or lowest BIN of a Skyblock item.
     * If includeLocation is true, it will return something like [ITEM_PRICE, PriceUtils.locations.BAZAAR] or null if no price is found
     *
     * @param skyblockID
     * @returns {Number | [Number, Integer] | null}
     */
    public Float[] getSellPrice(String skyblockID) {
        if (bzSellPrices.containsKey(skyblockID)) {
            return new Float[]{bzSellPrices.get(skyblockID), (float) BAZAAR};
        }
        if (bins.containsKey(skyblockID)) {
            return new Float[]{bins.get(skyblockID), (float) AUCTION};
        }
        return null;
    }


    public int getUpgradeCost(List<JsonObject> requirements) {
        int cost = 0;
        for (JsonObject requirement : requirements) {
            String type = requirement.get("type").getAsString();
            int amount = requirement.has("amount") ? requirement.get("amount").getAsInt() : 1;

            switch (type) {
                case "ESSENCE":
                    cost += (int) (getPrice("ESSENCE_" + requirement.get("essence_type").getAsString())[0] * amount);
                    break;
                case "ITEM":
                    cost += (int) (getPrice(requirement.get("item_id").getAsString())[0] * amount);
                    break;
                case "COINS":
                    cost += amount;
                    break;
            }
        }
        return cost;
    }
//TODO: implement getSbApiItemData
//    public int getItemValue(ItemStack itemStack, boolean returnBreakdown) {
//        NBTTagCompound nbt = itemStack.getTagCompound();
//        if (nbt == null || !nbt.hasKey("ExtraAttributes")) return 0;
//        NBTTagCompound extraAttributes = nbt.getCompoundTag("ExtraAttributes");
//
//        Map<String, Float> values = new HashMap<>();
//        String itemID = null;
//
//        if (extraAttributes.hasKey("id")) {
//            itemID = extraAttributes.getString("id");
//        }
//
//        // The item itself
//        values.put("base", (float) getPrice(itemID, false));
//
//        // Recomb
//        if (extraAttributes.hasKey("rarity_upgrades")) {
//            values.put("recomb", (float) extraAttributes.getInteger("rarity_upgrades") * getPrice("RECOMBOBULATOR_3000", false));
//        }
//
//        // Runes
//        if (extraAttributes.hasKey("runes")) {
//            NBTTagCompound runes = extraAttributes.getCompoundTag("runes");
//            for (String key : runes.getKeySet()) {
//                values.put(key + "_RUNE", (float) getPrice(key + "_RUNE;" + runes.getInteger(key), false));
//            }
//        }
//
//        // Scrolls
//        if (extraAttributes.hasKey("ability_scroll")) {
//            NBTTagList scrolls = extraAttributes.getTagList("ability_scroll", Constants.NBT.TAG_ANY_NUMERIC);
//            for (int i = 0; i < scrolls.tagCount(); i++) {
//                values.put("scrolls", (float) getPrice(scrolls.getStringTagAt(i), false));
//            }
//        }
//
//        // Hot potato books and fumings
//        if (extraAttributes.hasKey("hot_potato_count")) {
//            int hotPotatoCount = extraAttributes.getInteger("hot_potato_count");
//            values.put("hpb", (float) getPrice("HOT_POTATO_BOOK", false) * Math.min(hotPotatoCount, 10));
//            values.put("fuming", (float) getPrice("FUMING_POTATO_BOOK", false) * Math.max(hotPotatoCount - 10, 0));
//        }
//
//        // Dungeon Stars etc
//        int upgrades = 0;
//        if (extraAttributes.hasKey("dungeon_item_level")) {
//            upgrades = extraAttributes.getInteger("dungeon_item_level");
//        } else if (extraAttributes.hasKey("upgrade_level")) {
//            upgrades = extraAttributes.getInteger("upgrade_level");
//        }
//
//        JsonObject itemJson = getSbApiItemData(itemID);
//        if (upgrades > 0 && itemID != null) {
//            JsonArray upgradeCosts = itemJson.get("upgrade_costs").getAsJsonArray();
//
//            // Is a dungeon item, can have master stars
//            if (upgrades > upgradeCosts.size() && extraAttributes.hasKey("dungeon_item") && extraAttributes.getInteger("dungeon_item") == 1) {
//                int mStars = upgrades - upgradeCosts.size();
//                values.put("masterStars", (float) 0);
//                for (int i = 0; i < mStars; i++) {
//                    values.put("masterStars", (float) getPrice(masterStars[i], false));
//                }
//                upgrades = upgradeCosts.size();
//            }
//
//            // Normal stars and other upgrades
//            for (int i = 0; i < upgrades; i++) {
//                JsonObject tier = upgradeCosts.get(i).getAsJsonObject();
//                for (Map.Entry<String, JsonElement> entry : tier.entrySet()) {
//                    values.put(entry.getKey(), (float) getUpgradeCost(Arrays.asList(entry.getValue().getAsJsonArray().get(0).getAsJsonObject())));
//                }
//            }
//        }
//
//        // Enchantments value
//        if (extraAttributes.hasKey("enchantments")) {
//            NBTTagCompound enchants = extraAttributes.getCompoundTag("enchantments");
//            for (String key : enchants.getKeySet()) {
//                int enchantLevel = enchants.getInteger(key);
//                int enchantPrice = getPrice("ENCHANTMENT_" + key.toUpperCase() + "_" + enchantLevel, false);
//
//                if (key.equalsIgnoreCase("efficiency") && enchantLevel > 5) {
//                    enchantPrice = getPrice("SIL_EX", false) * (enchantLevel - 5);
//                }
//
//                // Scavenger 5 is worthless on dropped dungeon loot
//                if (key.equalsIgnoreCase("scavenger") && extraAttributes.hasKey("baseStatBoostPercentage")) {
//                    enchantPrice = 0;
//                }
//
//                values.put(key.toUpperCase() + "_" + enchantLevel, (float) enchantPrice);
//            }
//        }
//
//        // Gemstones and unlocking slots
//        if (extraAttributes.hasKey("gems")) {
//            NBTTagCompound gems = extraAttributes.getCompoundTag("gems");
//            if (gems.hasKey("unlocked_slots") && itemJson != null) {
//                NBTTagList unlockedSlots = gems.getTagList("unlocked_slots", Constants.NBT.TAG_ANY_NUMERIC);
//                for (int i = 0; i < unlockedSlots.tagCount(); i++) {
//                    int slot = (int) unlockedSlots.getFloatAt(i);
//                    // possible bug
//                    for (int j = 0; j < itemJson.get("gemstone_slots").getAsJsonArray().size(); j++) {
//                        JsonObject slotType = itemJson.get("gemstone_slots").getAsJsonArray().get(i).getAsJsonObject();
//                        if (slotType.get("slot_type").getAsString().equalsIgnoreCase(slot + "")) {
//                            List<JsonObject> requirements = new ArrayList<>();
//                            for (JsonElement requirement : slotType.get("requirements").getAsJsonArray()) {
//                                requirements.add(requirement.getAsJsonObject());
//                            }
//                            values.put("gemstoneUnlocks", (float) getUpgradeCost(requirements));
//                            break;
//                        }
//                    }
//                }
//            }
//
//            // The cost of the actual gemstones
//            for (String key : gems.getKeySet()) {
//                NBTBase tag = gems.getTag(key);
//                if (!(tag instanceof NBTTagString)) continue;
//
//                String[] parts = key.split("_");
//                String gemQuality = ((NBTTagString) tag).getString();
//                String gem = parts[0];
//
//                values.put(gem.toUpperCase() + "_" + gemQuality.toUpperCase() + "_GEM", (float) getPrice(gem.toUpperCase() + "_" + gemQuality.toUpperCase() + "_GEM", false));
//            }
//        }
//
//        // Dyes
//        if (extraAttributes.hasKey("dye_item")) {
//            values.put("dye", (float) getPrice(extraAttributes.getString("dye_item"), false));
//        }
//
//        // Etherwarp
//        if (extraAttributes.hasKey("ethermerge")) {
//            values.put("etherwarp", (float) getPrice("ETHERWARP_MERGER", false) + getPrice("ETHERWARP_CONDUIT", false));
//        }
//
//        // Transmission Tuners
//        if (extraAttributes.hasKey("tuned_transmission")) {
//            values.put("transmissionTuners", (float) getPrice("TRANSMISSION_TUNER", false) * extraAttributes.getInteger("tuned_transmission"));
//        }
//
//        if (extraAttributes.hasKey("art_of_war_count")) {
//            values.put("artOfWar", (float) getPrice("THE_ART_OF_WAR", false) * extraAttributes.getInteger("art_of_war_count"));
//        }
//
//        int totalValue = 0;
//        for (Map.Entry<String, Float> entry : values.entrySet()) {
//            totalValue += entry.getValue();
//        }
//
//        if (returnBreakdown) {
//            return totalValue;
//            //TODO: implement return breakdown
////            return new Integer[]{totalValue, values};
//        }
//        return totalValue;
//    }

    public int getBINPriceAfterTax(int initialPrice) {
        if (initialPrice < 10000000) return initialPrice * 99 / 100;
        if (initialPrice < 100000000) return initialPrice * 98 / 100;
        return initialPrice * 975 / 1000;
    }

    //TODO: redo getBookPriceWhenCrafted
        /*public Object getBookPriceWhenCrafted(ItemStack item, boolean instaSell) {
            String itemID = ItemUtils.getSkyblockItemID(item);
            if (itemID == null) return null;

            Matcher match = Pattern.compile("^(.+?)(\\d+)$").matcher(itemID);
            if (!match.find()) return null;

            String enchantNoTier = match.group(1);
            int tier = Integer.parseInt(match.group(2));

            int maxTier = tier;
            while (getPrice(enchantNoTier + (maxTier + 1)) != null) maxTier++;

            String maxTierId = enchantNoTier + maxTier;

            int t1sForCurrent = (int) Math.pow(2, tier - 1);
            int t1sForMax = (int) Math.pow(2, maxTier - 1);
            double maxTierValue = instaSell ? getSellPrice(maxTierId) : getPrice(maxTierId);


            return maxTierValue / t1sForMax * t1sForCurrent;
        }*/

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (System.currentTimeMillis() - lastUpdated < 1.2e6) return;
            lastUpdated = System.currentTimeMillis();
            update();
            System.out.println("Updated prices");

        }
    }
}