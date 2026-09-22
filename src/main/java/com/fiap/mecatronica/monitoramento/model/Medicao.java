package com.fiap.mecatronica.monitoramento.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Leitura coletada por um sensor do cortador de grama autonomo.
 *
 * O JSON devolvido ao app tem o formato:
 * { "id": 1, "sensor": { ... }, "valor": 12.5, "data": "2026-09-21T21:40:00", "status": "NORMAL" }
 */
@Entity
@Table(name = "medicoes")
public class Medicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sensor que produziu a leitura. Vai completo no JSON (nome, tipo, unidade, limites...).
    @ManyToOne(optional = false)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Column(name = "valor", nullable = false)
    private Double valor;

    // Enviada ao app como texto ISO 8601 sem fracao de segundo, ex.: 2026-09-21T21:40:00
    @Column(name = "data", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime data;

    // Calculado pelo MedicaoService no momento do registro
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private StatusMedicao status;

    public Medicao() {
    }

    public Medicao(Sensor sensor, Double valor, LocalDateTime data, StatusMedicao status) {
        this.sensor = sensor;
        this.valor = valor;
        this.data = data;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public StatusMedicao getStatus() {
        return status;
    }

    public void setStatus(StatusMedicao status) {
        this.status = status;
    }
}
