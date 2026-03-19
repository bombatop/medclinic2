package com.medclinic.auth.security;

import com.medclinic.auth.repository.UserRepository;
import com.medclinic.auth.service.RbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RbacService rbacService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> {
                    var roles = rbacService.resolveRoles(user);
                    var permissionCodes = rbacService.resolvePermissionCodes(roles);
                    var authorities = new ArrayList<SimpleGrantedAuthority>();
                    roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode())));
                    permissionCodes.forEach(permissionCode -> authorities.add(new SimpleGrantedAuthority("PERM_" + permissionCode)));

                    return new User(
                            user.getUsername(),
                            user.getPassword(),
                            user.isActive(),
                            true, true, true,
                            authorities
                    );
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
