package com.github.updatedvoxelmap.plugin;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class VoxelMapPlugin extends JavaPlugin implements Listener {
    private final String VOXELMAP_SETTINGS_CHANNEL = "voxelmap:settings";

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, VOXELMAP_SETTINGS_CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        reloadConfig();
    }

    public void sendVoxelMapSettingsToPlayer(Player player) {
        if (player.getListeningPluginChannels().contains(VOXELMAP_SETTINGS_CHANNEL)) {
            HashMap<String, Object> settings = new HashMap<>();
            if (getWorldConfigValue("sendWorldName", player)) {
                settings.put("worldName", getWorldConfigString("worldNamePrefix", player) + player.getWorld().getName());
            }
            boolean allowRadarMobs = getWorldConfigValue("allowRadarMobs", player);
            settings.put("radarMobsAllowed", allowRadarMobs);
            boolean allowRadarPlayers = getWorldConfigValue("allowRadarPlayers", player);
            settings.put("radarPlayersAllowed", allowRadarPlayers);
            settings.put("radarAllowed", allowRadarMobs && allowRadarPlayers);
            settings.put("cavesAllowed", getWorldConfigValue("allowCaves", player));
            settings.put("minimapAllowed", getWorldConfigValue("allowMinimap", player));
            settings.put("worldmapAllowed", getWorldConfigValue("allowWorldmap", player));
            settings.put("waypointsAllowed", getWorldConfigValue("allowWaypoints", player));
            settings.put("deathWaypointAllowed", getWorldConfigValue("allowDeathWaypoint", player));
            String teleportCommand = getWorldConfigString("teleportCommand", player);
            if (!teleportCommand.isEmpty()) {
                settings.put("teleportCommand", teleportCommand);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MinecraftDataOutputStream dataOut = new MinecraftDataOutputStream(out);
            try {
                dataOut.write(0); // channelid, always 0
                dataOut.writeString(new Gson().toJson(settings));
                dataOut.flush();
                player.sendPluginMessage(this, VOXELMAP_SETTINGS_CHANNEL, out.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getWorldConfigString(String setting, Player player) {
        return getConfig().getString("worlds." + player.getWorld().getName() + "." + setting, getConfig().getString("default." + setting, ""));
    }

    public boolean getWorldConfigValue(String setting, Player player) {
        String s = getWorldConfigString(setting, player);
        return s.equals("true") ? true : (s.equals("false") ? false : player.hasPermission(s));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        reloadConfig();
        for (Player player : getServer().getOnlinePlayers()) {
            sendVoxelMapSettingsToPlayer(player);
        }
        sender.sendMessage("[" + getName() + "] Settings reloaded.");
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        getServer().getScheduler().runTaskLater(this, () -> {
            sendVoxelMapSettingsToPlayer(e.getPlayer());
        }, 1);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        getServer().getScheduler().runTaskLater(this, () -> {
            sendVoxelMapSettingsToPlayer(e.getPlayer());
        }, 1);
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent e) {
        if (e.getChannel().equals(VOXELMAP_SETTINGS_CHANNEL)) {
            getServer().getScheduler().runTaskLater(this, () -> {
                sendVoxelMapSettingsToPlayer(e.getPlayer());
            }, 1);
        }
    }
}
