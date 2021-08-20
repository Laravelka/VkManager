package ru.laravelka.VkManager.Listeners;


import org.bukkit.event.Listener;
import ru.laravelka.VkManager.Main;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import java.io.UnsupportedEncodingException;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.configuration.file.FileConfiguration;

public class ChatListener implements Listener {
    Main plugin;
    Main fetchConfig;
    FileConfiguration config;
    FileConfiguration customConfig;

    public ChatListener(Main plugin) throws UnsupportedEncodingException {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.customConfig = plugin.getCustomConfigs("messages");
    }

    @EventHandler
    public void chatMessage(AsyncPlayerChatEvent event) {
        plugin.getLogger().info("CHAT: " + event.toString());
    }
}
