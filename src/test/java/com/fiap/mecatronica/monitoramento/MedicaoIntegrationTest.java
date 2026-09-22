package com.fiap.mecatronica.monitoramento;

import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fiap.mecatronica.monitoramento.model.Sensor;
import com.fiap.mecatronica.monitoramento.model.StatusMedicao;
import com.fiap.mecatronica.monitoramento.repository.SensorRepository;
import com.fiap.mecatronica.monitoramento.service.MedicaoService;

/**
 * Testes da integracao usada pelo app (Sprint 3). Rodam em um banco H2 em memoria,
 * separado do banco em arquivo da aplicacao: mvn test
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:monitoramento-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class MedicaoIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MedicaoService medicaoService;

    @Autowired
    private SensorRepository sensorRepository;

    // Sensor de teste com faixa 10 a 20 -> margem de alerta = 1,5
    private final Sensor sensorTeste = new Sensor("Sensor de teste", "teste", "bancada", "cm", 10.0, 20.0, true);

    @Test
    void statusSegueAFaixaDoSensor() {
        assertEquals(StatusMedicao.CRITICO, medicaoService.calcularStatus(sensorTeste, 9.9));
        assertEquals(StatusMedicao.ALERTA, medicaoService.calcularStatus(sensorTeste, 10.0));
        assertEquals(StatusMedicao.ALERTA, medicaoService.calcularStatus(sensorTeste, 11.4));
        assertEquals(StatusMedicao.NORMAL, medicaoService.calcularStatus(sensorTeste, 11.6));
        assertEquals(StatusMedicao.NORMAL, medicaoService.calcularStatus(sensorTeste, 15.0));
        assertEquals(StatusMedicao.NORMAL, medicaoService.calcularStatus(sensorTeste, 18.4));
        assertEquals(StatusMedicao.ALERTA, medicaoService.calcularStatus(sensorTeste, 18.6));
        assertEquals(StatusMedicao.ALERTA, medicaoService.calcularStatus(sensorTeste, 20.0));
        assertEquals(StatusMedicao.CRITICO, medicaoService.calcularStatus(sensorTeste, 20.1));
    }

    @Test
    void sensorSemFaixaCadastradaEhSempreNormal() {
        Sensor gps = new Sensor("GPS", "localizacao", "topo", "graus", null, null, true);
        assertEquals(StatusMedicao.NORMAL, medicaoService.calcularStatus(gps, -23.5));
    }

    @Test
    void valorSimuladoFicaDentroDaFolgaDaFaixa() {
        for (int i = 0; i < 1000; i++) {
            double valor = medicaoService.sortearValor(sensorTeste);
            assertTrue(valor >= 8.0 && valor <= 22.0, "valor fora da folga esperada: " + valor);
        }
    }

    @Test
    void simularColetaPersisteUmaLeituraPorSensorAtivoComFaixa() throws Exception {
        long esperado = sensorRepository.findByAtivoTrue().stream()
                .filter(s -> s.getLimiteMinimo() != null && s.getLimiteMaximo() != null)
                .count();

        mvc.perform(get("/api/medicoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(post("/api/medicoes/simular").header("Origin", "http://localhost:8081"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(jsonPath("$.length()").value((int) esperado))
                .andExpect(jsonPath("$[0].sensor.nome").isString())
                .andExpect(jsonPath("$[0].sensor.tipo").isString())
                .andExpect(jsonPath("$[0].sensor.unidade").isString())
                .andExpect(jsonPath("$[0].valor").isNumber())
                .andExpect(jsonPath("$[0].data").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
                .andExpect(jsonPath("$[0].status").value(in(List.of("NORMAL", "ALERTA", "CRITICO"))));

        mvc.perform(get("/api/medicoes").header("Origin", "http://localhost:8081"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(jsonPath("$.length()").value((int) esperado));
    }

    @Test
    void sensorInativoNaoParticipaDaColeta() {
        List<Sensor> ativos = sensorRepository.findByAtivoTrue();
        Sensor desligado = ativos.get(0);
        desligado.setAtivo(false);
        sensorRepository.save(desligado);

        int comFaixa = 0;
        for (Sensor sensor : ativos) {
            if (sensor != desligado && sensor.getLimiteMinimo() != null && sensor.getLimiteMaximo() != null) {
                comFaixa++;
            }
        }

        assertEquals(comFaixa, medicaoService.simularColeta().size());
    }

    @Test
    void registroManualCalculaOStatusNoServidor() throws Exception {
        Sensor ultrassonico = sensorRepository.findByTipoIgnoreCase("vegetacao").get(0); // faixa 5 a 30 cm

        mvc.perform(post("/api/medicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensorId\": " + ultrassonico.getId() + ", \"valor\": 42.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sensor.id").value(ultrassonico.getId()))
                .andExpect(jsonPath("$.valor").value(42.0))
                .andExpect(jsonPath("$.status").value("CRITICO"));

        mvc.perform(post("/api/medicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensorId\": 999999, \"valor\": 1.0}"))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/medicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sensorId\": " + ultrassonico.getId() + "}"))
                .andExpect(status().isBadRequest());
    }
}
