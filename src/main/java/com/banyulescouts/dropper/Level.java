package com.banyulescouts.dropper;

import org.bukkit.Location;

public class Level {
    private final Integer level;
    private final Location start;

    public Level(int level, Location start) {
        this.level = level;
        this.start = start;
    }

    public Location getStart() {
        return this.start;
    }

    public Integer getNumber() {
        return this.level;
    }
}
