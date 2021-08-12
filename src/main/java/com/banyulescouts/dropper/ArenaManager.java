package com.banyulescouts.dropper;

import me.tigerhix.lib.scoreboard.ScoreboardLib;
import me.tigerhix.lib.scoreboard.common.EntryBuilder;
import me.tigerhix.lib.scoreboard.type.Entry;
import me.tigerhix.lib.scoreboard.type.Scoreboard;
import me.tigerhix.lib.scoreboard.type.ScoreboardHandler;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArenaManager {

    public static HashMap<Player, HashMap<Arena, Long>> currentStarts = new HashMap<>();
    public static HashMap<Player, HashMap<Arena, Long>> totalTime = new HashMap<>();
    public static HashMap<Player, HashMap<Arena, Long>> currentTimes = new HashMap<>();

    public static HashMap<Player, Scoreboard> scoreboards = new HashMap<>();
    public static HashMap<Location, Arena> joinSigns = new HashMap<>();

    public static HashMap<Player, HashMap<Arena, Level>> currentLevels = new HashMap<>();
    public static HashMap<Player, HashMap<Arena, Integer>> currentAttempts = new HashMap<>();
    public static HashMap<Player, HashMap<Arena, Boolean>> safeZone = new HashMap<>();
    public static HashMap<Player, Arena> players = new HashMap<>();
    public static HashMap<Player, Arena> setup = new HashMap<>();

    public static Location lobby = new Location(Bukkit.getWorld("hub"), -88.5, 58, 1.5, 90, 0);

    public static ItemStack leave = new ItemStack(Material.RED_BED);
    public static ItemStack spectate = new ItemStack(Material.ENDER_EYE);
    public static ItemStack restart = new ItemStack(Material.PAPER);

    public static Boolean startRun(Arena arena, Player player) {
        if (!arena.isReady()) return false;

        if (players.containsKey(player)) players.replace(player, arena);
        else players.put(player, arena);

        if (currentLevels.containsKey(player)) {
            if (currentLevels.get(player).containsKey(arena)) player.teleport(currentLevels.get(player).get(arena).getStart());
            else {
                currentLevels.get(player).put(arena, arena.getLevel(0));
                player.teleport(arena.getLevel(0).getStart());
            }
        }
        else {
            HashMap<Arena, Level> level = new HashMap<>();
            level.put(arena, arena.getLevel(0));
            currentLevels.put(player, level);
            player.teleport(arena.getLevel(0).getStart());
        }

        getCurrentAttempts(arena, player);

        player.setGameMode(GameMode.ADVENTURE);
        Inventory inv = player.getInventory();
        inv.clear();
        ItemMeta leaveMeta = leave.getItemMeta();
        ItemMeta spectateMeta = spectate.getItemMeta();
        ItemMeta restartMeta = restart.getItemMeta();
        leaveMeta.setDisplayName(ChatColor.RED+"Leave");
        spectateMeta.setDisplayName(ChatColor.BLUE+"Spectate");
        restartMeta.setDisplayName(ChatColor.WHITE+"Restart");
        leave.setItemMeta(leaveMeta);
        spectate.setItemMeta(spectateMeta);
        restart.setItemMeta(restartMeta);
        inv.setItem(0, spectate);
        inv.setItem(4, restart);
        inv.setItem(8, leave);

        updateJoinSigns();

        startTime(player, arena);
        return true;
    }

    public static void quitRun(Player player) {
        Integer currentLevel = getCurrentLevel(getCurrentArena(player), player).getNumber();
        currentLevels.get(player).replace(getCurrentArena(player), getCurrentLevel(getCurrentArena(player), player));
        pauseTime(player, getCurrentArena(player));
        saveRuns(player);
        player.getInventory().clear();
        players.remove(player);
        updateJoinSigns();
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        player.teleport(lobby);
    }

    public static void completedRun(Player player) {
        updateCurrentTimes(player, getCurrentArena(player));
        String time = getTime(player, getCurrentArena(player));
        currentLevels.get(player).replace(getCurrentArena(player), getCurrentArena(player).getLevel(0));
        currentAttempts.get(player).replace(getCurrentArena(player), 1);
        currentStarts.get(player).remove(getCurrentArena(player));
        totalTime.get(player).remove(getCurrentArena(player));
        setSafe(player, false);
        saveRuns(player);
        updateJoinSigns();
        player.sendMessage(ChatColor.WHITE+"Well done! You completed the "+ChatColor.AQUA+getCurrentArena(player).getName()+ChatColor.WHITE+" dropper in "+time+ChatColor.WHITE+"!");
        player.getInventory().clear();
        players.remove(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        player.teleport(lobby);
    }

    public static void spectate(Player player) {

    }

    public static void updateCurrentTimes(Player player, Arena arena) {
        if (currentTimes.containsKey(player)) {
            if (currentTimes.get(player).containsKey(arena)) currentTimes.get(player).replace(arena, getTotalTime(player, arena));
            else currentTimes.get(player).put(arena, getTotalTime(player, arena));
        }
        else {
            HashMap<Arena, Long> c = new HashMap<>();
            c.put(arena, getTotalTime(player, arena));
            currentTimes.put(player, c);
        }
    }

    public static void startTime(Player player, Arena arena) {
        if (currentStarts.containsKey(player)) {
            if (currentStarts.get(player).containsKey(arena)) currentStarts.get(player).replace(arena, System.currentTimeMillis());
            else currentStarts.get(player).put(arena, System.currentTimeMillis());
        }
        else {
            HashMap<Arena, Long> timers = new HashMap<>();
            timers.put(arena, System.currentTimeMillis());
            currentStarts.put(player, timers);
        }
        updateCurrentTimes(player, arena);
    }

    public static Long getStartTime(Player player, Arena arena) {
        if (currentStarts.containsKey(player)) {
            if (!currentStarts.get(player).containsKey(arena)) startTime(player, arena);
            return currentStarts.get(player).get(arena);
        }
        else {
            startTime(player, arena);
            return currentStarts.get(player).get(arena);
        }
    }

    public static void pauseTime(Player player, Arena arena) {
        Long end = System.currentTimeMillis();
        Long total = end - getStartTime(player, arena);

        if (totalTime.containsKey(player)) {
            if (totalTime.get(player).containsKey(arena)) totalTime.get(player).replace(arena, totalTime.get(player).get(arena)+total);
            else totalTime.get(player).put(arena, total);
        }
        else {
            HashMap<Arena, Long> t = new HashMap<>();
            t.put(arena, total);
            totalTime.put(player, t);
        }

        if (currentStarts.containsKey(player)) {
            currentStarts.get(player).remove(arena);
        }
        updateCurrentTimes(player, arena);
    }

    public static Long getTotal(Player player, Arena arena) {
        if (totalTime.containsKey(player)) {
            if (totalTime.get(player).containsKey(arena)) return totalTime.get(player).get(arena);
            else {
                totalTime.get(player).put(arena, 0L);
                return 0L;
            }
        }
        else {
            HashMap<Arena, Long> t = new HashMap<>();
            t.put(arena, 0L);
            totalTime.put(player, t);
            return 0L;
        }
    }

    public static Long getTotalTime(Player player, Arena arena) {
        Long end = System.currentTimeMillis();
        Long current = end - getStartTime(player, arena);
        return current + getTotal(player, arena);
    }

    public static String getTime(Player player, Arena arena) {
        String result = "00:00";
        String ssecs;
        String smins;
        Double seconds = getTotalTime(player, arena) / 1000.0;
        int secs = (int) Math.floor(seconds % 60);
        Double minutes = seconds / 60.0;
        int mins = (int) Math.floor(minutes % 60);

        if (secs < 10) ssecs = "0"+secs;
        else ssecs = secs+"";
        if (mins < 10) smins = "0"+mins;
        else smins = mins+"";

        result = ChatColor.AQUA+smins+ChatColor.GRAY+":"+ChatColor.AQUA+ssecs;
        return result;
    }

    public static void setSafe(Player player, Boolean value) {
        Arena arena = getCurrentArena(player);
        if (safeZone.containsKey(player)) {
            if (safeZone.get(player).containsKey(arena)) safeZone.get(player).replace(arena, value);
            else safeZone.get(player).put(arena, value);
        }
        else {
            HashMap<Arena, Boolean> safe = new HashMap<>();
            safe.put(arena, value);
            safeZone.put(player, safe);
        }
    }

    public static Boolean isSafe(Player player, Arena arena) {
        if (safeZone.containsKey(player)) {
            return safeZone.get(player).getOrDefault(arena, false);
        }
        else {
            HashMap<Arena, Boolean> safe = new HashMap<>();
            safe.put(arena, false);
            safeZone.put(player, safe);
            return false;
        }
    }

    public static void completedLevel(Arena arena, Player player) {
        Integer currentLevel = getCurrentLevel(arena, player).getNumber();
        Integer nextLevel = getCurrentLevel(arena, player).getNumber() + 1;
        Integer maxLevels = getCurrentArena(player).getLevelCount();
        if (currentLevel.equals(maxLevels-1)) completedRun(player);
        else {
            player.teleport(arena.getLevel(nextLevel).getStart());
            currentLevels.get(player).replace(arena, arena.getLevel(nextLevel));
            setSafe(player, false);
            updateCurrentTimes(player, arena);
            saveRuns(player);
            updateJoinSigns();
        }
    }

    public static void restartArena(Player player) {
        currentLevels.get(player).replace(getCurrentArena(player), getCurrentArena(player).getLevel(0));
        currentAttempts.get(player).replace(getCurrentArena(player), 1);
        setSafe(player, false);
        player.teleport(getCurrentArena(player).getLevel(0).getStart());
    }

    public static Boolean isPlaying (Player player) {
        return players.containsKey(player);
    }

    public static Level getCurrentLevel(Arena arena, Player player) {
        return currentLevels.get(player).get(arena);
    }

    public static Arena getCurrentArena(Player player) {
        return players.get(player);
    }

    public static Integer getCurrentAttempts(Arena arena, Player player) {
        if (currentAttempts.containsKey(player)) {
            if (currentAttempts.get(player).containsKey(arena)) return currentAttempts.get(player).get(arena);
            else {
                currentAttempts.get(player).put(arena, 1);
            }
        }
        else {
            HashMap<Arena, Integer> attempts = new HashMap<>();
            attempts.put(arena, 1);
            currentAttempts.put(player, attempts);
        }
        return 1;
    }

    public static void addAttempt(Arena arena, Player player) {
        Integer attempts = getCurrentAttempts(arena, player);
        currentAttempts.get(player).replace(arena, attempts+1);
    }

    public static Boolean createArena(Player player, String name) {
        if (setup.containsKey(player)) return false;
        Arena arena = new Arena(name);
        setup.put(player, arena);
        return true;
    }

    public static void addLevel(Player player, Location location) {
        if (!setup.containsKey(player)) return;
        Arena arena = setup.get(player);
        arena.addLevel(new Level(arena.getLevelCount(), location));
    }

    public static void finishArena(Player player) {
        if (!setup.containsKey(player)) return;
        Arena arena = setup.get(player);
        arena.setReady(true);
        saveArena(arena);
        Dropper.arenas.add(arena);
        setup.remove(player);
    }

    public static void saveArena(Arena arena) {
        for(Level level : arena.getLevels()) {
            Dropper.data.getConfig().set("arenas."+arena.getName()+"."+level.getNumber().toString()+".lvl",level.getNumber());
            Dropper.data.getConfig().set("arenas."+arena.getName()+"."+level.getNumber().toString()+".loc",level.getStart());
        }
        Dropper.data.saveConfig();
        Bukkit.getLogger().info("Saved arena "+arena.getName());
    }

    public static void saveRuns(Player player) {
        for (Map.Entry<Arena, Level> levels : currentLevels.get(player).entrySet()) {
            Dropper.data.getConfig().set("players."+player.getUniqueId()+".levels."+levels.getKey().getName(),levels.getValue().getNumber());
        }
        for (Map.Entry<Arena, Integer> attempts : currentAttempts.get(player).entrySet()) {
            Dropper.data.getConfig().set("players."+player.getUniqueId()+".attempts."+attempts.getKey().getName(),attempts.getValue());
        }
        for (Map.Entry<Arena, Long> times : currentTimes.get(player).entrySet()) {
            Dropper.data.getConfig().set("players."+player.getUniqueId()+".times."+times.getKey().getName(),times.getValue());
        }
        Dropper.data.saveConfig();
    }

    public static void saveSigns() {
        for (Map.Entry<Location, Arena> join : joinSigns.entrySet()) {
            Dropper.data.getConfig().set("signs.join."+join.getValue().getName(),join.getKey());
        }
    }

    public static void loadSigns() {
        if (Dropper.data.getConfig().getConfigurationSection("signs.join") == null) return;
        Dropper.data.getConfig().getConfigurationSection("signs.join").getKeys(false).forEach(arena -> {
            if (arena != null) {
                Location loc = (Location) Dropper.data.getConfig().get("signs.join."+arena);
                joinSigns.put(loc, getArena(arena));
            }
        });
    }

    public static void loadArenas() {
        if (Dropper.data.getConfig().getConfigurationSection("arenas") == null) return;
        Dropper.data.getConfig().getConfigurationSection("arenas").getKeys(false).forEach(key -> {
            Arena arena = new Arena(key);
            arena.setReady(true);
            Dropper.data.getConfig().getConfigurationSection("arenas."+key).getKeys(false).forEach(level -> {
                if (level != null) {
                    Integer lvl = (Integer) Dropper.data.getConfig().get("arenas."+key+"."+level+".lvl");
                    arena.addLevel(new Level(lvl, (Location) Dropper.data.getConfig().get("arenas."+key+"."+level+".loc")));
                }
            });
            Dropper.arenas.add(arena);
        });
    }

    public static Arena getArena(String name) {
        for (Arena arena : Dropper.arenas) {
            if (name.equalsIgnoreCase(arena.getName())) return arena;
        }
        return null;
    }

    public static void loadPlayers() {
        if (Dropper.data.getConfig().getConfigurationSection("players") == null) return;
        Dropper.data.getConfig().getConfigurationSection("players").getKeys(false).forEach(key -> {
            Player player = Bukkit.getPlayer(UUID.fromString(key));
            HashMap<Arena, Level> levels = new HashMap<>();
            HashMap<Arena, Integer> attempts = new HashMap<>();
            HashMap<Arena, Long> times = new HashMap<>();
            if (Dropper.data.getConfig().getConfigurationSection("players.levels."+key) == null) return;
            Dropper.data.getConfig().getConfigurationSection("players.levels."+key).getKeys(false).forEach(arena -> {
                if (arena != null) {
                    Integer level = (Integer) Dropper.data.getConfig().get("players.levels." + key + ".arena");
                    levels.put(getArena(arena), getArena(arena).getLevel(level));
                }
            });
            if (Dropper.data.getConfig().getConfigurationSection("players.attempts."+key) == null) return;
            Dropper.data.getConfig().getConfigurationSection("players.attempts."+key).getKeys(false).forEach(arena -> {
                if (arena != null) attempts.put(getArena(arena), (Integer) Dropper.data.getConfig().get("players.attempts."+key+".arena"));
            });
            if (Dropper.data.getConfig().getConfigurationSection("players.times."+key) == null) return;
            Dropper.data.getConfig().getConfigurationSection("players.times."+key).getKeys(false).forEach(arena -> {
                if (arena != null) times.put(getArena(arena), (Long) Dropper.data.getConfig().get("players.times."+key+".arena"));
            });
            currentLevels.put(player, levels);
            currentAttempts.put(player, attempts);
            totalTime.put(player, times);
        });
    }

    public static Scoreboard createScoreboard(Player player) {
        return ScoreboardLib.createScoreboard(player).setHandler(new ScoreboardHandler() {
            @Override
            public String getTitle(Player player1) {
                return ChatColor.DARK_AQUA+""+ChatColor.BOLD+getCurrentArena(player1).getName();
            }

            @Override
            public List<Entry> getEntries(Player player1) {
                updateCurrentTimes(player1, getCurrentArena(player1));
                return new EntryBuilder()
                        .next("Time: "+getTime(player1, getCurrentArena(player1)))
                        .next("Level: "+ChatColor.AQUA+(getCurrentLevel(getCurrentArena(player), player).getNumber()+1)+ChatColor.GRAY+"/"+ChatColor.AQUA+getCurrentArena(player).getLevelCount())
                        .next("Attempts: "+ChatColor.AQUA+getCurrentAttempts(getCurrentArena(player), player))
                        .blank()
                        .next(ChatColor.DARK_AQUA+"mc.banyulescouts.com")
                        .build();
            }

        }).setUpdateInterval(20L);
    }

    public static int playersInArena(Arena arena) {
        int result = 0;
        for (Map.Entry<Player, Arena> set : players.entrySet()) {
            if (set.getValue().equals(arena)) result++;
        }
        return result;
    }

    public static void updateJoinSigns() {
        for (Map.Entry<Location, Arena> set : joinSigns.entrySet()) {
            Location loc = set.getKey();
            Arena arena = set.getValue();
            if (!(loc.getBlock().getState() instanceof Sign sign)) continue;
            sign.setLine(0, ChatColor.DARK_AQUA+"[Dropper]");
            sign.setLine(1, ChatColor.AQUA+arena.getName());
            sign.setLine(2, ChatColor.WHITE+"Click to join");
            sign.setLine(3, ChatColor.YELLOW+""+playersInArena(arena)+ChatColor.GOLD+" players");
            sign.update();
        }
    }

    public static Arena isJoinSign(Location location) {
        return joinSigns.getOrDefault(location, null);
    }
}
