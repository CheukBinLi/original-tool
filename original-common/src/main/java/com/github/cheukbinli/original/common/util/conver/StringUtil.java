package com.github.cheukbinli.original.common.util.conver;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 *
 * @Title: original-common
 * @Description: 字段串工具
 * @Company:
 * @Email: 20796698@qq.com
 * @author cheuk.bin.li
 * @date 2017年12月19日 上午9:32:04
 *
 */
public class StringUtil extends ConverType {

    private static StringUtil INSTANCE;

    public static final String EMPTY = "";

    protected StringUtil() {
    }

    @Deprecated
    public static StringUtil newInstance() {
        if (null == INSTANCE) {
            synchronized (StringUtil.class) {
                if (null == INSTANCE) {
                    INSTANCE = new StringUtil();
                }
            }
        }
        return INSTANCE;
    }

    /***
     * @see
     *      <p>
     *      生成正则字符串 例:
     *      <p>
     *      传入: get|load|read
     *      <p>
     *      返回 : ^.*get.*$|^.*load.*$|^.*read.*$
     * @param value
     * @return
     */
    public static String generateRegexString(String value) {
        if (null == value || value.length() < 1)
            return value;
        String result = value;
        result = "^*" + result.replaceAll("\\|", ".*\\$|\\^.*") + "*$";
        result = result.replaceAll("\\.\\*", "\\*");
        result = result.replaceAll("\\*|\\*\\*", "(\\.*)?");
        return result;
    }

