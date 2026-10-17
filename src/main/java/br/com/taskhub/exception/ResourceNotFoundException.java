package br.com.taskhub.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, UUID id) {
        super(resource + " não encontrado(a): " + id);
    }
}
