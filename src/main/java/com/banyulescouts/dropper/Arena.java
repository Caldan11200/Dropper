package com.banyulescouts.dropper;

import java.util.ArrayList;
import java.util.List;

public class Arena {

    private final String id;
    private List<Level> levels;
    private Boolean isReady;

    public Arena(String id) {
        this.id = id;
        this.isReady = false;
    }

    public Level getLevel(int level) {
        return levels.get(level);
    }

    public Integer getLevelCount() {
        if (this.levels == null) return 0;
        return this.levels.size();
    }

    public void addLevel(Level level) {
        if (this.levels == null) this.levels = new ArrayList<>();
        this.levels.add(level);
    }

    public List<Level> getLevels() {
        return this.levels;
    }

    public Boolean isReady() {
        return this.isReady;
    }

    public void setReady(Boolean value) {
        this.isReady = value;
    }

    public String getName() {
        return this.id;
    }
}
