package com.github.cheukbinli.original.common.cache;

public interface CacheSerialize {

	byte[] encode(Object o) throws CacheException;

	Object decode(byte[] o) throws CacheException;

	<T> T decodeT(byte[] o) throws CacheException;

	<T> T decodeT(byte[] o, Class<T> t) throws CacheException;

}
