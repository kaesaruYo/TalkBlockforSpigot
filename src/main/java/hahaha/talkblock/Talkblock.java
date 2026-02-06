package hahaha.talkblock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Talkblock extends JavaPlugin implements Listener {

    private final Map<UUID, String> talkblocked = new HashMap<>();
    private static final String PREFIX = ChatColor.LIGHT_PURPLE + "[Sakura SYSTEM] " + ChatColor.RESET;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadTalkblocks();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Talkblock plugin enabled.");
    }

    @Override
    public void onDisable() {
        saveTalkblocks();
        getLogger().info("Talkblock plugin disabled.");
    }

    private void loadTalkblocks() {
        FileConfiguration config = getConfig();
        if (!config.isConfigurationSection("talkblocks")) return;

        for (String key : config.getConfigurationSection("talkblocks").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String reason = config.getString("talkblocks." + key);
                if (reason != null) {
                    talkblocked.put(uuid, reason);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveTalkblocks() {
        FileConfiguration config = getConfig();
        config.set("talkblocks", null); // 一旦リセット

        for (Map.Entry<UUID, String> entry : talkblocked.entrySet()) {
            config.set("talkblocks." + entry.getKey().toString(), entry.getValue());
        }
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp() && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "このコマンドを実行する権限がありません。");
            return true;
        }

        // ===== /tablist =====
        if (command.getName().equalsIgnoreCase("tablist")) {
            if (talkblocked.isEmpty()) {
                sender.sendMessage(PREFIX + ChatColor.YELLOW + "現在Talkblockされているプレイヤーはいません。");
                return true;
            }

            sender.sendMessage(PREFIX + ChatColor.AQUA + "Talkblock中のプレイヤー一覧:");
            for (UUID uuid : talkblocked.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                String name = (p != null) ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                String reason = talkblocked.get(uuid);
                sender.sendMessage(ChatColor.YELLOW + "- " + name + ChatColor.GRAY + " : " + ChatColor.WHITE + reason);
            }
            return true;
        }

        // ===== /tab, /utab =====
        if (args.length < 2) {
            sender.sendMessage(PREFIX + ChatColor.RED + "使い方: /" + label + " <プレイヤー名> <理由>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "指定したプレイヤーが見つかりません（オンライン限定）。");
            return true;
        }

        String reason = String.join(" ", args).substring(args[0].length()).trim();
        String executorName = (sender instanceof Player) ? sender.getName() : "Console";

        if (command.getName().equalsIgnoreCase("tab")) {
            talkblocked.put(target.getUniqueId(), reason);
            saveTalkblocks();

            String message = PREFIX + ChatColor.YELLOW + target.getName() + ChatColor.RESET
                    + "が" + ChatColor.YELLOW + executorName + ChatColor.RESET
                    + "によってTalkblockされました。\n理由: " + ChatColor.WHITE + reason;

            Bukkit.broadcastMessage(message);
            return true;
        }

        if (command.getName().equalsIgnoreCase("utab")) {
            if (!talkblocked.containsKey(target.getUniqueId())) {
                sender.sendMessage(PREFIX + ChatColor.RED + "そのプレイヤーはTalkblockされていません。");
                return true;
            }

            talkblocked.remove(target.getUniqueId());
            saveTalkblocks();

            String message = PREFIX + ChatColor.YELLOW + target.getName() + ChatColor.RESET
                    + "が" + ChatColor.YELLOW + executorName + ChatColor.RESET
                    + "によってTalkblockを解除されました。\n理由: " + ChatColor.WHITE + reason;

            Bukkit.broadcastMessage(message);
            return true;
        }

        return false;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (talkblocked.containsKey(player.getUniqueId())) {
            String reason = talkblocked.get(player.getUniqueId());
            player.sendMessage(PREFIX + ChatColor.RED + "あなたはTalkblockされています。理由: " + ChatColor.WHITE + reason);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!talkblocked.containsKey(player.getUniqueId())) return;

        String message = event.getMessage().toLowerCase();
        // /me コマンドもブロック
        if (message.startsWith("/me ") || message.equals("/me")) {
            String reason = talkblocked.get(player.getUniqueId());
            player.sendMessage(PREFIX + ChatColor.RED + "あなたはTalkblockされています。理由: " + ChatColor.WHITE + reason);
            event.setCancelled(true);
        }
    }
}