    public static int concatCount(String content, String regex) {
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(content);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    public static String toUpperCaseFirstOne(String name) {
        char[] ch = name.toCharArray();
        ch[0] = Character.toUpperCase(ch[0]);
        return new String(ch);
    }

    public static String toLowerCaseFirstOne(String name) {
        if (name.length() < 1)
            return name;
        char[] ch = name.toCharArray();
        ch[0] = Character.toLowerCase(ch[0]);
        return new String(ch);
    }

    public static boolean isEmpty(String str) {
        if (null == str || str.length() < 1)
            return true;
        return false;
    }

    public static String isEmpty(String str, String defaultValue) {
        if (null == str || str.length() < 1)
            return defaultValue;
        return str;
    }

    public static String isEmpty(String str, String defaultValue, boolean isDefaultValueToUpperCaseFirstOne) {
        if (null == str || str.length() < 1)
            if (null == str || str.length() < 1)
                return isDefaultValueToUpperCaseFirstOne ? toLowerCaseFirstOne(defaultValue) : defaultValue;
        return str;
    }

    public static boolean isBlank(String str) {
        if (isEmpty(str) || str.trim().length() < 1)
            return true;
        return false;
    }

    public static boolean isAllBlank(String... str) {
        if (null == str || str.length < 1) {
            return true;
        }
        for (String item : str) {
            if (!isBlank(item)) {
                return false;
            }
        }
        return true;
    }

    public static String isBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /***
     *
     * 驼峰变量名字转下划线变量
     *
     * @param str
     * @return
     */
    public static String toLowerCaseUnderscoreCamel(String str) {
        if (isEmpty(str))
            return EMPTY;
        StringBuilder result = new StringBuilder();
        for (Character item : str.toCharArray()) {
            result.append((65 <= item && 90 >= item) ? "_" + (char) (item + 32) : item);
        }
        return result.toString();
    }

    /***
     * 下划线变量名字转驼峰变量
     *
     * @param str
     * @return
     */
    public static String toLowerUnderscoreCaseCamel(String str) {
        if (isEmpty(str))
            return EMPTY;
        StringBuilder result = new StringBuilder();
        boolean nextChange = false;
        for (Character item : str.toLowerCase().toCharArray()) {
            if (item == '_') {
                nextChange = true;
                continue;
            }
            result.append(nextChange ? (char) (item - 32) : item);
            if (nextChange) {
                nextChange = false;
            }
        }
        return result.toString();
    }

    public static Long[] converLongs(String... strs) {
        if (null == strs || strs.length < 1)
            return new Long[0];
        Long[] result = new Long[strs.length];
        for (int i = 0, len = strs.length; i < len; i++) {
            result[i] = Long.valueOf(Long.valueOf(strs[i]));
        }
        return result;
    }

    public static Integer[] converIntegers(String... strs) {
        if (null == strs || strs.length < 1)
            return new Integer[0];
        Integer[] result = new Integer[strs.length];
        for (int i = 0, len = strs.length; i < len; i++) {
            result[i] = Integer.valueOf(Integer.valueOf(strs[i]));
        }
        return result;
    }

    /***
     * 补位
     *
     * @param content
     * @param ch
     *            补位字符
     * @param len
     *            位数
     * @param left
     *            是否在左边补位
     * @return
     */
    public static String fillPosition(String content, char ch, int len, boolean left) {
        if (null == content || content.length() >= len)
            return content;
        StringBuilder result = new StringBuilder();
        for (int i = content.length(); i < len; i++) {
            result.append(ch);
        }
        return left ? result.toString() + content : content + result.toString();
    }

    /***
     * 向左补位
     *
     * @param content
     * @param ch
     *            补位字符
     * @param len
     *            位数
     * @return
     */
    public static String fillPositionLeft(String content, char ch, int len) {
        return fillPosition(content, ch, len, true);
    }

    /***
     * 向右补位
     *
     * @param content
     * @param ch 补位字符
     * @param len 位数
     * @return
     */
    public static String fillPositionRight(String content, char ch, int len) {
        return fillPosition(content, ch, len, false);
    }

    /***
     * 过滤指定符号
     *
     * @param str
     * @param cs
     *            要过滤的符号
     * @return
     * @throws UnsupportedEncodingException
     */
    public static byte[] filterCharestBytes(String str, Character... cs) throws UnsupportedEncodingException {
        if (isEmpty(str)) {
            return new byte[0];
        } else if (null == cs || cs.length < 1) {
            return str.getBytes("UTF-8");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Set<Character> filter = new HashSet<>(Arrays.asList(cs));
        Character code;
        for (int i = 0; i < str.length(); i++) {
            if (!filter.contains(code = str.charAt(i))) {
                out.write(code);
            }
        }
        return out.toByteArray();
    }

    /***
     * 过滤指定符号
     *
     * @param str
     * @param cs
     *            要过滤的符号
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String filterCharest(String str, Character... cs) {
        if (isEmpty(str) || (null == cs || cs.length < 1)) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        Set<Character> filter = new HashSet<>(Arrays.asList(cs));
        Character code;
        for (int i = 0; i < str.length(); i++) {
            if (!filter.contains(code = str.charAt(i))) {
                result.append(code);
            }
        }
        return result.toString();
    }

    /***
     * 拼接
     *
     * @param coupler
     *            偶合连接字符
     * @param str
     * @return
     */
    public static String assemble(String coupler, String... str) {
        if (null == str || str.length < 1)
            return null;
        coupler = isEmpty(coupler, EMPTY);
        StringBuilder result = new StringBuilder();
        String value;
        for (String item : str) {
            if (null == item || isEmpty(value = item)) {
                continue;
            }
            result.append(coupler).append(value);
        }
        return result.length() > 0 ? result.substring(coupler.length()) : null;
    }

    public static String assemble(String coupler, Long... values) {
        if (null == values || values.length < 1)
            return null;
        coupler = isEmpty(coupler, EMPTY);
        StringBuilder result = new StringBuilder();
        for (Long item : values) {
            if (null == item) {
                continue;
            }
            result.append(coupler).append(Long.toString(item));
        }
        return result.length() > 0 ? result.substring(coupler.length()) : null;
    }

    /***
     *
     * 拼接
     *
     * key${valueCoupler}value${variableCoupler}key${valueCoupler}value${variableCoupler}
     *
     * @param valueCoupler
     *            key和value连接字符
     * @param param
     * @param variableCoupler
     *            变量与变量连接字符
     * @return
     */
    public static String assemble(String valueCoupler, Map<Object, Object> param, String variableCoupler) {
        if (CollectionUtil.isEmpty(param))
            return null;
        valueCoupler = isEmpty(valueCoupler, EMPTY);
        variableCoupler = isEmpty(variableCoupler, EMPTY);
        StringBuilder result = new StringBuilder();
        for (Entry<Object, Object> en : param.entrySet()) {
            result.append(variableCoupler).append(en.getKey()).append(valueCoupler).append(en.getValue());
        }
        return result.length() > 0 ? result.substring(variableCoupler.length()) : null;
    }

    public static String getParent(String path, Character separator) {
        int len = 0;
        while (len < path.length()) {
            if (path.charAt(len) == separator && len != 0) {
                return path.substring(0, len);
            }
            len++;
        }
        return path;
    }

    public static PathDirectory getPaths(String path, String split, boolean onlyParent) {
        String[] paths = path.split(split);
        if (null == paths || paths.length < 1)
            return null;
        PathDirectory previous, next;
        PathDirectory result = new PathDirectory();
        PathDirectory last = new PathDirectory();
        int index = 0;
        if (StringUtil.isBlank(paths[index])) {
            index++;
        }
        result
                .setPath(split + paths[index++])
                .setParentDirectory(result)
                .setLastDirectory(last)
                .setParent(true);

        if (onlyParent)
            return result;

        last.setLast(true);

        previous = result;
        StringBuilder tempPath = new StringBuilder(result.getPath());
        for (int i = index, len = paths.length, lastIndex = len - 1; i < len; i++) {
            tempPath.append(split).append(paths[i]);
            next = (i == lastIndex ? last : new PathDirectory())
                    .setLastDirectory(last)
                    .setPreviousDirectory(previous)
                    .setParent(false)
                    .setPath(tempPath.toString())
                    .setParentDirectory(result);
            previous.setNextDirectory(next);
            previous = next;
        }

        return result;
    }

    public static PathDirectory getParentPath(String path, String split) {
        return getPaths(path, split, true);
    }

    public static class PathDirectory {
        private String path;
        private boolean isParent;
        private boolean isLast;
        private PathDirectory parentDirectory;
        private PathDirectory nextDirectory;
        private PathDirectory previousDirectory;
        private PathDirectory lastDirectory;

        public String getPath() {
            return path;
        }

        public PathDirectory setPath(String path) {
            this.path = path;
            return this;
        }

        public boolean isParent() {
            return isParent;
        }

        public boolean isLast() {
            return isLast;
        }

        public PathDirectory setLast(boolean isLast) {
            this.isLast = isLast;
            return this;
        }

        public PathDirectory setParent(boolean isParent) {
            this.isParent = isParent;
            return this;
        }

        public PathDirectory getNextDirectory() {
            return nextDirectory;
        }

        public PathDirectory setNextDirectory(PathDirectory nextDirectory) {
            this.nextDirectory = nextDirectory;
            return this;
        }

        public PathDirectory getParentDirectory() {
            return parentDirectory;
        }

        public PathDirectory setParentDirectory(PathDirectory parentDirectory) {
            this.parentDirectory = parentDirectory;
            return this;
        }

        public PathDirectory getPreviousDirectory() {
            return previousDirectory;
        }

        public PathDirectory setPreviousDirectory(PathDirectory previousDirectory) {
            this.previousDirectory = previousDirectory;
            return this;
        }

        public PathDirectory getLastDirectory() {
            return lastDirectory;
        }

        public PathDirectory setLastDirectory(PathDirectory lastDirectory) {
            this.lastDirectory = lastDirectory;
            return this;
        }


    }

    /***
     * 参数剥离
     */
    public static class StripParam {
        private String content;
        private List<String> paramNames;
        private Map<String, Object> param;
        private List<String> contents;
        private static final String TAG = "@-$";

        public StripParam() {
        }

        public StripParam(String content, List<String> paramNames, List<String> contents, Map<String, Object> param) {
            this.content = content;
            this.paramNames = paramNames;
            this.contents = contents;
            this.param = param;
        }

        public StripParam(String content, List<String> paramNames) {
            this.content = content;
            this.paramNames = paramNames;
        }

        public String rebuildWitchParam() {
            return rebuild(null);
        }

        public String rebuild(String replaceString) {
            if (CollectionUtil.isEmpty(contents)) {
                return content;
            }
            StringBuilder result = new StringBuilder();
            for (String item : contents) {
                if (StringUtil.isBlank(item)) {
                    continue;
                }
                if (item.startsWith(TAG)) {
                    result.append(null == replaceString ? param.get(item.substring(TAG.length())) : replaceString);
                    continue;
                }
                result.append(item);
            }
            return result.toString();
        }

        public String getConetnt() {
            return content;
        }
        public String getContent() {
            return content;
        }

        public void setContent(String conetnt) {
            this.content = conetnt;
        }
        public void setConetnt(String conetnt) {
            this.content = conetnt;
        }

        public List<String> getParamNames() {
            return paramNames;
        }

        public void setParamNames(List<String> paramNames) {
            this.paramNames = paramNames;
        }

        public Map<String, Object> getParam() {
            return param;
        }

        public void setParam(Map<String, Object> param) {
            this.param = param;
        }

        public List<String> getContents() {
            return contents;
        }

        public void setContents(List<String> contents) {
            this.contents = contents;
        }
    }

    public static StripParam stripParam(Pattern pattern, int prefixLen, int suffixLen, String content) {
        return stripParam(pattern, prefixLen, suffixLen, content, true);
    }

    public static StripParam stripParam(Pattern pattern, int prefixLen, int suffixLen, String content, boolean isReplace) {
        if (null == pattern) {
            throw new NullPointerException("pattern can't be null.");
        }
        if (StringUtil.isBlank(content))
            throw new NullPointerException("content name can't be null.");
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        List<String> paramNames = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        Map<String, Object> param = new LinkedHashMap<>();
        String orginParam, paramName;
        int pre = 0;
        while (matcher.find()) {
            orginParam = matcher.group(0);
            paramNames.add(paramName = orginParam.substring(prefixLen, orginParam.length() - suffixLen));
            contents.add(content.substring(pre, matcher.start()));
            contents.add(StripParam.TAG + paramName);
            param.put(paramName, orginParam);
            if(isReplace) {
                matcher.appendReplacement(result, "%s");
            }
            pre = matcher.end();
        }
        contents.add(content.substring(pre));
        matcher.appendTail(result);
        return new StripParam(result.toString(), paramNames, contents, param);
    }

    public static StripParam stripParam(String prefix, String suffix, String content, boolean isReplace) {
        return stripParam(toRegex(prefix) + "[0-9,a-z,A-Z,_]+" + toRegex(suffix), prefix.length(), suffix.length(), content,isReplace);
    }

    public static StripParam stripParam(String prefix, String suffix, String content) {
        return stripParam(toRegex(prefix) + "[0-9,a-z,A-Z,_]+" + toRegex(suffix), prefix.length(), suffix.length(), content);
    }

    public static StripParam stripParam(String regex, int prefixLen, int suffixLen, String content) {
        return stripParam(Pattern.compile(regex), prefixLen, suffixLen, content);
    }

    public static StripParam stripParam(String regex, int prefixLen, int suffixLen, String content, boolean isReplace) {
        return stripParam(Pattern.compile(regex), prefixLen, suffixLen, content,isReplace);
    }


    public static String toRegex(String str){
        if(isBlank(str)){
            return str;
        }
        StringBuilder result =new StringBuilder();
        char[]strChars=str.toCharArray();
//        * . ? + $ ^ [ ] ( ) { } | \ /
        for (char item : strChars) {
            if (
                    '*' == item |
                    '.' == item |
                    '?' == item |
                    '+' == item |
                    '$' == item |
                    '^' == item |
                    '[' == item |
                    ']' == item |
                    '(' == item |
                    ')' == item |
                    '{' == item |
                    '}' == item |
                    '|' == item |
                    '\\' == item |
                    '/' == item

            ) {
                result.append("\\");
            }
            result.append(item);
        }
        return result.toString();
    }

    public static void main(String[] args) {
        // System.out.println(StringUtil.concatCount(
        // "file:/F:/Sync/JavaProject/original-3.0/original-prototype/original-prototype.spring.cloud/original-prototype.spring.cloud.eureka-server/target/original-prototype.spring.cloud.eureka-server-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/original-cache-0.0.1-SNAPSHOT.jar!/lua",
        // ".jar!"));
        //
        // String a =
        // "file:/F:/Sync/JavaProject/original-3.0/original-prototype/original-prototype.spring.cloud/original-prototype.spring.cloud.eureka-server/target/original-prototype.spring.cloud.eureka-server-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/original-cache-0.0.1-SNAPSHOT.jar!/lua";
        // String[] as = a.split("!");
        // System.err.println(Arrays.toString(as));
        // System.out.println(a.substring(a.lastIndexOf(".jar!") + 5));
        //
        // System.out.println(new
        // StringUtil().toLowerUnderscoreCaseCamel("a_abcde_fghijk_"));
        //
        // System.err.println(newInstance().fillPositionLeft("x", '0', 3));
        // System.err.println(newInstance().fillPositionRight("x", '0', 3));
        //
        PathDirectory pd = getPaths("/test/a/b/c/x", "/", false);
        System.err.println(pd.getParentDirectory().getPath());
        System.err.println(getParent("/test/a/b/c/x", '/'));

        StripParam r= stripParam("${","}","你${xxx},给你${mmm}.");
//        StripParam r= stripParam("\\$\\{[0-9,a-z,A-Z]+\\}",2,1,"叼你${xxx},给你${mmm}.");
        System.out.println(r.getConetnt());
    }
}
