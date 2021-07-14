package com.github.cheukbinli.original.common.util.conver;

import com.github.cheukbinli.original.common.cache.redis.Script;
import com.github.cheukbinli.original.common.util.reflection.ClassInfo;
import com.github.cheukbinli.original.common.util.reflection.FieldInfo;
import com.github.cheukbinli.original.common.util.reflection.ReflectionUtil;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;

@SuppressWarnings({"unchecked", "rawtypes"})
public class CollectionUtil {

    private static final CollectionUtil newInstance = new CollectionUtil();

    public static final Set EMPTY_SET = new HashSet<>(1);

    public static final Map EMPTY_MAP = new HashMap<>(1);

    @Deprecated
    public static final CollectionUtil newInstance() {
        return newInstance;
    }

    public static <K, V> Map<K, V> removeNullValue(final Map<K, V> collection) {
        Iterator<Entry<K, V>> it = collection.entrySet().iterator();
        Entry<K, V> en;
        while (it.hasNext()) {
            en = it.next();
            if (null == en.getValue())
                it.remove();
        }
        return collection;
    }

    public static <K, V> Map<K, V> toMap(Object... params) {
        if (null == params || 0 != (params.length % 2))
            return null;
        Map<String, Object> map = new WeakHashMap<String, Object>(params.length * 2);
        for (int i = 0, len = params.length; i < len; i++) {
            map.put((String) params[i++], params[i]);
        }
        return (Map<K, V>) map;
    }

    public static <K, V> Map<K, V> toMap(boolean isWeak, Object... params) {
        if (null == params || 0 != (params.length % 2))
            return null;
        Map<K, Object> result = isWeak ? new WeakHashMap<K, Object>(params.length * 2) : new HashMap<K, Object>(params.length * 2);
        for (int i = 0, len = params.length; i < len; i++) {
            result.put((K) params[i++], params[i]);
        }
        return (Map<K, V>) result;
    }

    @SafeVarargs
    public static <K, V> Map<K, V> collage(Map<K, V>... maps) {
        Map<K, V> result = null;
        for (Map<K, V> map : maps) {
            if (null == map || map.size() < 1) {
                continue;
            }
            if (null == result) {
                result = map;
                continue;
            }
            result.putAll(map);
        }
        return result;
    }

    @SafeVarargs
    public static <T> Collection<T> collageCollection(Collection<T>... arrays) {
        Collection result = null;
        for (Collection item : arrays) {
            if (null == item || item.size() < 1) {
                continue;
            }
            if (null == result) {
                result = item;
            }
            result.addAll(item);
        }
        return result;
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Collection<?> collection) {
        return null == collection || collection.size() == 0;
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return null == map || map.size() == 0;
    }

    public static boolean isNotEmpty(Object... o) {
        return !isEmpty(o);
    }

    public static boolean isEmpty(Object... o) {
        return null == o || o.length < 1;
    }

    public static MapBuilder mapBuilder() {
        return new MapBuilder();
    }

    public static class MapBuilder {

        private Map<Object, Object> data;

        public MapBuilder append(Object k, Object v) {
            if (null == this.data)
                data = new HashMap<Object, Object>();
            data.put(k, v);
            return this;
        }

        public <K, V> Map<K, V> build() {
            return (Map<K, V>) data;
        }

    }

