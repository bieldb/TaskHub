package br.com.taskhub.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.taskhub.dto.UserRequestDTO;
import br.com.taskhub.dto.UserResponseDTO;
import br.com.taskhub.exception.ConflictException;
import br.com.taskhub.exception.ResourceNotFoundException;
import br.com.taskhub.service.UserService;

@WebMvcTest (UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UUID userId;
    private UserResponseDTO response;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        response = new UserResponseDTO(userId, "Ana", "ana@email.com", LocalDateTime.now());
    }

    @Test
    void shouldReturn201WithLocationWhenUserIsCreated() throws Exception {
        when(userService.create(any(UserRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Ana", "email": "Ana@Email.com"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/users/" + userId))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("ana@email.com"));
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Ana", "email": "nao-e-email"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")));

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn400WhenNameAndEmailAreBlank() throws Exception {
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "", "email": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("name", "email")));

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        when(userService.create(any(UserRequestDTO.class)))
                .thenThrow(new ConflictException("Já existe um usuário com o email ana@email.com"));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Ana", "email": "ana@email.com"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"))
                .andExpect(jsonPath("$.detail").value("Já existe um usuário com o email ana@email.com"));
    }

    @Test
    void shouldReturn409WhenDatabaseConstraintIsViolated() throws Exception {
        when(userService.create(any(UserRequestDTO.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Ana", "email": "ana@email.com"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A operação viola uma restrição de integridade dos dados"));
    }

    @Test
    void shouldReturnUserById() throws Exception {
        when(userService.findById(userId)).thenReturn(response);

        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Ana"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.findById(userId)).thenThrow(new ResourceNotFoundException("Usuário", userId));

        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Usuário não encontrado(a): " + userId));
    }

    @Test
    void shouldReturn500WithoutLeakingDetailsOnUnexpectedError() throws Exception {
        when(userService.findById(userId)).thenThrow(new IllegalStateException("detalhe interno sensível"));

        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value("Erro interno inesperado"));
    }
}
