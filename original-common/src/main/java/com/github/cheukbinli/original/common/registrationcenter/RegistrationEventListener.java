package com.github.cheukbinli.original.common.registrationcenter;

/***
 * 
 * @author ben
 *
 */
public interface RegistrationEventListener<T> {

	void nodeChanged(T params, Object... other) throws Exception;

}
