package com.github.cheukbinli.original.cache;

import com.github.cheukbinli.original.cache.redis.JedisClusterFactory;
import com.github.cheukbinli.original.common.cache.CacheException;
import com.github.cheukbinli.original.common.cache.RedisCacheFactory;

import java.util.Collection;
import java.util.Set;

public class JedisClusterCacheFactory extends JedisClusterFactory implements RedisCacheFactory<Object, Object> {

	public Object take(Object key) throws CacheException {
		return getOO(key);
	}

	
	
	public Object put(Object key, Object value) throws CacheException {
		return getAndSetOO(key, value);
	}

	public Object remove(Object key) throws CacheException {
		Object o = getOO(key);
		deleteOO(key);
		return o;
	}

	public void clear() throws CacheException {
		System.err.println("REDIS NOT SUPPORT CLEAR");
	}

	public int size() throws CacheException {
		return -1;
	}

	public Set<Object> keys() throws CacheException {
		System.err.println("REDIS NOT SUPPORT keys");
		return null;
	}

	public Collection<Object> values() throws CacheException {
		System.err.println("REDIS NOT SUPPORT Collection");
		return null;
	}

}
