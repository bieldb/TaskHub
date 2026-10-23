package br.com.taskhub.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.taskhub.audit.AuditEvent;
import br.com.taskhub.dto.UserRequestDTO;
import br.com.taskhub.dto.UserResponseDTO;
import br.com.taskhub.entity.AuditEntityType;
import br.com.taskhub.entity.User;
import br.com.taskhub.exception.ConflictException;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.mapper.UserMapper;
import br.com.taskhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        String email = dto.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Já existe um usuário com o email " + email);
        }
        User user = new User(dto.name().trim(), email);
        User saved = userRepository.save(user);
        UserResponseDTO response = userMapper.toResponse(saved);
        eventPublisher.publishEvent(AuditEvent.created(AuditEntityType.USER, saved.getId(), response));
        return response;
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        return userMapper.toResponse(findUserOrThrow(id));
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }
}
