package com.github.cheukbinli.original.common.rmi.net;

import com.github.cheukbinli.original.common.cache.CacheException;
import com.github.cheukbinli.original.common.cache.CacheSerialize;
import com.github.cheukbinli.original.common.rmi.RmiException;

import java.io.Serializable;

/***
 * 
 * @Title: original-rmi
 * @Description: RMI编码器
 * @Company:
 * @Email: 20796698@qq.com
 * @author cheuk.bin.li
 * @date 2017年4月28日 下午12:04:04
 *
 */
public abstract class AbstractRmiEncoder<INPUT, RETURN> {

	private CacheSerialize cacheSerialize;

	public abstract RETURN combination(final INPUT input, byte[] data);

	public final RETURN encoder(final INPUT input, Serializable serializable) throws RmiException {
		try {
			return combination(input, cacheSerialize.encode(serializable));
		} catch (CacheException e) {
			throw new RmiException(e);
		}
	}

	public AbstractRmiEncoder<INPUT, RETURN> setCacheSerialize(CacheSerialize cacheSerialize) {
		this.cacheSerialize = cacheSerialize;
		return this;
	}

}
