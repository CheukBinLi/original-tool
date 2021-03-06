package com.github.cheukbinli.original.oauth.model;

import com.github.cheukbinli.original.common.util.conver.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Role implements Serializable {

    private static final long serialVersionUID = 6934763213719225849L;

    private long id;
    private String name;
    private String remark;
    private Set<Granted> granteds;

    public Role appendGranted(Set<String> authoritys) {
        if (null == authoritys || authoritys.size() < 1)
            return this;
        appendGranted(authoritys.toArray(new String[0]));
        return this;
    }

    public Role appendGranted(String... authoritys) {
        if (null == authoritys || authoritys.length < 1)
            return this;
        if (null == granteds) {
            granteds = new HashSet<Granted>();
        }
        for (String item : authoritys) {
            if (StringUtil.isBlank(item))
                continue;
            granteds.add(new Granted(item));
        }
        return this;
    }

    public Role setId(long id) {
        this.id = id;
        return this;
    }

    public Role setName(String name) {
        this.name = name;
        return this;
    }

    public Role setRemark(String remark) {
        this.remark = remark;
        return this;
    }

    public Role setGranteds(Set<Granted> granteds) {
        this.granteds = granteds;
        return this;
    }

}
