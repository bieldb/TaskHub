package br.com.taskhub.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "users")
@Getter 
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class User {

    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @Column (name = "name", nullable = false, length = 120)
    @Setter 
    private String name;

    @Column (name = "email", nullable = false, length = 255)
    @Setter    
    private String email;

    @Column (name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
