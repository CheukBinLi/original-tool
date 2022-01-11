package com.github.cheukbinli.original.rmi;

import com.github.cheukbinli.original.common.util.conver.StringUtil;
import javassist.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.StringMemberValue;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class ClassRebuild {

    private final ClassPool pool = ClassPool.getDefault();
    private volatile CtClass VOID;

    {
        pool.insertClassPath(new ClassClassPath(this.getClass()));
        try {
            VOID = pool.get(void.class.getName());
        } catch (NotFoundException e) {
            VOID = null;
        }
    }

    private final String suffixName = "$proxyClass";

    public String getSuffixName(String nick) {
        if (null != nick)
            return "_" + nick + suffixName;
        return suffixName;
    }

    public Class<?> build(final Class<?> clazz, ModifyMethod modifyMethod, List<FieldModel> fieldmodels, String suffix, String paramPrefix) throws Throwable {
        return build(clazz, modifyMethod, fieldmodels, null, suffix, paramPrefix, null);
    }
    public Class<?> build(final Class<?> clazz, ModifyMethod modifyMethod, List<FieldModel> fieldmodels, String suffix) throws Throwable {
        return build(clazz, modifyMethod, fieldmodels, null, suffix, null);
    }

    public Class<?> build(final Class<?> clazz, ModifyMethod modifyMethod, List<FieldModel> fieldmodels, String suffix, ClassLoader classLoader) throws Throwable {
        return build(clazz, modifyMethod, fieldmodels, null, suffix, classLoader);
    }

    public Class<?> build(final Class<?> clazz, ModifyMethod modifyMethod, List<FieldModel> fieldmodels, String className, String suffix, ClassLoader classLoader) throws Throwable {
        return build(clazz, modifyMethod, fieldmodels, className, suffix, null, classLoader);
    }

    public Class<?> build(final Class<?> clazz, ModifyMethod modifyMethod, List<FieldModel> fieldmodels, String className, String suffix, String paramPrefix, ClassLoader classLoader) throws Throwable {

        boolean isInterface = clazz.isInterface();
        final String orginalClassName = (null == className ? clazz.getName() : clazz.getPackage().getName() + "." + className) + getSuffixName(suffix);

        CtClass orginalClass = pool.get(clazz.getName());
        CtClass newClass = pool.getOrNull(orginalClassName);
        if (null != newClass) {
            newClass.defrost();
            newClass.detach();
        }
        newClass = pool.makeClass(orginalClassName);
        newClass.setModifiers(Modifier.PUBLIC);
        addCtConstructor(orginalClass, newClass);

        if (isInterface)
            newClass.addInterface(orginalClass);
        else
            newClass.setSuperclass(orginalClass);

        addField(newClass, fieldmodels);

        CtMethod[] orginalClassMethods = orginalClass.getDeclaredMethods();
        String methodString;
        String superBody;
        String temp;
        String param;
        for (final CtMethod m : orginalClassMethods) {
            if (Modifier.isStatic(m.getModifiers()) || Modifier.isPrivate(m.getModifiers()) || Modifier.isFinal(m.getModifiers()) || Modifier.isNative(m.getModifiers())) {
                continue;
            }
            param = m.getParameterTypes().length > 0 ? "$$" : "";
            superBody = isInterface ? "" : (isVoid(m) ? "" : "return ") + "super." + m.getName() + "(" + param + ");";
            //			methodString = generateMethod(m, null == modifyMethod ? superBody : "try{ " + modifyMethod.appCodeToBefore(m.getName(), param), (superBody = null == (temp = modifyMethod.overrideSuperMethod(m.getName(), param)) ? superBody : temp) + " }finally{" + modifyMethod.appCodeToAfter(m.getName(), param) + "}");
            methodString = generateMethod(m, null == modifyMethod ? "" : modifyMethod.appCodeToBefore(m, m.getName(), param), (superBody = null == (temp = null == modifyMethod ? null : modifyMethod.overrideSuperMethod(m, m.getName(), param)) ? superBody : temp), null == modifyMethod ? "" : modifyMethod.appCodeToAfter(m, m.getName(), param), paramPrefix);
            newClass.addMethod(CtNewMethod.make(methodString, newClass));
        }
//		newClass.writeFile("C:/Users/BIN/Desktop");
        return null == classLoader ? newClass.toClass() : newClass.toClass(classLoader, null);
    }

    private final void addField(final CtClass clazz, List<FieldModel> fields) throws CannotCompileException, NotFoundException {

        if (null == clazz || null == fields)
            return;
        String filed;
        String temp;
        CtClass xAnnotation;
        List<String> annotationAttribute;
        AnnotationsAttribute annotationsAttribute;
        javassist.bytecode.annotation.Annotation annotation;
        CtMethod getting;
        CtMethod setting;

        for (FieldModel item : fields) {
            filed = String.format("%s %s %s%s", null == (temp = item.modifier) ? "" : temp, item.returnType.getName(), item.name, null == (temp = item.instance) ? ";" : "=" + temp + (temp.endsWith(";") ? "" : ";"));

            CtField newField = CtField.make(filed, clazz);
            newField.setModifiers(AccessFlag.PUBLIC);
            if (null != item.annotations) {
                for (Entry<Class<?>, List<String>> subItem : item.annotations.entrySet()) {
                    xAnnotation = pool.get(subItem.getKey().getCanonicalName());
                    annotationsAttribute = new AnnotationsAttribute(clazz.getClassFile().getConstPool(), AnnotationsAttribute.visibleTag);
                    annotation = new javassist.bytecode.annotation.Annotation(clazz.getClassFile().getConstPool(), xAnnotation);
                    if (null != (annotationAttribute = subItem.getValue())) {
                        for (String attribute : annotationAttribute) {
                            String[] keyValue = attribute.split("=");
                            if (keyValue.length < 2) {
                                continue;
                            }
                            annotation.addMemberValue(keyValue[0].trim(), new StringMemberValue(keyValue[1].trim(), clazz.getClassFile().getConstPool()));
                        }
                    }
                    annotationsAttribute.addAnnotation(annotation);
                    newField.getFieldInfo().addAttribute(annotationsAttribute);
                }
            }
            clazz.addField(newField);
            if (item.createGetting) {
                getting = CtMethod.make(String.format("public %s get%s(){return this.%s;}", item.returnType.getName(), StringUtil.toUpperCaseFirstOne(item.name), item.name), clazz);
                clazz.addMethod(getting);
            }
            if (item.createSetting) {
                setting = CtMethod.make(String.format("public void set%s(%s %s){this.%s=%s;}", StringUtil
                        .toUpperCaseFirstOne(item.name), item.returnType.getName(), item.name, item.name, item.name), clazz);
                clazz.addMethod(setting);
            }
        }

    }

    private String generateMethod(CtMethod m, String before, String overrideSuper, String after) throws NotFoundException {
        return generateMethod(m, before, overrideSuper, after, null);
    }

    private String generateMethod(CtMethod m, String before, String overrideSuper, String after, String prefix) throws NotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append("public ").append(m.getReturnType().getName()).append(" ").append(m.getName()).append("(");
        CtClass[] params = m.getParameterTypes();
        prefix = StringUtil.isBlank(prefix, "p_");
        if (null != params)
            for (int i = 0, len = params.length; i < len; i++) {
                sb.append(params[i].getName()).append(" ").append(prefix + i);
                if (i + 1 < len)
                    sb.append(",");
            }
        sb.append(")");
        CtClass[] throwses = m.getExceptionTypes();
        if (null != throwses && throwses.length > 0) {
            sb.append("throws ");
            for (int i = 0, len = throwses.length; i < len; i++) {
                sb.append(throwses[i].getName());
                if (i + 1 < len)
                    sb.append(",");
            }
        }

        sb.append("{");
        if (!StringUtil.isEmpty(after)) {
            sb.append("try{");
        }
        if (null != before) {
            sb.append(before);
        }
        if (null != overrideSuper) {
            sb.append(overrideSuper).append((!overrideSuper.endsWith(";") && !overrideSuper.endsWith("}")) ? ";" : "");
        }
        //after
        if (!StringUtil.isEmpty(after)) {
            sb.append("}finally{").append(after).append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    public final String convery4CodeByCtClass(String objectName, CtClass t) {
        if (null == t)
            return null;
        String typeName = t.getSimpleName();
        if (("void").equals(typeName))
            return objectName;
        if (("int").equals(typeName))
            return "((Integer)" + objectName + ").intValue()";
        else if (("boolean").equals(typeName)) {
            return "((Boolean)" + objectName + ").booleanValue()";
        } else if (("short").equals(typeName)) {
            return "((Short)" + objectName + ").shortValue()";
        } else if (("byte").equals(typeName)) {
            return "((Byte)" + objectName + ").byteValue()";
        } else if (("long").equals(typeName)) {
            return "((Long)" + objectName + ").longValue()";
        } else if (("char").equals(typeName)) {
            return "((Character)" + objectName + ").charValue()";
        } else if (("double").equals(typeName)) {
            return "((Double)" + objectName + ").doubleValue()";
        } else if (("float").equals(typeName)) {
            return "((Float)" + objectName + ").floatValue()";
        }
        return String.format("(%s)%s", t.getName(), objectName);
    }

    public final String convery4CodeByClass(String objectName, Class t) {
        String name = t.getName();
        if (name.equals(int.class.getName()) || name.equals(Integer.class.getName())) {
            return "Integer.valueOf(" + objectName + ")";
        } else if (name.equals(short.class.getName()) || name.equals(Short.class.getName())) {
            return "Short.valueOf(" + objectName + ")";
        } else if (name.equals(double.class.getName()) || name.equals(Double.class.getName())) {
            return "Double.valueOf(" + objectName + ")";
        } else if (name.equals(long.class.getName()) || name.equals(Long.class.getName())) {
            return "Long.valueOf(" + objectName + ")";
        } else if (name.equals(float.class.getName()) || name.equals(Float.class.getName())) {
            return "Float.valueOf(" + objectName + ")";
        } else if (name.equals(byte.class.getName()) || name.equals(Byte.class.getName())) {
            return "Byte.valueOf(" + objectName + ")";
        } else if (name.equals(boolean.class.getName()) || name.equals(Boolean.class.getName())) {
            return "Boolean.valueOf(" + objectName + ")";
        } else if (name.equals(char.class.getName()) || name.equals(Character.class.getName())) {
            return "Character.valueOf(" + objectName + ")";
        }
        return objectName;
    }

    public final String defaultValue(CtClass t) {
        if (null == t)
            return null;
        String typeName = t.getSimpleName();
        if (("void").equals(typeName) || ("Void").equals(typeName))
            return "";
        if (("int").equals(typeName) || ("short").equals(typeName) || ("byte").equals(typeName) || ("long").equals(typeName) || ("char").equals(typeName) || ("double").equals(typeName) || ("float").equals(typeName)) {
            return "0";
        } else if (("boolean").equals(typeName)) {
            return "true";
        }
//        else if (("Long").equals(typeName)||("Boolean").equals(typeName)||("Double").equals(typeName)||("Float").equals(typeName)) {
        return "null";
//        }
    }

    public boolean isVoid(CtMethod method) throws NotFoundException {
        if (null == VOID) {
            synchronized (this) {
                if (null == VOID) {
                    VOID = pool.get(void.class.getName());
                }
            }
//            return "void".equals(method.getReturnType().getSimpleName());
        }
        return VOID.equals(method.getReturnType());
    }

    public void addCtConstructor(CtClass source, CtClass target) {
        try {
            CtConstructor[] constructors = source.getDeclaredConstructors();
            boolean hasEmpty = false;
            CtConstructor constructor;
            for (CtConstructor item : constructors) {
                constructor = new CtConstructor(item, target, null);
                if (item.isConstructor()) {
                    hasEmpty = true;
                    constructor.setBody("super();");
                }
                constructor.setModifiers(Modifier.PUBLIC);
                target.addConstructor(constructor);
            }
            if (hasEmpty)
                return;
            target.addConstructor(CtNewConstructor.make(String.format("public %s(){}", target.getSimpleName()), target));
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

    public static interface ModifyMethod {

        /***
         *
         * @param paramsTag
         *            方法体里所有的参数代代替码/$1代表每个参数$2为第二个参数.......
         * @return
         */
        default String appCodeToBefore(CtMethod ctMethod, String methodName, String paramsTag) {
            return "";
        }

        default String overrideSuperMethod(CtMethod ctMethod, String methodName, String paramsTag) {
            //			return "super." + methodName + "(" + paramsTag + ");";
            return null;
        }

        /***
         *
         * @param paramsTag
         *            方法体里所有的参数代代替码/$1代表每个参数$2为第二个参数.......
         * @return
         */
        default String appCodeToAfter(CtMethod ctMethod, String methodName, String paramsTag) {
            return "";
        }
    }

    public byte[] getCode(Class clazz) throws NotFoundException, IOException, CannotCompileException {
        return pool.get(clazz.getName()).toBytecode();
    }

    public static class FieldModel {

        private Map<Class<?>, List<String>> annotations;
        private String modifier;
        private Class<?> returnType;
        private String name;
        private String instance;
        private boolean createGetting;
        private boolean createSetting;

        public FieldModel appendAnnotation(Class<?> annotation, String... attributes) {
            if (null == annotation)
                return this;
            List<String> list = null;
            if (null == annotations) {
                annotations = new HashMap<Class<?>, List<String>>();
            }
            if (null != attributes && attributes.length > 0) {
                list = annotations.get(annotation);
                if (null == list) {
                    list = new LinkedList<String>();
                }
                list.addAll(Arrays.asList(attributes));
            }
            annotations.put(annotation, list);
            return this;
        }

        public Map<Class<?>, List<String>> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(Map<Class<?>, List<String>> annotations) {
            this.annotations = annotations;
        }

        public String getModifier() {
            return modifier;
        }

        public void setModifier(String modifier) {
            this.modifier = modifier;
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public void setReturnType(Class<?> returnType) {
            this.returnType = returnType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getInstance() {
            return instance;
        }

        public void setInstance(String instance) {
            this.instance = instance;
        }

        public boolean isCreateGetting() {
            return createGetting;
        }

        public void setCreateGetting(boolean createGetting) {
            this.createGetting = createGetting;
        }

        public boolean isCreateSetting() {
            return createSetting;
        }

        public void setCreateSetting(boolean createSetting) {
            this.createSetting = createSetting;
        }

        public FieldModel(String modifier, Class<?> returnType, String name, String instance, boolean createGetting, boolean createSetting) {
            super();
            this.modifier = modifier;
            this.returnType = returnType;
            this.name = name;
            this.instance = instance;
            this.createGetting = createGetting;
            this.createSetting = createSetting;
        }
    }

    public static void main(String[] args) throws Throwable {
        ClassRebuild c = new ClassRebuild();
        c.build(ClassRebuild.class, null, null, null);
    }

}
