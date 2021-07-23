package com.github.cheukbinli.original.common.util.xml;

import com.github.cheukbinli.original.common.util.conver.ObjectFill;
import com.github.cheukbinli.original.common.util.conver.StringUtil;
import com.github.cheukbinli.original.common.util.reflection.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

/***
 * xml数据填充
 *
 * @author Ben-Book
 *
 */
public class XmlReaderAll extends DefaultHandler {

    private final static Logger LOG = LoggerFactory.getLogger(XmlReaderAll.class);

    private ReflectionCache reflectionCache = ReflectionCache.newInstance();

    private ReflectionUtil reflectionUtil = ReflectionUtil.instance();

    private final static Set<String> IGNORE_STRING = new HashSet<>(Arrays.asList(
            "\n", "\r", "\r\n", "\n\r"
    ));

    private Field clazz = null;

    {
        try {
            clazz = Field.class.getDeclaredField("clazz");
            clazz.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private final ObjectFill objectFill = new ObjectFill();

    private static volatile XmlReaderAll INSTANCE;

    public static XmlReaderAll newInstance() {
        if (null == INSTANCE) {
            synchronized (XmlReaderAll.class) {
                if (null == INSTANCE) {
                    INSTANCE = new XmlReaderAll();
                }
            }
        }
        return INSTANCE;
    }

    SAXParserFactory factory = SAXParserFactory.newInstance();

    SAXParser parser = null;

    {
        try {
            parser = factory.newSAXParser();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static <T> T paddingModel(byte[] bytes, Class<T> obj) throws NoSuchFieldException, SecurityException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        return new XmlReaderAll().padding(bytes, obj);
    }

    public static <T> T paddingModel(byte[] bytes, T obj) throws NoSuchFieldException, SecurityException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        return new XmlReaderAll().padding(bytes, obj);
    }

    public synchronized <T> T padding(byte[] bytes, Class<T> obj) throws ParserConfigurationException, SAXException, IOException, NoSuchFieldException, SecurityException, InstantiationException, IllegalAccessException {
        return padding(bytes, obj.newInstance());
    }

    public synchronized <T> T padding(byte[] bytes, T obj) throws ParserConfigurationException, SAXException, IOException, NoSuchFieldException, SecurityException, InstantiationException, IllegalAccessException {
        link.clear();
        T result = obj;
        link.addFirst(new Node(null, null, result, result, null));
        XmlReaderAll handler = this;
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        InputSource is = new InputSource(in);
        is.setEncoding("utf-8");
        parser.parse(is, handler);

        T t = (T) result;
        // 译放obj
        result = null;
        return t;
    }

    static class Node {
        private String tagName;

        private Field field;

        private Object parent;

        private Object obj;

        /***
         * 泛型对象
         */
        private Class genericity;

        public String getTagName() {
            return tagName;
        }

        public Node setTagName(String tagName) {
            this.tagName = tagName;
            return this;
        }

        public Object getParent() {
            return parent;
        }

        public void setParent(Object parent) {
            this.parent = parent;
        }

        public Object getObj() {
            return obj;
        }

        public Node setObj(Object obj) {
            this.obj = obj;
            return this;
        }

        public Field getField() {
            return field;
        }

        public Node setField(Field field) {
            this.field = field;
            return this;
        }

        public Class getGenericity() {
            return genericity;
        }

        public void setGenericity(Class genericity) {
            this.genericity = genericity;
        }

        public Node(String tagName, Field field, Object parent, Object obj, Class genericity) {
            super();
            this.tagName = tagName;
            this.field = field;
            this.parent = parent;
            this.obj = obj;
            this.genericity = genericity;
        }
    }

    LinkedList<Node> link = new LinkedList<XmlReaderAll.Node>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            Node node = link.getLast();
            Object x = link.getLast().getObj();
            ClassInfo classInfo = ClassInfo.getClassInfo(x.getClass());
            Object parent = x;
            Object value = null;
            Class genericity = null;
            if (null == classInfo.getFields()) {
                classInfo.setFields(ReflectionUtil.instance().scanClassFieldInfo4Map(classInfo.getClazz(), true, true, true));
            }
            FieldInfo fieldInfo = classInfo.getFields().get(qName);
            if (null == fieldInfo) {
                fieldInfo = classInfo.getFields().get(StringUtil.toLowerUnderscoreCaseCamel(qName));
            }
            if (null == fieldInfo) {
                link.add(null);
                return;
            }
            ClassInfo fieldClassInfo = ClassInfo.getClassInfo(fieldInfo.getField().getType());
            if (fieldClassInfo.isMapOrSetOrCollection()) {
                Object collection = fieldInfo.getField().get(x);
                if (null != node.getGenericity()) {
                    value = node.getGenericity().newInstance();
                } else {
                    List<Field> subField = reflectionUtil.searchCollection(fieldInfo.getField(), true);
                    if (!subField.isEmpty()) {
                        Class subClass = (Class) clazz.get(subField.get(0));
                        value = subClass.newInstance();
                    }
                }

                if (null == collection) {
                    if (fieldClassInfo.isMap()) {
                        throw new RuntimeException("not support Map.");
                    } else if (fieldClassInfo.isSet()) {
                        collection = new LinkedHashMap<>();
                    } else if (fieldClassInfo.isCollection()) {
                        collection = new ArrayList<>();
                    }
                    fieldInfo.getField().set(x, collection);
                    link.addLast(new Node(qName, fieldInfo.getField(), collection, value, null));
                    return;
                } else {
                    if (fieldClassInfo.isMap()) {
                        throw new RuntimeException("not support Map.");
                    } else {
                        ((Collection) collection).add(value);
                    }
                }
            } else if (fieldClassInfo.isBasicOrArrays()) {
//                fieldInfo.getField().set(x, fieldInfo.getField().getType().newInstance());
            } else {
                fieldInfo.getField().set(x, value = fieldInfo.getField().getType().newInstance());
            }
            link.addLast(new Node(qName, fieldInfo.getField(), parent, value, genericity));
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        Node node = link.getLast();
        if (null == node || qName.equals(node.getTagName())) {
            link.removeLast();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        Node node;
        if (link.size() <= 1 || null == (node = link.getLast())) {
            return;
        }
        try {
            String value = new String(ch, start, length);
            if (StringUtil.isBlank(value) || IGNORE_STRING.contains(value))
                return;

            node.getField().set(null == node.getObj() ? node.getParent() : node.getObj(), Type.getValue(node.getField().getType(), new String(ch, start, length), null));
        } catch (IllegalAccessException e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
