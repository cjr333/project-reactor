package service;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class Chunk {
    private List<Long> items;
    private int count;
    private long lastKey;

    public static Chunk getInstance(long idx) {
        Chunk chunk = new Chunk();
        chunk.items = Collections.singletonList(idx);
        chunk.count = 1;
        chunk.lastKey = idx;
        return chunk;
    }
}
