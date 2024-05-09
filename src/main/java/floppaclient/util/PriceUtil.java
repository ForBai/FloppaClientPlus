package floppaclient.util;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import floppaclient.utils.ChatUtils;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.Constants;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceUtils {
    public class PriceUtils {
        private Map<String, Double> bzBuyPrices;
        private Map<String, Double> bzSellPrices;
        private Map<String, Double> bins;
        private Map<String, Integer> locations;

        public PriceUtils() {
            bzBuyPrices = new HashMap<>();
            bzSellPrices = new HashMap<>();
            bins = new HashMap<>();
            locations = new HashMap<>();
            locations.put("AUCTION", 0);
            locations.put("BAZAAR", 1);

            loadFromBZFile();
            loadFromBinFile();

            // Update the prices every 20 mins
            // register("tick", () -> {
            //     if (System.currentTimeMillis() - bcData.priceUtilsLastUpdated < 1.2e6) return;
            //     bcData.priceUtilsLastUpdated = System.currentTimeMillis();
            //     bcData.save();
            //     update();
            // });
        }

        /**
         * Loads the last saved bazaar buy/sell prices from file.
         */
        private void loadFromBZFile() {
            if (!FileLib.exists("BloomCore", "data/bz.json")) return;
            String bzData = FileLib.read("BloomCore", "data/bz.json");
            JsonElement json = new JsonParser().parse(bzData);
            JsonObject obj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonObject value = entry.getValue().getAsJsonObject();
                bzBuyPrices.put(key, value.get("buy").getAsDouble());
                bzSellPrices.put(key, value.get("sell").getAsDouble());
            }
        }

        /**
         * Loads the last saved BINs from file.
         */
        private void loadFromBinFile() {
            if (!FileLib.exists("BloomCore", "data/bins.json")) return;
            String binData = FileLib.read("BloomCore", "data/bins.json");
            JsonElement json = new JsonParser().parse(binData);
            JsonObject obj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                bins.put(key, value.getAsDouble());
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
                    FileLib.write("BloomCore", "data/bz.json", new Gson().toJson(prices));
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
                    chatComponentText.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,""));
                    ChatUtils.INSTANCE.modMessage("Failed to update BINs");
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (!response.isSuccessful()) return;
                    if (response.body() == null) return;
                    JsonElement json = new JsonParser().parse(response.body().string());
                    FileLib.write("BloomCore", "data/bins.json", json.toString());
                    loadFromBinFile();
                }
            });
        }

        /**
         * Gets the buy price or BIN of a Skyblock item.
         * If includeLocation is true, it will return something like [ITEM_PRICE, PriceUtils.locations.BAZAAR] or null if no price is found
         *
         * @param skyblockID
         * @param includeLocation
         * @returns {Number | [Number, Integer] | null}
         */
        public Object getPrice(String skyblockID, boolean includeLocation) {
            if (bzBuyPrices.containsKey(skyblockID)) {
                if (includeLocation) return new Object[]{bzBuyPrices.get(skyblockID), locations.get("BAZAAR")};
                return bzBuyPrices.get(skyblockID);
            }
            if (bins.containsKey(skyblockID)) {
                if (includeLocation) return new Object[]{bins.get(skyblockID), locations.get("AUCTION")};
                return bins.get(skyblockID);
            }
            return null;
        }

        /**
         * Gets the sell price or lowest BIN of a Skyblock item.
         * If includeLocation is true, it will return something like [ITEM_PRICE, PriceUtils.locations.BAZAAR] or null if no price is found
         *
         * @param skyblockID
         * @param includeLocation
         * @returns {Number | [Number, Integer] | null}
         */
        public Object getSellPrice(String skyblockID, boolean includeLocation) {
            if (bzSellPrices.containsKey(skyblockID)) {
                if (includeLocation) return new Object[]{bzSellPrices.get(skyblockID), locations.get("BAZAAR")};
                return bzSellPrices.get(skyblockID);
            }
            if (bins.containsKey(skyblockID)) {
                if (includeLocation) return new Object[]{bins.get(skyblockID), locations.get("AUCTION")};
                return bins.get(skyblockID);
            }
            return null;
        }

        /**
         * Gets the coins required to upgrade by the given requirements (Using the hypixel Skyblock items API data).
         *
         * @param requirements
         * @returns
         */
        public double getUpgradeCost(List<Map<String, Object>> requirements) {
            double cost = 0;
            for (Map<String, Object> upgrade : requirements) {
                String type = (String) upgrade.get("type");
                int amount = (int) upgrade.getOrDefault("amount", 1);

                if (type.equals("ESSENCE")) cost += getPrice("ESSENCE_" + upgrade.get("essence_type")) * amount;
                else if (type.equals("ITEM")) cost += getPrice((String) upgrade.get("item_id")) * amount;
                else if (type.equals("COINS")) cost += amount;
            }
            return cost;
        }

        /**
         * Returns the value of an item taking into account gemstones, recombs, enchants etc.
         *
         * @param itemStack
         * @param returnBreakdown
         * @returns {Number | [Number, Object]}
         */
        public Object getItemValue(ItemStack itemStack, boolean returnBreakdown) {
            if (itemStack == null) return null;

            NBTTagCompound nbt = itemStack.getTagCompound();

            if (nbt == null || !nbt.hasKey("ExtraAttributes")) return null;
            NBTTagCompound extraAttributes = nbt.getCompoundTag("ExtraAttributes");

            // The breakdown of where the value of this item comes from
            Map<String, Double> values = new HashMap<>();
            String itemID = null;

            // The item itself
            if (extraAttributes.hasKey("id")) itemID = extraAttributes.getString("id");
            ItemData itemJson = getSbApiItemData(itemID);

            values.put("base", getPrice(itemID) == null ? 0 : getPrice(itemID));

            // Recomb
            if (extraAttributes.hasKey("rarity_upgrades")) {
                int rarityUpgrades = extraAttributes.getInteger("rarity_upgrades");
                values.put("recomb", rarityUpgrades * getPrice("RECOMBOBULATOR_3000"));
            }

            // Runes
            if (extraAttributes.hasKey("runes")) {
                NBTTagList runes = extraAttributes.getTagList("runes", Constants.NBT.TAG_COMPOUND);
                List<String> keys = new ArrayList<>();
                for (int i = 0; i < runes.tagCount(); i++) {
                    keys.add(runes.getCompoundTagAt(i).getString("rune"));
                }
                for (String key : keys) {
                    int runeLevel = extraAttributes.getCompoundTag("runes").getInteger(key);
                    if (!values.containsKey("runes")) values.put("runes", 0.0);
                    values.put("runes", values.get("runes") + getPrice(key + ";" + runeLevel));
                }
            }

            // Enchants
            if (extraAttributes.hasKey("enchantments")) {
                NBTTagList enchants = extraAttributes.getTagList("enchantments", Constants.NBT.TAG_COMPOUND);
                List<String> keys = new ArrayList<>();
                for (int i = 0; i < enchants.tagCount(); i++) {
                    keys.add(enchants.getCompoundTagAt(i).getString("id"));
                }
                for (String key : keys) {
                    int enchantLevel = extraAttributes.getCompoundTag("enchantments").getInteger(key);
                    if (!values.containsKey("enchants")) values.put("enchants", 0.0);
                    values.put("enchants", values.get("enchants") + getPrice(key + ";" + enchantLevel));
                }
            }

            // Bonus Modifiers
            if (extraAttributes.hasKey("modifier")) {
                String modifier = extraAttributes.getString("modifier");
                int modifierValue = extraAttributes.getInteger("modifier_amount");
                if (!values.containsKey("modifier")) values.put("modifier", 0.0);
                values.put("modifier", values.get("modifier") + getPrice(modifier + ";" + modifierValue));
            }

            // Reforge
            if (extraAttributes.hasKey("reforge")) {
                String reforge = extraAttributes.getString("reforge");
                if (!values.containsKey("reforge")) values.put("reforge", 0.0);
                values.put("reforge", values.get("reforge") + getPrice("REFORGE_" + reforge.toUpperCase()));
            }

            // Talisman Upgrades
            if (itemJson != null && itemJson.isTalisman) {
                String talismanType = itemID.substring(0, itemID.indexOf("_"));
                if (extraAttributes.hasKey("gems")) {
                    int talismanUpgrades = extraAttributes.getInteger("gems");
                    values.put("gems", talismanUpgrades * getPrice("TALISMAN_UPGRADE_" + talismanType.toUpperCase()));
                }
            }

            double value = 0;
            for (double val : values.values()) {
                value += val;
            }

            return returnBreakdown ? new Object[]{value, values} : value;
        }

        public Object getEnchantPrice(String enchant, int level) {
            if (enchantmentPrices.containsKey(enchant + ";" + level)) {
                return enchantmentPrices.get(enchant + ";" + level);
            }
            return null;
        }

        public Object getRunePrice(String rune, int level) {
            if (runePrices.containsKey(rune + ";" + level)) {
                return runePrices.get(rune + ";" + level);
            }
            return null;
        }

        public Object getReforgePrice(String reforge) {
            if (reforgePrices.containsKey("REFORGE_" + reforge.toUpperCase())) {
                return reforgePrices.get("REFORGE_" + reforge.toUpperCase());
            }
            return null;
        }

        public Object getTalismanUpgradePrice(String talismanType, int level) {
            if (talismanUpgradePrices.containsKey("TALISMAN_UPGRADE_" + talismanType.toUpperCase() + "_" + level)) {
                return talismanUpgradePrices.get("TALISMAN_UPGRADE_" + talismanType.toUpperCase() + "_" + level);
            }
            return null;
        }

        public Object getRecombPrice(int rarityUpgrades) {
            return getPrice("RECOMBOBULATOR_3000") * rarityUpgrades;
        }

        public Object getModifierPrice(String modifier, int modifierValue) {
            if (modifierPrices.containsKey(modifier + ";" + modifierValue)) {
                return modifierPrices.get(modifier + ";" + modifierValue);
            }
            return null;
        }

        public Object getBINPriceAfterTax(double initialPrice) {
            if (initialPrice < 10_000_000) return initialPrice * (1 - 0.01);
            if (initialPrice < 100_000_000) return initialPrice * (1 - 0.02);
            return initialPrice * (1 - 0.025);
        }

        public Object getBookPriceWhenCrafted(ItemStack item, boolean instaSell) {
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
        }
    }
}

