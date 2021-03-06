package com.github.cheukbinli.original.common.util.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class ObjectPoolManager<T, V> {

	private final Map<String, AbstractObjectPool<T, V>> POOL = new ConcurrentSkipListMap<String, AbstractObjectPool<T, V>>();

	/***
	 * 重试次数
	 */
	private int tryAgain = 30;
	/***
	 * 重试间隔
	 */
	private int tryInterval = 50;

	/***
	 * 初始连接为空时池等待权重
	 */
	private int weight = 10;

	/***
	 * 添加池对象
	 * 
	 * @param abstractObjectPool
	 * @return 返回旧有对像，不存在旧有对象返回null
	 * @throws Throwable
	 */
	public AbstractObjectPool<T, V> addPool(AbstractObjectPool<T, V> abstractObjectPool) throws Throwable {
		synchronized (this) {
			return POOL.put(abstractObjectPool.getPoolName(), abstractObjectPool);
		}
	}

	public String[] getPoolNames() {
		return POOL.keySet().toArray(new String[0]);
	}

	public List<AbstractObjectPool<T, V>> getPoolValues() {
		return new ArrayList<AbstractObjectPool<T, V>>(POOL.values());
	}

	public AbstractObjectPool<T, V> remove(String poolName) {
		return POOL.remove(poolName);
	}

	public T borrowObject(String poolName) throws Throwable {
		int tryCount = getTryAgain();
		T result;
		while (true) {
			AbstractObjectPool<T, V> pool = POOL.get(poolName);
			if (null == pool) {
				Thread.sleep(getTryInterval() * this.weight);
			} else if (null != (result = pool.borrowObject())) {
				return result;
			}
			if (tryCount-- < 0)
				throw new NullPointerException("can't found objectPool is [" + poolName + "],you can check it.");
			Thread.sleep(getTryInterval());
		}
	}

	public void returnObject(String poolName, T t) throws Throwable {
		AbstractObjectPool<T, V> result = POOL.get(poolName);
		if (null == result) {
			return;
			// throw new NullPointerException("can't found objectPool is [" + poolName + "],fail by return object,you can check it.");
		}
		result.returnObject(t);
	}

	public AbstractObjectPool<T, V> getPool(String poolName) throws Throwable {
		return POOL.get(poolName);
	}

	public int getTryAgain() {
		return tryAgain;
	}

	public ObjectPoolManager<T, V> setTryAgain(int tryAgain) {
		this.tryAgain = tryAgain;
		return this;
	}

	public int getTryInterval() {
		return tryInterval;
	}

	public ObjectPoolManager<T, V> setTryInterval(int tryInterval) {
		this.tryInterval = tryInterval;
		return this;
	}

}
