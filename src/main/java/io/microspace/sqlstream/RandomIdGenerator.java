package io.microspace.sqlstream;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author i1619kHz
 */
public class RandomIdGenerator implements IdGenerator<Long> {

    @Override
    public Long generate() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Override
    public IdType support() {
        return IdType.RANDOM;
    }
}