    /***
     * 从对象里取值转换为map对象
     * @param obj
     * @param keyUseAliasName
     * @param keyNames
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static Map createMapByObject(Object obj, boolean keyUseAliasName, String... keyNames) {
        try {
            if (null == obj || isEmpty(keyNames))
                return EMPTY_MAP;
            ClassInfo classInfo = ClassInfo.getClassInfo(obj.getClass());
            Map<String, FieldInfo> fields = classInfo.getFields();
            if (null == fields) {
                synchronized (classInfo) {
                    fields = classInfo.getFields();
                    if (null == fields) {
                        classInfo.setFields(fields = ReflectionUtil.instance().scanClassFieldInfo4Map(classInfo.getClazz(), true, true, true));
                    }
                }
            }
            Map result = new HashMap(keyNames.length * 2);
            FieldInfo fieldInfo = null;
            String key;
            for (String item : keyNames) {
                if (null == (fieldInfo = fields.get(item)))
                    continue;
                result.put(fieldInfo.getAliasOrFieldName(keyUseAliasName), fieldInfo.getField().get(obj));
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return EMPTY_MAP;
    }

    public static SetBuilder setBuilder(boolean isConcurrent) {
        return new SetBuilder(isConcurrent);
    }

    public static class SetBuilder {

        private Set<Object> data;

        public SetBuilder(boolean isConcurrent) {
            data = isConcurrent ? new CopyOnWriteArraySet<Object>() : new HashSet<Object>();
        }

        public SetBuilder append(Object value) {
            data.add(value);
            return this;
        }

        public <V> Set<V> build() {
            return (Set<V>) data;
        }

    }

    //	public static <T> Link<T> LinkBuilder() {
//		return new Link<T>();
//	}
//
//	public static class Link<T> {
//
//		public static class Node<T> implements Serializable {
//			private static final long serialVersionUID = -2845390390604021521L;
//			private Node<T> previous;
//			private T current;
//			private Node<T> next;
//
//			public Node() {
//				super();
//			}
//
//			public Node(T current) {
//				super();
//				this.current = current;
//			}
//
//			public Node(Node<T> previous, T current, Node<T> next) {
//				super();
//				this.previous = previous;
//				this.current = current;
//				this.next = next;
//			}
//
//			public Node<T> setPrevious(Node<T> previous) {
//				this.previous = previous;
//				return this;
//			}
//
//			public Node<T> setCurrent(T current) {
//				this.current = current;
//				return this;
//			}
//
//			public Node<T> setNext(Node<T> next) {
//				this.next = next;
//				return this;
//			}
//
//			public T getCurrent() {
//				return current;
//			}
//
//		}
//
//		private int size = 0;
//		private int index = 0;
//		private Node<T> node;
//		private Node<T> first;
//		private Node<T> end;
//
//		public void rollback() {
//			node = this.first;
//			this.index = 0;
//		}
//
//		public T first() {
//			return null == first ? null : first.current;
//		}
//		public Node<T> firstNode() {
//			return null == first ? null : first;
//		}
//
//		public Node<T> lastNode() {
//			return null == first ? null : first;
//		}
//
//		public T last() {
//			return null == end ? null : end.current;
//		}
//
//		public Link<T> append(T data_template) {
//			if (size < 2) {
//				this.first = new Node<T>(null == this.first ? data_template : this.first.current);
//				this.end = new Node<T>(this.first, data_template, null);
//				this.first.setNext(this.end);
//
//			} else {
//				Node<T> tempEnd = new Node<T>(this.end, data_template, null);
//				this.end.setNext(tempEnd);
//				this.end = tempEnd;
//			}
//			this.size++;
//			return this;
//		}
//
//		public T previous() {
//			if (size == index)
//				if (null == node) {
//					return null;
//				} else {
//					index--;
//					return node.current;
//				}
//			if (index == 0) {
//				this.node = this.first;
//			}
//			Node<T> tempNode = node.previous;
//			if (null == tempNode) {
//				return null;
//			}
//			index--;
//			node = tempNode;
//			return tempNode.current;
//		}
//
//		public T next() {
//			if (index == 0) {
//				node = this.first;
//				if (null == node) {
//					return null;
//				}
//				index++;
//				return node.current;
//			}
//			if (size == index)
//				return null;
//			Node<T> tempNode = node.next;
//			if (null == tempNode) {
//				return null;
//			}
//			index++;
//			node = tempNode;
//			return tempNode.current;
//		}
//
//		public Object current() {
//			if (null == node)
//				return null;
//			return node.current;
//		}
//
//		public int index() {
//			return this.index;
//		}
//
//		public int size() {
//			return this.size;
//		}
//
//	}
//
    public static void main(String[] args) {
//		Map<String, Object> a = toMap(true, new Object[]{1, "1", 2, "2"});
//		Map<String, Object> b = toMap("1", 1, "2", 2);
//		System.out.println(a);
//		System.out.println(b);
//
//		Link<Integer> link = new Link<Integer>();
//		link.append(1).append(2).append(3);
//		System.err.println(link.size);
//		for (int i = 0, len = link.size + 1; i < len; i++) {
//			System.err.println(link.next());
//		}
//		for (int i = 0, len = link.size + 1; i < len; i++) {
//			System.err.println(link.previous());
//		}

        Script aaa = new Script();
        aaa.setName("993");
        aaa.setSlotName("slotName");
        aaa.setScript("hello world");
        Map aaaMap = createMapByObject(aaa, true, "name", "script");
        System.out.println(aaaMap);
    }

}
