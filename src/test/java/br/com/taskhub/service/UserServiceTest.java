package br.com.taskhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.taskhub.dto.UserRequestDTO;
import br.com.taskhub.dto.UserResponseDTO;
import br.com.taskhub.entity.User;
import br.com.taskhub.exception.ConflictException;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.mapper.UserMapper;
import br.com.taskhub.repository.UserRepository;

@ExtendWith (MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void shouldCreateUserWithNormalizedNameAndEmail() {
        UserResponseDTO response = new UserResponseDTO(UUID.randomUUID(), "Ana", "ana@email.com", LocalDateTime.now());
        when(userRepository.existsByEmail("ana@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(response);

        UserResponseDTO result = userService.create(new UserRequestDTO("  Ana ", "  Ana@Email.COM "));

        assertThat(result).isEqualTo(response);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo("Ana");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("ana@email.com");
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("ana@email.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(new UserRequestDTO("Ana", "Ana@Email.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ana@email.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldFindUserById() {
        UUID userId = UUID.randomUUID();
        User user = new User("Ana", "ana@email.com");
        UserResponseDTO response = new UserResponseDTO(userId, "Ana", "ana@email.com", LocalDateTime.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponseDTO result = userService.findById(userId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldReturn404WhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }
}
