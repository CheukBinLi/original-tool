package com.github.cheukbinli.original.common.util.conver;

import com.github.cheukbinli.original.common.util.reflection.ReflectionCache;
import com.github.cheukbinli.original.common.util.reflection.ReflectionUtil;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

@Deprecated
public class ObjectToJson {

	private ReflectionCache reflectionCache = ReflectionCache.newInstance();

	private ReflectionUtil reflectionUtil = ReflectionUtil.instance();

	private static ObjectToJson INSTANCE;

	private volatile SimpleDateFormat defaultFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private volatile JsonMapperPropertyInclusion defaultPropertyInclusion = JsonMapperPropertyInclusion.ALWAYS;

	public SimpleDateFormat getDefaultFormat() {
		return defaultFormat;
	}

	public ObjectToJson setDefaultFormat(String format) {
		this.defaultFormat = new SimpleDateFormat(format);
		return this;
	}

	public JsonMapperPropertyInclusion getDefaultPropertyInclusion() {
		return defaultPropertyInclusion;
	}

	public ObjectToJson setDefaultPropertyInclusion(JsonMapperPropertyInclusion defaultPropertyInclusion) {
		this.defaultPropertyInclusion = defaultPropertyInclusion;
		return this;
	}

	public void setDefaultFormat(SimpleDateFormat defaultFormat) {
		this.defaultFormat = defaultFormat;
	}

	protected ObjectToJson() {
	}

	public static ObjectToJson newInstance() {
		if (null == INSTANCE) {
			synchronized (ObjectToJson.class) {
				if (null == INSTANCE) {
					INSTANCE = new ObjectToJson();
				}
			}
		}
		return INSTANCE;
	}

	public String writeToString(Object o, FilterProvider filterProvider) throws Exception {
		StringBuilder result = new StringBuilder();
		recursion(o, null, filterProvider, result);
		return result.toString();
	}

	private void recursion(Object o, final List<Field> fieldData, FilterProvider filterProvider, final StringBuilder result) throws Exception {
		List<Field> fields = null == fieldData ? reflectionCache.getFields4List(o.getClass(), true, false) : fieldData;
		String tagName;
		Object tempValue;
		result.append("{");
		Filter currentClazz = null == filterProvider ? null : filterProvider.getFilterByClass(o.getClass());
		Filter filterAll = null == filterProvider ? null : filterProvider.getFilterByClass(null);
		for (Field field : fields) {
			tagName = field.getName();
			if (FilterProvider.isIgnore(tagName, currentClazz, filterAll)) {
				continue;
			}
			tempValue = field.get(o);
			if (null != tempValue && null != field && !field.getType().isPrimitive() && !reflectionUtil.isWrapperClass(tempValue.getClass()) && !Date.class.equals(field.getType())) {
				if (reflectionUtil.isMap(tempValue) || reflectionUtil.isCollection(tempValue)) {
					recursionSub(field.getName(), tempValue, result, null, filterProvider);
				} else {
					result.append("\"").append(tagName).append("\":");
					recursion(tempValue, null, filterProvider, result);
				}
			} else if (null != tempValue) {
				result.append("\"").append(tagName).append("\"").append(":\"").append(Date.class.equals(field.getType()) ? defaultFormat.format(tempValue) : tempValue.toString()).append("\"");
			} else {
				switch (this.defaultPropertyInclusion) {
				case ALWAYS:
					result.append("\"").append(tagName).append("\"").append(":null");
					break;
				case NON_NULL:
					break;
				case NON_EMPTY:
					result.append("\"").append(tagName).append("\"").append(":\"\"");
					break;
				default:
					break;
				}
			}
			result.append(",");
		}
		if (result.length() > 1)
			result.setLength(result.length() - 1);
		result.append("}");
	}

