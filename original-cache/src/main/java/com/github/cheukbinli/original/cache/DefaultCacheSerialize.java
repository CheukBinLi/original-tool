package com.github.cheukbinli.original.cache;

import com.github.cheukbinli.original.common.cache.CacheException;
import com.github.cheukbinli.original.common.cache.CacheSerialize;

import java.io.*;

public class DefaultCacheSerialize implements CacheSerialize {

	public byte[] encode(Object o) throws CacheException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(baos);
			out.writeObject(o);
			return baos.toByteArray();
		} catch (Throwable e) {
			throw new CacheException(e);
		} finally {
			try {
				if (null != out)
					out.close();
			} catch (IOException e) {
			}
		}
	}

	public Object decode(byte[] o) throws CacheException {
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new ByteArrayInputStream(o));
			return in.readObject();
		} catch (Throwable e) {
			throw new CacheException(e);
		} finally {
			try {
				if (null != in)
					in.close();
			} catch (IOException e) {
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T decodeT(byte[] o) throws CacheException {
		return (T) decodeT(o, null);
	}

	@SuppressWarnings("unchecked")
	public <T> T decodeT(byte[] o, Class<T> t) throws CacheException {
		return (T) decode(o);
	}

}
