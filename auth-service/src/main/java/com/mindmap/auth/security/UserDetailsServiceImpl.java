package com.mindmap.auth.security;

import com.mindmap.auth.entity.User;
import com.mindmap.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementation backed by PostgreSQL UserRepository.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username cannot be blank");
        }

        String lowerUsername = username.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByUserIdLower(lowerUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + username));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .collect(Collectors.toList());

        boolean accountNonLocked = !user.isAccountLocked();

        return new org.springframework.security.core.userdetails.User(
                user.getUserId(),
                user.getPasswordHash(),
                user.isActive(),
                true,
                true,
                accountNonLocked,
                authorities
        );
    }
}
