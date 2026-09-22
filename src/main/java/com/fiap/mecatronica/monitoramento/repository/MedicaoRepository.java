package com.fiap.mecatronica.monitoramento.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fiap.mecatronica.monitoramento.model.Medicao;

@Repository
public interface MedicaoRepository extends JpaRepository<Medicao, Long> {

    // Historico completo, da leitura mais recente para a mais antiga
    List<Medicao> findAllByOrderByDataDescIdDesc();

    // Historico de um unico sensor (sensor.id)
    List<Medicao> findBySensorIdOrderByDataDescIdDesc(Long sensorId);
}
