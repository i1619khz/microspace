package io.microspace.sqlstream;

/**
 * @author i1619kHz
 */
public interface SQLAction<T> {
    <K, R> T distinct(TypeFunction<K, R>... typeFunctions);

    <K, R> T select(TypeFunction<K, R>... typeFunctions);

    <K, R> T count(TypeFunction<K, R> typeFunctions);

    T count();

    T count(String fieldNames);

    T distinct(String... fieldNames);

    T select(String... fieldNames);

    T select();
}
