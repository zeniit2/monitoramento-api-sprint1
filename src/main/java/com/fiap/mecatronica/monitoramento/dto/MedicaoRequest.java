package com.fiap.mecatronica.monitoramento.dto;

/**
 * Corpo do POST /api/medicoes (registro manual de uma leitura).
 *
 * Exemplo: { "sensorId": 1, "valor": 18.5 }
 */
public record MedicaoRequest(Long sensorId, Double valor) {
}
