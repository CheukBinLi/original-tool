package com.github.cheukbinli.original.common.cache.redis;

import java.util.List;
import java.util.Map;

public interface RedisCommand {

	public void delete(String key) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public Object evalSha(String sha, int keys, String... params) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public Object evalSha(String sha, List<String> keys, List<String> argv) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public Object evalSha(String sha, String key) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public boolean scriptExists(String sha, String key) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public void scriptFlush() throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public void scriptKill() throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public String scriptLoad(String key, String script) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public boolean exists(String key) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public boolean expireAt(String key, long unixTime) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public boolean expire(String key, int seconds) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public boolean set(String key, String value) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public boolean set(String key, String value, int expireSeconds) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	/***
	 * 运算操作: +value
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws RedisExcecption
	 */
	public boolean incr(String key, Integer value) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	/***
	 * 运算操作: +value
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @throws RedisExcecption
	 */
	public boolean incrByMap(String key, String mapKey, Integer value) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public String getAndSet(String key, String value) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public String get(String key) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public List<String> get(String... key) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	/**
	 * 设置数据有效时间 /***
	 * 
	 * Set<Map<mapKey,value>>
	 * 
	 * @param key
	 * @param mapKey
	 * @param value
	 * @return
	 */
	public boolean setMap(String key, String mapKey, String value) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public boolean setMap(String key, Map<String, String> map) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public Map<String, String> getMap(String key) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public List<String> getMapList(String key, String... subKyes) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public boolean mapKeyExists(String key, String mapKey) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public String getMapValue(String key, String mapKey) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public boolean mapRemove(String key, String... mapKey) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public List<String> getListByString(String key, int start, int end) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	/** List */

	public boolean addListFirst(String key, String... value) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public boolean addListLast(String key, String... value) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	/***
	 * 删除列表，指定行除外
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @return
	 */
	public boolean removeListWithoutFor(String key, int start, int end) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	/***
	 * 修改指定行
	 * 
	 * @param key
	 * @param index
	 * @param value
	 * @return
	 */
	public boolean setListIndex(String key, int index, String value) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public String getListIndex(String key, int index) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	/***
	 * 获取列表长度
	 * 
	 * @param key
	 * @return
	 */
	public long listLen(String key) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public String popListFirst(String key) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public long removeListValue(String key, String value, int count) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public Object eval(String script, int keysCount, String... params) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

	public Object eval(String script, List<String> keys, List<String> params) throws com.github.cheukbinli.original.common.cache.redis.RedisExcecption;

}
