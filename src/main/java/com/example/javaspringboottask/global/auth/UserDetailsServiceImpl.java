package com.example.javaspringboottask.global.auth;

import com.example.javaspringboottask.user.entity.User;
import com.example.javaspringboottask.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new UsernameNotFoundException("username 에 해당하는 사용자가 존재하지 않습니다."));

        return new UserDetailsImpl(user);
    }
}
