package com.banyulescouts.dropper;

import me.tigerhix.lib.scoreboard.type.Scoreboard;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class DropperListener implements Listener {

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        //do nothing if entity is not a player
        if (!(event.getEntity() instanceof Player player)) return;

        //do nothing if player is not playing the dropper minigame
        if (!ArenaManager.isPlaying(player)) return;

        //cancel event
        event.setCancelled(true);

        //check damage is fall damage
        if (!event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) return;

        //teleport player back to top of current level if hasn't reached the water yet
        if (!ArenaManager.isSafe(player, ArenaManager.getCurrentArena(player))) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1F, 1F);
            player.teleport(ArenaManager.getCurrentLevel(ArenaManager.getCurrentArena(player), player).getStart());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1F, 1F);
        }


        //add attempt to player's current run
        ArenaManager.addAttempt(ArenaManager.getCurrentArena(player), player);
    }

    @EventHandler
    public void onLevelComplete(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        //do nothing if player is not playing the dropper minigame
        if (!ArenaManager.isPlaying(player)) return;

        //do nothing if player is not right clicking a block
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        //do nothing if block is not a player head
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!block.getType().equals(Material.PLAYER_HEAD)) return;

        //if clicked on player head - complete level
        ArenaManager.completedLevel(ArenaManager.getCurrentArena(player), player);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        //do nothing if player isn't playing dropper
        if (!ArenaManager.isPlaying(event.getPlayer())) return;

        //else prevent block from being broken
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        //do nothing if player isn't playing dropper
        if (!ArenaManager.isPlaying(event.getPlayer())) return;

        //else prevent block from being placed
        event.setCancelled(true);
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        //do nothing if entity is not a player
        if (!(event.getEntity() instanceof Player player)) return;

        //do nothing if player is not playing the dropper minigame
        if (!ArenaManager.isPlaying(player)) return;

        //cancel food level change
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        //do nothing if player is not playing the dropper minigame
        if (!ArenaManager.isPlaying(player)) return;

        //do nothing if player is not right clicking air
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR)) return;

        //get item player is holding
        ItemStack item = player.getInventory().getItemInMainHand();
        Material material = item.getType();

        //do various things depending on item being held
        switch (material) {
            case RED_BED:
                ArenaManager.quitRun(player); return;
            case ENDER_EYE:
                ArenaManager.spectate(player); return;
            case PAPER:
                ArenaManager.restartArena(player); return;
            default:
        }
    }

    @EventHandler
    public void inWater(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        //do nothing if player is not playing the dropper minigame
        if (!ArenaManager.isPlaying(player)) return;

        //make player safe if in water
        Location loc = player.getLocation();
        if (loc.getBlock().getType() == Material.WATER) ArenaManager.setSafe(player, true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!ArenaManager.scoreboards.containsKey(player)) ArenaManager.scoreboards.put(player, ArenaManager.createScoreboard(player));

        if (player.getWorld().getName().equalsIgnoreCase("dropper")) {
            ArenaManager.players.remove(player);
            player.teleport(ArenaManager.lobby);
        }
        ArenaManager.updateJoinSigns();
    }

    @EventHandler
    public void enterDropperWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (event.getPlayer().getWorld().getName().equalsIgnoreCase("dropper")) {
            if (!ArenaManager.scoreboards.containsKey(player)) ArenaManager.createScoreboard(player);
            ArenaManager.scoreboards.get(player).activate();
        }
        else {
            Optional.ofNullable(ArenaManager.scoreboards.remove(player)).ifPresent(Scoreboard::deactivate);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (ArenaManager.isPlaying(event.getPlayer())) ArenaManager.quitRun(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        if (ArenaManager.isPlaying(event.getPlayer())) ArenaManager.quitRun(event.getPlayer());
    }

    @EventHandler
    public void signClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equalsIgnoreCase("hub")) return;
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof WallSign) && !(block.getState() instanceof Sign)) return;
        Arena arena = ArenaManager.isJoinSign(block.getLocation());
        if (arena == null) return;
        else {
            Boolean joined = ArenaManager.startRun(arena, player);
            if (!joined) player.sendMessage(ChatColor.RED+"This arena is not finished yet");
        }
    }
}
