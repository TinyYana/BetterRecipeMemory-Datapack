package io.github.kuohsuanlo.betterrecipememory;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * BetterRecipeMemory datapack 的配套外掛,兩個功能:
 * 1. 吞掉 datapack 移除配方成就後,老玩家首登時原版噴的一次性孤兒清理警告
 *    "Ignored advancement 'minecraft:recipes/...' ... - it doesn't exist anymore?"
 *    (net.minecraft.server.PlayerAdvancements#load;每人最多 ~1572 行,存檔後自動消失)。
 * 2. 靜音配方解鎖 toast:datapack 用隱形成就 fallout:fill_recipe_book 在每分流首訪時
 *    `recipe give @s *` 一次塞滿配方書,客戶端會跳一張「新配方已解鎖」輪播卡。
 *    datapack 拔掉全部配方成就後,伺服器上已不存在任何「玩家該看到的」配方解鎖通知,
 *    所以把 PlayerRecipeDiscoverEvent 的 showNotification 全域設 false(配方照樣寫入配方書,
 *    原版 ServerRecipeBook#addRecipes 對每筆 entry 採用本 event 的 notification 旗標)。
 *    注意這是全域靜音:其他外掛 discoverRecipe 刻意給的提示 toast 也會一併安靜
 *    (刻意取捨——分流首訪的 `recipe give @s *` 會把自訂配方也塞進去,放行任何
 *    namespace 都會讓首訪又跳卡)。
 * 功能 2 只在偵測到 datapack 生效(fallout:fill_recipe_book 存在)時啟用:
 * 沒裝 datapack 的伺服器維持原版通知行為,本外掛只剩功能 1(無意義但無害)。
 */
public class BetterRecipeMemoryPlugin extends JavaPlugin implements Listener {

    static final String AUTHOR_LINE =
            "crafted by 廢土貓大 LogoCat · 廢土 · mcfallout.net";
    static final String REPO =
            "github.com/kuohsuanlo/BetterRecipeMemory-Datapack";

    @Override
    public void onLoad() {
        // onLoad 比 onEnable 早、且早於任何玩家 join(孤兒警告只在 join 時出現),
        // filter 失敗頂多回到「有噪音」的原狀,不能影響開服 → 全吞。
        try {
            OrphanAdvancementLogFilter.install();
            getLogger().info("Orphan-advancement log filter installed.");
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Log filter install failed (non-fatal, noise stays): " + t);
        }
    }

    @Override
    public void onEnable() {
        // 後台作者橫幅 —— datapack + 配套外掛同屬一個作品,一併署名。
        getLogger().info("BetterRecipeMemoryPlugin (datapack companion) —— " + AUTHOR_LINE);
        getLogger().info(REPO);
        if (getCommand("brm") != null) {
            getCommand("brm").setExecutor(this);
        }
        // 只有 datapack 真的生效(隱形成就在 registry 裡)才啟用配方 toast 靜音;
        // 否則這台還有原版配方成就,玩家該看到的解鎖通知不能吃掉。
        if (org.bukkit.Bukkit.getAdvancement(
                java.util.Objects.requireNonNull(org.bukkit.NamespacedKey.fromString("fallout:fill_recipe_book"))) != null) {
            getServer().getPluginManager().registerEvents(this, this);
            getLogger().info("Recipe-unlock toast silencer enabled (BetterRecipeMemory datapack detected).");
        } else {
            getLogger().warning("BetterRecipeMemory datapack not detected (fallout:fill_recipe_book missing); "
                    + "recipe-unlock toast silencer NOT enabled, vanilla notifications stay.");
        }
    }

    // 配方解鎖不再跳「新配方已解鎖」toast(配方書內容不受影響)。datapack 在位時,
    // 來源只剩 fill_recipe_book 的 `recipe give @s *`(每分流首訪一次,含外掛自訂配方)
    // 與管理指令/外掛 discoverRecipe,全都不該吵玩家。
    @EventHandler
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        event.shouldShowNotification(false);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        String sub = args.length == 0 ? "about" : args[0].toLowerCase();
        switch (sub) {
            case "help":
                s.sendMessage(ChatColor.GOLD + "BetterRecipeMemory " + ChatColor.GRAY + "commands:");
                s.sendMessage(ChatColor.YELLOW + "/brm about   " + ChatColor.GRAY + "— author & project card");
                s.sendMessage(ChatColor.YELLOW + "/brm version " + ChatColor.GRAY + "— plugin version");
                s.sendMessage(ChatColor.YELLOW + "/brm help    " + ChatColor.GRAY + "— this list");
                return true;
            case "version":
                s.sendMessage(ChatColor.GOLD + "BetterRecipeMemoryPlugin "
                        + ChatColor.WHITE + getDescription().getVersion());
                return true;
            case "about":
            default:
                s.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "BetterRecipeMemory "
                        + ChatColor.GRAY + "(datapack + companion plugin)");
                s.sendMessage(ChatColor.YELLOW + "—— " + AUTHOR_LINE);
                s.sendMessage(ChatColor.WHITE + "移除原版配方成就 · 保留配方書一鍵合成 · 每玩家成就物件 1688→127");
                s.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + REPO);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("about", "help", "version");
        }
        return java.util.Collections.emptyList();
    }
}
