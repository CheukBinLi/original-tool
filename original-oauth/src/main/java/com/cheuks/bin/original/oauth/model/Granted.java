package com.github.cheukbinli.original.oauth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Granted implements GrantedAuthority {

    private static final long serialVersionUID = 1L;

    private String authority;//权限

    @Override
    public String getAuthority() {
        return this.authority;
    }

    @Override
    public int hashCode() {
        return this.authority.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj || !(obj instanceof Granted))
            return false;
        return this.authority.equals(((Granted) obj).authority);
    }

}
