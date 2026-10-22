package br.com.taskhub.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.taskhub.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);
}
