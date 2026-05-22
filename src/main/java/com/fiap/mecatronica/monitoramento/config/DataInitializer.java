package com.fiap.mecatronica.monitoramento.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.fiap.mecatronica.monitoramento.model.Sensor;
import com.fiap.mecatronica.monitoramento.repository.SensorRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SensorRepository sensorRepository;

    @Override
    public void run(String... args) {
        if (sensorRepository.count() > 0) {
            return;
        }

        System.out.println("Inicializando banco de dados com sensores do cortador de grama autonomo...");

        sensorRepository.save(new Sensor(
                "Sensor Ultrassonico HC-SR04",
                "vegetacao",
                "Frente do cortador - chassi inferior",
                "cm",
                5.0,
                30.0,
                true
        ));

        sensorRepository.save(new Sensor(
                "Sensor de Umidade do Solo Capacitivo",
                "umidade",
                "Haste lateral direita",
                "%",
                20.0,
                80.0,
                true
        ));

        sensorRepository.save(new Sensor(
                "Sensor de Inclinacao MPU6050",
                "inclinacao",
                "Centro do chassi",
                "graus",
                0.0,
                25.0,
                true
        ));

        sensorRepository.save(new Sensor(
                "Sensor de Temperatura DHT22",
                "temperatura",
                "Compartimento eletronico",
                "C",
                5.0,
                45.0,
                true
        ));

        sensorRepository.save(new Sensor(
                "Sensor LIDAR de Densidade Vegetal",
                "densidade",
                "Topo do cortador - torre dianteira",
                "%",
                10.0,
                100.0,
                true
        ));

        sensorRepository.save(new Sensor(
                "Sensor Infravermelho de Obstaculos",
                "obstaculo",
                "Parachoque frontal",
                "cm",
                10.0,
                200.0,
                true
        ));

        sensorRepository.save(new Sensor(
                "Sensor GPS NEO-6M",
                "localizacao",
                "Topo do cortador - antena",
                "graus",
                null,
                null,
                true
        ));

        sensorRepository.save(new Sensor(
                "Sensor de Crescimento NDVI",
                "crescimento",
                "Camera multiespectral frontal",
                "indice",
                0.2,
                0.8,
                true
        ));

        System.out.println("Sensores cadastrados com sucesso. Sistema pronto para uso.");
    }
}
