package com.github.cheukbinli.original.common.rmi.net;

import com.github.cheukbinli.original.common.rmi.RmiContant;
import com.github.cheukbinli.original.common.rmi.RmiException;

/***
 * 
 * @Title: original-rmi
 * @Description: 消息处理工厂
 * @Company:
 * @Email: 20796698@qq.com
 * @author cheuk.bin.li
 * @date 2017年4月28日 下午3:35:22
 *
 */
public interface MessageHandleFactory<Input, Value, Args> extends RmiContant {

	/***
	 * 
	 * @param in
	 * @param v
	 * @param serviceType
	 * @throws Exception
	 */
	void messageHandle(final Input in, final Value v, final int serviceType) throws RmiException;

	/***
	 * 
	 * @param serviceType
	 *            服务类型:request,response,ping等等
	 * @param messageHandle
	 */
	void registrationMessageHandle(int serviceType, MessageHandle<Input, Value> messageHandle);

	/***
	 * 服务是否存在
	 * 
	 * @param serviceType
	 *            服务类型:request,response,ping等等
	 * @return
	 */
	boolean serviceTypeContains(int serviceType);

	/***
	 * 运行消息处理工厂
	 * 
	 * @param args
	 */
	void start(Args args);
}
