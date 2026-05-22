package com.fiap.mecatronica.monitoramento.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fiap.mecatronica.monitoramento.model.Sensor;
import com.fiap.mecatronica.monitoramento.service.SensorService;

@RestController
@RequestMapping("/api/sensores")
@CrossOrigin(origins = "*")
public class SensorController {

    @Autowired
    private SensorService sensorService;

    @GetMapping
    public ResponseEntity<List<Sensor>> listarTodos() {
        return ResponseEntity.ok(sensorService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sensor> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sensorService.buscarPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Sensor>> listarPorTipo(@PathVariable String tipo) {
        return ResponseEntity.ok(sensorService.listarPorTipo(tipo));
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<Sensor>> listarAtivos() {
        return ResponseEntity.ok(sensorService.listarAtivos());
    }

    @GetMapping("/local/{local}")
    public ResponseEntity<List<Sensor>> buscarPorLocal(@PathVariable String local) {
        return ResponseEntity.ok(sensorService.buscarPorLocal(local));
    }

    @PostMapping
    public ResponseEntity<Sensor> criar(@RequestBody Sensor sensor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorService.criar(sensor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sensor> atualizar(@PathVariable Long id, @RequestBody Sensor sensor) {
        try {
            return ResponseEntity.ok(sensorService.atualizar(id, sensor));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        try {
            sensorService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
