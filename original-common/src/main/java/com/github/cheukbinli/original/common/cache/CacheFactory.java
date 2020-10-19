package com.github.cheukbinli.original.common.cache;

import java.util.Collection;
import java.util.Set;

/***
 * 缓存接口
 *
 * @author ben
 *
 * @param <K>
 * @param <V>
 */
public interface CacheFactory<K, V> {

    /**
     * 取出
     */
    V take(K key) throws com.github.cheukbinli.original.common.cache.CacheException;

    V put(K key, V value) throws com.github.cheukbinli.original.common.cache.CacheException;

    V remove(K key) throws com.github.cheukbinli.original.common.cache.CacheException;

    void scriptClear() throws com.github.cheukbinli.original.common.cache.CacheException;

    int size() throws com.github.cheukbinli.original.common.cache.CacheException;

    Set<K> keys() throws com.github.cheukbinli.original.common.cache.CacheException;

    Collection<V> values() throws com.github.cheukbinli.original.common.cache.CacheException;

    default void dectory() {
    }

    default void init() {
    }
}