	private void recursionSub(final String name, final Object value, final StringBuilder result, final List<Field> fieldData, FilterProvider filterProvider) throws Exception {
		boolean isMap;
		boolean isCollection = false;
		Map<?, ?> map = null;
		List<Field> subField;
		Object tempSubValue;
		Object tempSubKey;
		Collection<?> collection = null;
		Iterator<?> it;
		Entry<?, ?> en;
		boolean isDate = false;
		String end = "]";
		Filter currentClazz = null == filterProvider ? null : filterProvider.getFilterByClass(value.getClass());
		Filter filterAll = null == filterProvider ? null : filterProvider.getFilterByClass(null);
		result.append("\"").append(name).append("\"").append(":");
		if ((isMap = reflectionUtil.isMap(value)) || (isCollection = reflectionUtil.isCollection(value))) {
			if (isMap) {
				map = (Map<?, ?>) value;
				if (map.isEmpty()) {
					result.setLength(result.length() - 1);
					result.append(":null");
					return;
				}
				result.append("{");
				end = "}";
			} else if (isCollection) {
				collection = (Collection<?>) value;
				if (collection.isEmpty()) {
					result.setLength(result.length() - 1);
					result.append(":null");
					return;
				}
				result.append("[");
			}

			it = isMap ? map.entrySet().iterator() : collection.iterator();
			subField = null;
			while (it.hasNext()) {
				tempSubValue = it.next();
				if (isMap) {
					en = (Entry<?, ?>) tempSubValue;
					tempSubValue = en.getValue();
					tempSubKey = en.getKey();
					if (null == tempSubKey || FilterProvider.isIgnore((String) tempSubKey, currentClazz, filterAll)) {
						result.append(",");
						continue;
					}
					if (null == tempSubValue) {
						switch (this.defaultPropertyInclusion) {
						case ALWAYS:
							result.append("\"").append(tempSubKey).append("\"").append(":null");
							break;
						case NON_NULL:
							break;
						case NON_EMPTY:
							result.append("\"").append(tempSubKey).append("\"").append(":\"\"");
							break;
						default:
							break;
						}
						result.append(",");
					} else if ((isDate = Date.class.equals(tempSubValue.getClass())) || tempSubValue.getClass().isPrimitive() || reflectionUtil.isWrapperClass(tempSubValue.getClass())) {
						result.append("\"").append(tempSubKey.toString()).append("\":\"").append(isDate ? defaultFormat.format(tempSubValue) : tempSubValue).append("\",");
					} else {
						recursionSub(tempSubKey.toString(), tempSubValue, result, null, filterProvider);
						result.append(",");
					}
				} else {
					if (null == subField) {
						subField = null == fieldData ? reflectionCache.getFields4List(tempSubValue.getClass(), true, false) : fieldData;
					}
					if ((isDate = Date.class == tempSubValue.getClass()) || tempSubValue.getClass().isPrimitive() || reflectionUtil.isWrapperClass(tempSubValue.getClass())) {
						result.append("\"").append(isDate ? defaultFormat.format(tempSubValue) : tempSubValue).append("\",");
					} else {
						recursion(tempSubValue, subField, filterProvider, result);
						result.append(",");
					}
				}
			}
			result.setLength(result.length() - 1);

			result.append(end);
		} else if (null != value) {
			result.append("\"").append(name).append("\"").append(":\"").append(value).append("\"");
		}
	}

	public static void main(String[] args) throws Throwable {
		//		Map<String, String> mmx = new HashMap<>();
		//		mmx.put("A", "A");
		//		mmx.put("B", "B");
		//		mmx.put("C", "C");
		//		mmx.put("D", "D");
		//		Result<Map<String, String>> result = new Result<Map<String, String>>("20", "哇哈哈", mmx);
		//
		//		Map<String, List<Object>> a = new HashMap<>();
		//		List<Object> list = new ArrayList<>();
		//		list.add("z");
		//		list.add("x");
		//		list.add("c");
		//		list.add("v");
		//		list.add(new Date());
		//		a.put("abc", list);
		//		System.out.println(new ObjectToJson().writeToString(result, new FilterProvider(new Filter(Result.class, "msg"), new Filter(null, "abc"))));
		//
		//		System.out.println(new ObjectToJson().writeToString(new Result<Object>("11", "xxx", a), new FilterProvider(new Filter(Result.class, "msg"), new Filter(null, "abc1"))));
		//
		//		System.out.println(new ObjectToJson().writeToString(new Result<>("111", "xxxxxx", new TagEntity(1L, 2L, "31", null, 1)), new FilterProvider(new Filter(null, "msg"), new Filter(null, "abc1"))));

	}

}
