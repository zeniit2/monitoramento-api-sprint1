package com.fiap.mecatronica.monitoramento.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fiap.mecatronica.monitoramento.model.Sensor;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    List<Sensor> findByTipoIgnoreCase(String tipo);

    List<Sensor> findByAtivoTrue();

    List<Sensor> findByLocalContainingIgnoreCase(String local);
}
