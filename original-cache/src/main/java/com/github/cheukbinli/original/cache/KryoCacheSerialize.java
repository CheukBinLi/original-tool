package com.github.cheukbinli.original.cache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.cheukbinli.original.common.cache.CacheException;
import com.github.cheukbinli.original.common.cache.CacheSerialize;

import java.io.ByteArrayOutputStream;

@SuppressWarnings("unchecked")
public class KryoCacheSerialize implements CacheSerialize {

	private static final Kryo kryo = new KryoEx();
	static {
		// kryo.setRegistrationRequired(false);
		// kryo.register(Exception.class, new JavaSerializer());
	}

	public byte[] encode(Object o) throws CacheException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Output output = new Output(out);
		kryo.writeObject(output, o);
		output.flush();
		output.close();
		return out.toByteArray();
	}

	public Object decode(byte[] o) throws CacheException {
		return kryo.readClassAndObject(new Input(o));
	}

	public <T> T decodeT(byte[] o) throws CacheException {
		return decodeT(o, null);
	}

	public <T> T decodeT(byte[] o, Class<T> t) throws CacheException {
		Object result = decode(o);
		return null == result ? null : (T) result;
	}

}
