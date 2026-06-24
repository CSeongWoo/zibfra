package com.example.zipfra.security;

import io.jsonwebtoken.Claims;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
@Builder
public class ZipfraPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    // 필드 초기화는 생성자에서 처리하므로 여기서 할당하지 않습니다.
    private final Collection<? extends GrantedAuthority> authorities;

    // 1. 일반 생성자: Lombok이 @RequiredArgsConstructor로 생성해주지만,
    // authorities 초기화를 위해 커스텀 생성자를 둡니다.
    public ZipfraPrincipal(Long id, String email) {
        this(id, email, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // 2. Claims 생성자
    public ZipfraPrincipal(Claims claims) {
        this(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class))));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}