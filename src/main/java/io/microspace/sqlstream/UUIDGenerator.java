package io.microspace.sqlstream;

import java.util.UUID;

/**
 * @author i1619kHz
 */
public class UUIDGenerator implements IdGenerator<String> {
    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }

    @Override
    public IdType support() {
        return IdType.UUID;
    }
}
