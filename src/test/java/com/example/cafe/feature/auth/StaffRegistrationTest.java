package com.example.cafe.feature.auth;

import com.example.cafe.common.entity.auth.User;
import com.example.cafe.feature.auth.dto.request.RegisterRequest;
import com.example.cafe.feature.auth.repository.UserRepository;
import com.example.cafe.feature.auth.service.impl.AuthServiceImpl;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class StaffRegistrationTest {
  @Test void createsOnlyStaffAndHashesPasswordWithoutMintingALoginToken() {
    var encoder = new BCryptPasswordEncoder(4);
    User[] saved = new User[1];
    var repository = (UserRepository) Proxy.newProxyInstance(UserRepository.class.getClassLoader(),
        new Class<?>[] {UserRepository.class}, (proxy, method, args) -> switch (method.getName()) {
          case "existsByUsername", "existsByEmail" -> false;
          case "save" -> { saved[0] = (User) args[0]; saved[0].setId(12L); yield saved[0]; }
          default -> throw new AssertionError("Unexpected repository call " + method.getName());
        });
    var service = new AuthServiceImpl(repository, encoder, null, null);
    var request = new RegisterRequest();
    request.setUsername("staffnew01");
    request.setPassword("test-password");
    request.setEmail("staff@example.test");
    var response = service.register(request);
    assertEquals(12L, response.id());
    assertEquals("STAFF", response.role());
    assertNotEquals(request.getPassword(), saved[0].getPassword());
    assertTrue(encoder.matches(request.getPassword(), saved[0].getPassword()));
  }
}
