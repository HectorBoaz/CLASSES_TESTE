package com.minecraft.classesplugin;

/**
 * Classe que armazena os dados de classe de um jogador
 */
public class PlayerClassData {
    private String className;
    private int level;
    private int xp;
    private long resetCooldownEnd; // Novo campo para o cooldown de reset

    public PlayerClassData(String className, int level, int xp) {
        this.className = className;
        this.level = level;
        this.xp = xp;
        this.resetCooldownEnd = 0; // Sem cooldown por padrão
    }

    public PlayerClassData(String className, int level, int xp, long resetCooldownEnd) {
        this.className = className;
        this.level = level;
        this.xp = xp;
        this.resetCooldownEnd = resetCooldownEnd;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public void addXp(int amount) {
        this.xp += amount;
    }

    public long getResetCooldownEnd() {
        return resetCooldownEnd;
    }

    public void setResetCooldownEnd(long resetCooldownEnd) {
        this.resetCooldownEnd = resetCooldownEnd;
    }

    public boolean isOnResetCooldown() {
        return System.currentTimeMillis() < resetCooldownEnd;
    }

    public long getRemainingResetCooldown() {
        return Math.max(0, resetCooldownEnd - System.currentTimeMillis());
    }
}