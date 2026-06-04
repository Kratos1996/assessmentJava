package com.developer.test.service;

import com.developer.test.dto.CreateUserRequest;
import com.developer.test.exception.BadRequestException;
import com.developer.test.exception.NotFoundException;
import com.developer.test.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests for user validation and creation rules.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceQualityTest {
    @Mock
    private DataStore dataStore;

    @Mock
    private StatsService statsService;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserTrimsInputAndInvalidatesStatsCache() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("  Ada Lovelace  ");
        request.setEmail("  ada@example.com  ");
        request.setRole("  engineer  ");

        when(dataStore.createUser("Ada Lovelace", "ada@example.com", "engineer"))
                .thenReturn(new User(10, "Ada Lovelace", "ada@example.com", "engineer"));

        User created = userService.createUser(request);

        assertThat(created.getId()).isEqualTo(10);
        verify(dataStore).createUser("Ada Lovelace", "ada@example.com", "engineer");
        verify(statsService).invalidate();
    }

    @Test
    void createUserRejectsInvalidEmail() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Ada");
        request.setEmail("not-an-email");
        request.setRole("engineer");

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("email must be a valid email address");
    }

    @Test
    void getUserByIdThrowsNotFoundForMissingUser() {
        when(dataStore.getUserById(42)).thenReturn(null);

        assertThatThrownBy(() -> userService.getUserById(42))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id 42 was not found");
    }
}
