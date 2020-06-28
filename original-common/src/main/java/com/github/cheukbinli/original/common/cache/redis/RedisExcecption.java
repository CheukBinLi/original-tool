package com.github.cheukbinli.original.common.cache.redis;

import com.github.cheukbinli.original.common.cache.CacheException;

public class RedisExcecption extends CacheException {

	private static final long serialVersionUID = 1L;

	public RedisExcecption(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public RedisExcecption(String message, Throwable cause) {
		super(message, cause);
	}

	public RedisExcecption(String message) {
		super(message);
	}

	public RedisExcecption(Throwable cause) {
		super(cause);
	}

	@Override
	public void printStackTrace() {
		super.printStackTrace();
	}

}
