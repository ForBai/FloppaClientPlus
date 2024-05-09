package floppaclient.util;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import floppaclient.utils.ChatUtils;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static floppaclient.FloppaClient.mc;

public class PriceUtils {
    private Map<String, Double> bzBuyPrices;
    private Map<String, Double> bzSellPrices;
    private Map<String, Double> bins;
    private Map<String, Integer> locations;
    private File bzFile;
    private File binFile;

    public PriceUtils() {
        bzBuyPrices = new HashMap<>();
        bzSellPrices = new HashMap<>();
        bins = new HashMap<>();
        locations = new HashMap<>();
        locations.put("AUCTION", 0);
        locations.put("BAZAAR", 1);


        if (bzFile == null) bzFile = new File(mc.mcDataDir, "config/data/bz.json");
        if (binFile == null) binFile = new File(mc.mcDataDir, "config/data/bins.json");

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
        if (bzFile == null || !bzFile.exists()) return;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(bzFile));
            String bzData = bzFile.
                    JsonElement json = new JsonParser().parse(bzData);
            JsonObject obj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonObject value = entry.getValue().getAsJsonObject();
                bzBuyPrices.put(key, value.get("buy").getAsDouble());
                bzSellPrices.put(key, value.get("sell").getAsDouble());
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                chatComponentText.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/floppaclient copy " + e.getMessage()));
                ChatUtils.INSTANCE.modMessage(chatComponentText);
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
    public Integer[] getPrice(String skyblockID, boolean includeLocation) {
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
    public Integer[] getSellPrice(String skyblockID, boolean includeLocation) {
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

    //TODO: getItemUpgradeCost

    //TODO: getItemValue

    public Object getBINPriceAfterTax(double initialPrice) {
        if (initialPrice < 10_000_000) return initialPrice * (1 - 0.01);
        if (initialPrice < 100_000_000) return initialPrice * (1 - 0.02);
        return initialPrice * (1 - 0.025);
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
}

