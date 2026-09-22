package com.fiap.mecatronica.monitoramento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lancada quando um id de sensor nao existe. Vira HTTP 404 automaticamente
 * (via @ResponseStatus) e permite que os controllers tratem so este caso,
 * sem mascarar outros erros como "nao encontrado".
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class SensorNaoEncontradoException extends RuntimeException {

    public SensorNaoEncontradoException(Long id) {
        super("Sensor nao encontrado com id: " + id);
    }
}
