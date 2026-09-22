package com.fiap.mecatronica.monitoramento.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fiap.mecatronica.monitoramento.dto.MedicaoRequest;
import com.fiap.mecatronica.monitoramento.exception.SensorNaoEncontradoException;
import com.fiap.mecatronica.monitoramento.model.Medicao;
import com.fiap.mecatronica.monitoramento.service.MedicaoService;

/**
 * Endpoints de medicoes consumidos pelo app React Native (Sprint 3).
 *
 * @CrossOrigin libera o acesso a partir de outra origem (Expo web na porta 8081,
 * emulador Android ou celular fisico); sem isso o navegador bloqueia as chamadas.
 */
@RestController
@RequestMapping("/api/medicoes")
@CrossOrigin(origins = "*")
public class MedicaoController {

    @Autowired
    private MedicaoService medicaoService;

    @GetMapping
    public ResponseEntity<List<Medicao>> listarTodas() {
        return ResponseEntity.ok(medicaoService.listarTodas());
    }

    @GetMapping("/sensor/{sensorId}")
    public ResponseEntity<List<Medicao>> listarPorSensor(@PathVariable Long sensorId) {
        try {
            return ResponseEntity.ok(medicaoService.listarPorSensor(sensorId));
        } catch (SensorNaoEncontradoException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Medicao> registrar(@RequestBody MedicaoRequest request) {
        if (request.sensorId() == null || request.valor() == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Medicao medicao = medicaoService.registrar(request.sensorId(), request.valor());
            return ResponseEntity.status(HttpStatus.CREATED).body(medicao);
        } catch (SensorNaoEncontradoException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Botao "Simular coleta" do app: gera e persiste uma leitura por sensor ativo.
     */
    @PostMapping("/simular")
    public ResponseEntity<List<Medicao>> simular() {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicaoService.simularColeta());
    }
}
