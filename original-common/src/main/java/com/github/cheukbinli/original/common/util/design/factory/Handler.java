package com.github.cheukbinli.original.common.util.design.factory;

public interface Handler<T> {

	String getType();

	default void init() {
	}

	default boolean isSupport(T t) {
		return false;
	}

}
