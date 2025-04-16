package com.minecraft.classesplugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BlockMiningData {

    private final Map<String, com.minecraft.classesplugin.BlockMiningData> minedBlocks = new HashMap<>();
    private static final long BLOCK_COOLDOWN = 3600000; // 1 hora em milissegundos

    private final long timestamp;
    private final boolean wasNaturalBlock;

    public BlockMiningData(long timestamp, boolean wasNaturalBlock) {
        this.timestamp = timestamp;
        this.wasNaturalBlock = wasNaturalBlock;
    }

    private void cleanupMinedBlocks() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, BlockMiningData>> iterator = minedBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BlockMiningData> entry = iterator.next();
            if (currentTime - entry.getValue().getTimestamp() > BLOCK_COOLDOWN) {
                iterator.remove();
            }
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean wasNaturalBlock() {
        return wasNaturalBlock;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > BLOCK_COOLDOWN;
    }
}
