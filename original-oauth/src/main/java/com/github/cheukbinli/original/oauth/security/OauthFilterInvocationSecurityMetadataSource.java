package com.github.cheukbinli.original.oauth.security;

import com.github.cheukbinli.original.common.util.conver.CollectionUtil;
import com.github.cheukbinli.original.common.util.conver.CollectionUtil.SetBuilder;
import com.github.cheukbinli.original.common.util.conver.StringUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class OauthFilterInvocationSecurityMetadataSource implements FilterInvocationSecurityMetadataSource {

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final Comparator<String> LENGTH_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.length() < o2.length() ? 1 : o1.length() == o2.length() ? 0 : -1;
        }
    };

    //	private Map<String, Set<ConfigAttribute>> RESOURCES = new ConcurrentHashMap<String, Set<ConfigAttribute>>();
//	private LinkedList<String> RESOURCES_NAME = new LinkedList<String>();
//	private Map<String, Set<String>> RESOURCES_AUTH_GROUP = new ConcurrentHashMap<String, Set<String>>();
    private Map<String, Map<String, Set<ConfigAttribute>>> RESOURCES_PARENT_PATH_GROUP = new ConcurrentHashMap<String, Map<String, Set<ConfigAttribute>>>();

    /***
     *
     * @param url url
     * @param isAddRoot 是否为添加根类型
     * @param role 权限
     * @return
     */
    public OauthFilterInvocationSecurityMetadataSource append(String url, boolean isAddRoot, String... role) {
        SetBuilder builder = CollectionUtil.setBuilder(false);
        Set<ConfigAttribute> attributes;
//		RESOURCES_NAME.add(url);
//		Set<String> urls;
        for (String item : role) {
            builder.append(new OauthConfigAttribute(item));
            // 权限-地址表
//			urls = RESOURCES_AUTH_GROUP.get(item);
//			if (null == urls) {
//				RESOURCES_AUTH_GROUP.put(item, urls = new HashSet<String>());
//			}
//			urls.add(url);
        }
//		RESOURCES.put(url, attributes = builder.build());
        attributes = builder.build();
        // 分组
        String parentDirectory = StringUtil.getParent(url, '/');
        Map<String, Set<ConfigAttribute>> parent = RESOURCES_PARENT_PATH_GROUP.get(parentDirectory);
        if (null == parent) {
            parent = new TreeMap<String, Set<ConfigAttribute>>(LENGTH_COMPARATOR);
            RESOURCES_PARENT_PATH_GROUP.put(parentDirectory, parent);
        }
//		url = url.equals(parentDirectory) ? url + "/**" : url;
        if (isAddRoot) {
            if (parent.containsKey(url) || parent.containsKey(url + "/") || parent.containsKey(url + "/*") || parent.containsKey(url + "/**"))
                return this;
            url = url.equals(parentDirectory) ? url + "/**" : url;
        }
        parent.put(url, attributes);
        return this;
    }

    public OauthFilterInvocationSecurityMetadataSource sort() {
//		RESOURCES_NAME.sort(LENGTH_COMPARATOR);
        return this;
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        FilterInvocation filterInvocation = (FilterInvocation) object;
        final String url = filterInvocation.getRequest().getServletPath();
        // 分组管理
        // Authentication auth =
        // SecurityContextHolder.getContext().getAuthentication();
        // if (auth instanceof OauthAuthenticationToken) {
        // RESOURCES_AUTH_GROUP.get(((User)auth.getPrincipal()).getAuthorities())
        // }
        // 排序，从长到短做匹配。高相拟匹配原则，匹配通过即返回
        Map<String, Set<ConfigAttribute>> group = RESOURCES_PARENT_PATH_GROUP.get(StringUtil.getParent(url, '/'));
        if (null == group) {
            try {
                filterInvocation.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e) {
                throw new AccessDeniedException("not found url");
            }
            return null;
        }
        for (Entry<String, Set<ConfigAttribute>> item : group.entrySet()) {
            if (antPathMatcher.match(item.getKey(), url)) {
//				System.err.println(item);
                return item.getValue();
            }
        }
        throw new AccessDeniedException("not allow");
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        List<ConfigAttribute> attributes = new LinkedList<>();
        attributes.add(new OauthConfigAttribute());
        return attributes;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

}
