package com.fiap.mecatronica.monitoramento.model;

/**
 * Classificacao de uma medicao em relacao a faixa de operacao do sensor.
 *
 * E calculada no servidor (MedicaoService) e persistida junto com a medicao,
 * para que o app apenas exiba o status devolvido pela API.
 */
public enum StatusMedicao {
    NORMAL,
    ALERTA,
    CRITICO
}
