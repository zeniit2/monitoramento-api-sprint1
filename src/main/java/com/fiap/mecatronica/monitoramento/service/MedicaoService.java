package com.fiap.mecatronica.monitoramento.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fiap.mecatronica.monitoramento.model.Medicao;
import com.fiap.mecatronica.monitoramento.model.Sensor;
import com.fiap.mecatronica.monitoramento.model.StatusMedicao;
import com.fiap.mecatronica.monitoramento.repository.MedicaoRepository;

/**
 * Regras de negocio das medicoes: classificacao do status e simulacao de coleta.
 *
 * Toda a "inteligencia" fica aqui, no servidor. O app apenas chama a API
 * e exibe o status que ela devolve.
 */
@Service
public class MedicaoService {

    /**
     * Fracao da faixa (limiteMaximo - limiteMinimo) considerada "perto do limite".
     * Leituras dentro dessa margem geram ALERTA.
     */
    public static final double MARGEM_ALERTA = 0.15;

    /**
     * Fracao da faixa que a simulacao pode extrapolar para fora dos limites,
     * para que aparecam leituras em ALERTA e CRITICO alem das NORMAIS.
     */
    public static final double FOLGA_SIMULACAO = 0.20;

    @Autowired
    private MedicaoRepository medicaoRepository;

    @Autowired
    private SensorService sensorService;

    public List<Medicao> listarTodas() {
        return medicaoRepository.findAllByOrderByDataDescIdDesc();
    }

    public List<Medicao> listarPorSensor(Long sensorId) {
        sensorService.buscarPorId(sensorId); // lanca RuntimeException se o sensor nao existir
        return medicaoRepository.findBySensorIdOrderByDataDescIdDesc(sensorId);
    }

    /**
     * Registra uma leitura informada manualmente (POST /api/medicoes).
     */
    public Medicao registrar(Long sensorId, Double valor) {
        Sensor sensor = sensorService.buscarPorId(sensorId);
        return salvar(sensor, valor);
    }

    /**
     * Simula uma coleta completa do cortador: uma leitura para cada sensor
     * ativo que possua faixa de operacao (limiteMinimo e limiteMaximo).
     */
    @Transactional
    public List<Medicao> simularColeta() {
        List<Medicao> coleta = new ArrayList<>();
        for (Sensor sensor : sensorService.listarAtivos()) {
            if (!possuiFaixa(sensor)) {
                continue; // ex.: GPS nao tem faixa de referencia para simular
            }
            coleta.add(salvar(sensor, sortearValor(sensor)));
        }
        return coleta;
    }

    /**
     * Classifica a leitura em relacao a faixa de operacao do sensor:
     *
     *  - CRITICO: abaixo do limite minimo ou acima do limite maximo
     *  - ALERTA : dentro da faixa, mas a menos de 15% da largura da faixa de um dos limites
     *  - NORMAL : na regiao central da faixa (ou sensor sem faixa cadastrada)
     *
     * Exemplo com faixa 10 a 20 (margem 1,5): 9 = CRITICO, 11 = ALERTA, 15 = NORMAL, 19 = ALERTA, 21 = CRITICO.
     */
    public StatusMedicao calcularStatus(Sensor sensor, double valor) {
        if (!possuiFaixa(sensor)) {
            return StatusMedicao.NORMAL;
        }

        double minimo = sensor.getLimiteMinimo();
        double maximo = sensor.getLimiteMaximo();

        if (valor < minimo || valor > maximo) {
            return StatusMedicao.CRITICO;
        }

        double margem = (maximo - minimo) * MARGEM_ALERTA;
        if (valor < minimo + margem || valor > maximo - margem) {
            return StatusMedicao.ALERTA;
        }

        return StatusMedicao.NORMAL;
    }

    /**
     * Sorteia um valor entre (minimo - 20% da faixa) e (maximo + 20% da faixa).
     * A maior parte das leituras cai dentro da faixa, mas algumas ultrapassam
     * os limites para exercitar os status ALERTA e CRITICO.
     */
    public double sortearValor(Sensor sensor) {
        double minimo = sensor.getLimiteMinimo();
        double maximo = sensor.getLimiteMaximo();
        double faixa = maximo - minimo;

        double inicio = minimo - faixa * FOLGA_SIMULACAO;
        double largura = faixa * (1 + 2 * FOLGA_SIMULACAO);
        double valor = inicio + ThreadLocalRandom.current().nextDouble() * largura;

        if (minimo >= 0) {
            valor = Math.max(0, valor); // grandeza fisica nao negativa (cm, %, indice...)
        }
        if ("%".equals(sensor.getUnidade())) {
            valor = Math.min(100, valor); // percentual nao passa de 100
        }

        return Math.round(valor * 100.0) / 100.0; // duas casas decimais
    }

    private boolean possuiFaixa(Sensor sensor) {
        return sensor.getLimiteMinimo() != null
                && sensor.getLimiteMaximo() != null
                && sensor.getLimiteMaximo() > sensor.getLimiteMinimo();
    }

    private Medicao salvar(Sensor sensor, double valor) {
        StatusMedicao status = calcularStatus(sensor, valor);
        Medicao medicao = new Medicao(sensor, valor, LocalDateTime.now().withNano(0), status);
        return medicaoRepository.save(medicao);
    }
}
