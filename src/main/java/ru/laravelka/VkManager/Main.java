package ru.laravelka.VkManager;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.HashMap;
import java.io.IOException;
import org.bukkit.entity.Player;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.UnsupportedEncodingException;
import ru.laravelka.VkManager.Libs.VkApi.Client;
import ru.laravelka.VkManager.Listeners.ChatListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;

public class Main extends JavaPlugin {
    private File customConfigFile;
    final Logger log = getLogger();
    private FileConfiguration customConfig;
    final FileConfiguration config = getConfig();
    final Map<UUID, Long> players = new HashMap<>();
    final String locale = config.getString("locale", "ru");
    final Map<String, File> customConfigFiles = new HashMap<>();
    final int messageDelay = config.getInt("message_delay");
    final Map<String, FileConfiguration> customConfigs = new HashMap<>();

    @Override
    public void onEnable() {
        try {
            new ChatListener(this);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        saveDefaultConfig();
        createCustomConfigs("messages");

        this.sendToChat("Плагин запущен");
    }

    @Override
    public void onDisable() {
        this.sendToChat("Плагин выключен");
    }

    public FileConfiguration getCustomConfigs(String name) {
        return this.customConfigs.get(name);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final long timestamp = System.currentTimeMillis();

        if (label.equalsIgnoreCase("vkm")) {
            if (args.length < 1) {
                sender.sendMessage(
                        this.getCustomConfigs("messages")
                                .getString(this.locale + ".usage", "Usage /vkm <§3message>")
                );
                return false;
            }
            String message = args[0];

            if (sender.hasPermission("vkm.reloadConfig") && message.equalsIgnoreCase("reload")) {
                this.reloadConfig();
                this.createCustomConfigs("messages");

                sender.sendMessage(
                        this.getCustomConfigs("messages")
                                .getString(this.locale + ".reloaded", "§2Config has been reloaded!")
                );
                return false;
            }

            if (sender.getName().equals("CONSOLE")) {
                final String[] forbiddenWords = Objects
                        .requireNonNull(config.getString("forbidden_words"))
                        .split(",");

                for (String forbiddenWord : forbiddenWords) {
                    int isForbidden = message.indexOf(forbiddenWord);

                    if (isForbidden != -1) {
                        sender.sendMessage(
                                this.getCustomConfigs("messages")
                                        .getString(
                                                this.locale + ".forbidden_words",
                                                "§cYour message contains forbidden characters/words."
                                        )
                        );
                        return false;
                    }
                }

                if (this.sendToChat("[Сервер] " + message)) {
                    sender.sendMessage(
                            this.getCustomConfigs("messages")
                                    .getString(this.locale + ".sent", "§2The message has been sent!")
                    );
                } else {
                    sender.sendMessage(
                            this.getCustomConfigs("messages")
                                    .getString(this.locale + ".not_sent", "§cThe message was not sent!")
                    );
                }
            } else {
                final Player player = (Player) sender;
                final String[] forbiddenWords = Objects
                        .requireNonNull(config.getString("forbidden_words"))
                        .split(",");

                if (!player.hasPermission("vkm.bypass.forbiddenWords")) {
                    for (String forbiddenWord : forbiddenWords) {
                        int isForbidden = message.indexOf(forbiddenWord);

                        if (isForbidden != -1) {
                            sender.sendMessage(
                                    this.getCustomConfigs("messages")
                                            .getString(
                                                    this.locale + ".forbidden_words",
                                                    "§cYour message contains forbidden characters/words."
                                            )
                            );
                            return false;
                        }
                    }
                }
                String name = player.getName();
                UUID uuid   = player.getUniqueId();

                if (!player.hasPermission("vkm.bypass.delay") && this.players.containsKey(uuid)) {
                    Long getLastUsed = this.players.get(uuid);

                    if ((getLastUsed + this.messageDelay) > timestamp) {
                        int waitSeconds = (int) Math.ceil(
                                ((getLastUsed + this.messageDelay) - timestamp) / 1000
                        );

                        player.sendMessage(
                                this.getCustomConfigs("messages")
                                        .getString(
                                                this.locale + ".message_delay",
                                                "Not so fast! Wait {seconds} sec."
                                        )
                                        .replace("{seconds}", Integer.toString(waitSeconds))
                        );
                        return false;
                    }
                }

                if (this.sendToChat(name + ": " + message)) {
                    this.players.put(uuid, timestamp);
                    sender.sendMessage(
                            this.getCustomConfigs("messages")
                                    .getString(this.locale + ".sent", "The message has been sent!")
                    );
                    sender.sendMessage(
                            this.getCustomConfigs("messages")
                                    .getString(
                                            this.locale + ".user_sent_message",
                                            "The {user} sent a message to the VK - {message}"
                                    )
                                    .replace("{user}", player.getDisplayName())
                                    .replace("{message}", message)
                    );
                } else {
                    sender.sendMessage(
                            this.getCustomConfigs("messages")
                                    .getString(this.locale + ".not_sent", "§cThe message was not sent!")
                    );
                }
            }

        }
        return super.onCommand(sender, command, label, args);
    }

    private boolean sendToChat(String message) {
        int peerId = config.getInt("peer_id");
        String token = config.getString("access_token");
        String version = config.getString("api_version");

        Client api = new Client(token, version);
        String response = api.message(peerId, message);

        return !response.equals("");
    }

    private void createCustomConfigs(String name) {
        this.customConfigFiles.put(name, new File(this.getDataFolder(), name + ".yml"));

        if (!this.customConfigFiles.get(name).exists()) {
            this.customConfigFiles.get(name).getParentFile().mkdirs();
            this.saveResource(name + ".yml", false);
        }
        this.customConfigs.put(name, new YamlConfiguration());

        try {
            this.customConfigs.get(name).load(this.customConfigFiles.get(name));
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }
}