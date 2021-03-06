package com.github.cheukbinli.original.cache;

import com.github.cheukbinli.original.common.cache.CacheException;
import com.github.cheukbinli.original.common.cache.CacheSerialize;
import org.nustaq.serialization.FSTConfiguration;

@SuppressWarnings("unchecked")
public class FstCacheSerialize implements CacheSerialize {

	static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

	public byte[] encode(Object o) throws CacheException {
		return conf.asByteArray(o);
	}

	public Object decode(byte[] o) throws CacheException {
		return conf.asObject(o);
	}

	public <T> T decodeT(byte[] o) throws CacheException {
		return decodeT(o, null);
	}

	public <T> T decodeT(byte[] o, Class<T> t) throws CacheException {
		Object result = decode(o);
		return null == result ? null : (T) result;
	}

}
