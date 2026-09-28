package com.example.cafe.security;

import com.example.cafe.entity.User;
import com.example.cafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

    return org.springframework.security.core.userdetails.User.withUsername(user.getUsername()).password(user.getPassword())
        .authorities(new ArrayList<>()).build();
  }
}
