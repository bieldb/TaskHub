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
@Table (name = "projects")
@Getter 
@NoArgsConstructor (access = AccessLevel.PROTECTED) 
public class Project {

    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @Column (name = "name", nullable = false, length = 100)
    @Setter 
    private String name;

    @Column (name = "description", length = 500)
    @Setter 
    private String description;

    @Column (name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Project(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
