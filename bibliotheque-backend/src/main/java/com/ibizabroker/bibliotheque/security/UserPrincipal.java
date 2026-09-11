package com.ibizabroker.bibliotheque.security;

import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Identité complète de l'utilisateur authentifié : porte le userId (indispensable
 * aux règles RS-03 / RS-04 / RS-05) et les autorités mappées (rôles historiques
 * + rôles canoniques). Est placé dans le SecurityContext par le JwtRequestFilter.
 */
public class UserPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private static final String PREFIX_ROLE = "ROLE_";

    private final Integer userId;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Integer userId, String username, String password,
                         Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserPrincipal from(Users user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (user.getRole() != null) {
            for (Role role : user.getRole()) {
                AppRoles.authorityRoleNames(role.getRoleName()).forEach(nom ->
                        authorities.add(new SimpleGrantedAuthority(PREFIX_ROLE + nom)));
            }
        }
        return new UserPrincipal(user.getUserId(), user.getUsername(), user.getPassword(), authorities);
    }

    public Integer getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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

    /** Indique si l'utilisateur possède le rôle canonique donné (ex : AppRoles.ADHERENT). */
    public boolean hasRole(String roleName) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authorite -> authorite.equals(PREFIX_ROLE + roleName));
    }
}