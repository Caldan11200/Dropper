package com.banyulescouts.dropper;

import me.tigerhix.lib.scoreboard.ScoreboardLib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Wall;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class Dropper extends JavaPlugin {

    public static DataManager data;
    public static Dropper PLUGIN;

    public static List<Arena> arenas = new ArrayList<>();

    @Override
    public void onEnable() {
        PLUGIN = this;

        //set scoreboard instance
        ScoreboardLib.setPluginInstance(this);

        //config files
        data = new DataManager();
        ArenaManager.loadArenas();
        ArenaManager.loadPlayers();
        ArenaManager.loadSigns();

        //add listener
        Bukkit.getPluginManager().registerEvents(new DropperListener(), this);
    }

    @Override
    public void onDisable() {
        //save config files
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (ArenaManager.isPlaying(player)) ArenaManager.quitRun(player);
        }
        ArenaManager.saveSigns();
    }

    public static Dropper getPlugin() {
        return PLUGIN;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!label.equalsIgnoreCase("dropper")) return true;

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED+"This command cannot be executed via console");
        }

        if (sender instanceof Player player) {
            if (args.length > 0) {
                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("addlevel")) {
                        if (!player.hasPermission("dropper.setup")) {
                            player.sendMessage(ChatColor.RED+"You do not have permission to perform this command");
                            return true;
                        }
                        ArenaManager.addLevel(player, player.getLocation());
                        player.sendMessage(ChatColor.WHITE+"Level added, please use "+ChatColor.AQUA+"/dropper addlevel"+ChatColor.WHITE+" to add more or use "+
                                ChatColor.AQUA+"/dropper finish"+ChatColor.WHITE+" to complete the setup");
                    }
                    else if (args[0].equalsIgnoreCase("finish")) {
                        if (!player.hasPermission("dropper.setup")) {
                            player.sendMessage(ChatColor.RED+"You do not have permission to perform this command");
                            return true;
                        }
                        ArenaManager.finishArena(player);
                        player.sendMessage(ChatColor.WHITE+"Arena has successfully been added!");
                    }
                }
                if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("create")) {
                        if (!player.hasPermission("dropper.setup")) {
                            player.sendMessage(ChatColor.RED+"You do not have permission to perform this command");
                            return true;
                        }
                        Boolean created = ArenaManager.createArena(player, args[1]);
                        if (created) {
                            player.sendMessage(ChatColor.AQUA+args[1]+ChatColor.WHITE+" has now been created");
                            player.sendMessage(ChatColor.WHITE+"Please use "+ChatColor.AQUA+"/dropper addlevel"+ChatColor.WHITE+" to add levels to the arena");
                        }
                        else player.sendMessage(ChatColor.RED+"You are already creating an arena");
                    }
                    else if (args[0].equalsIgnoreCase("join")) {
                        Boolean joined = ArenaManager.startRun(ArenaManager.getArena(args[1]), player);
                        if (!joined) player.sendMessage(ChatColor.RED+"This arena is not finished yet");
                    }
                    else if (args[0].equalsIgnoreCase("addjoinsign")) {
                        Block block = player.getTargetBlock(null, 20);
                        if (block == null) {
                            player.sendMessage(ChatColor.RED+"You are not looking at a block");
                            return true;
                        }
                        if (block.getState() instanceof Sign) {
                            Location loc = block.getLocation();
                            ArenaManager.joinSigns.put(loc, ArenaManager.getArena(args[1]));
                            ArenaManager.updateJoinSigns();
                            ArenaManager.saveSigns();
                            player.sendMessage(ChatColor.WHITE+"Join sign successfully added");
                            return true;
                        }
                        else {
                            player.sendMessage(ChatColor.RED+"You are not looking at a sign");
                            return true;
                        }
                    }
                }
            }
        }

        // join command   (/dropper join [arena]
            // check arena name or join random
            // add to current players
            // teleport player in

        //leave command     (/dropper leave)
            // remove from current players
            // teleport player to minigames lobby


        return true;
    }
}
