# Monitoramento de Sensores - API REST

FIAP - Mecatronica - Sprint 1 (Backend) e Sprint 3 (Integracao com o app mobile)

---

## 1. Descricao do sistema

Este projeto e a API REST do sistema de **monitoramento de vegetacao em
rodovias** que da suporte a um **cortador de grama autonomo** (projeto
VerdeSmart). A aplicacao foi construida em **Spring Boot** e expoe endpoints
HTTP para cadastrar os sensores instalados no equipamento e registrar as
leituras (medicoes) que eles produzem.

O cortador autonomo percorre trechos de rodovia e precisa saber, em tempo real,
informacoes sobre o ambiente (altura da vegetacao, umidade do solo, inclinacao
do terreno, presenca de obstaculos, etc.) para decidir quando e onde realizar o
corte da vegetacao.

- **Sprint 1:** base de dados e CRUD REST dos sensores fisicos do equipamento.
- **Sprint 3:** medicoes com status calculado no servidor, endpoint de simulacao
  de coleta e liberacao de CORS, tudo consumido pelo app React Native (Expo).

Repositorio do app mobile: https://github.com/zeniit2/app-sprint1-hete

**Stack utilizada:**

- Java 17
- Spring Boot 3.2 (Spring Web + Spring Data JPA)
- H2 Database em modo arquivo (dados persistem apos reiniciar)
- Maven

**Arquitetura em camadas (conforme padrao da aula):**

```
src/main/java/com/fiap/mecatronica/monitoramento/
├── controller/   -> endpoints REST (SensorController, MedicaoController) com @CrossOrigin
├── service/      -> regras de negocio (SensorService, MedicaoService: status e simulacao)
├── repository/   -> acesso ao banco (Spring Data JPA)
├── model/        -> entidades JPA (Sensor, Medicao) e o enum StatusMedicao
├── dto/          -> MedicaoRequest (corpo do POST /api/medicoes)
└── config/       -> DataInitializer (cadastra os sensores na primeira execucao)
```

---

## 2. O que e o Sensor no contexto do projeto

Um **Sensor** representa um **componente fisico real** instalado no cortador de
grama autonomo. Cada sensor coleta um tipo especifico de informacao do
ambiente em que o equipamento opera e possui limites operacionais que definem
o que e considerado uma leitura normal.

A entidade `Sensor` foi modelada com os seguintes campos:

| Campo          | Tipo    | Descricao                                                      |
|----------------|---------|----------------------------------------------------------------|
| `id`           | Long    | Identificador unico (gerado automaticamente)                   |
| `nome`         | String  | Nome / modelo do sensor (ex.: "Sensor Ultrassonico HC-SR04")   |
| `tipo`         | String  | Tipo da grandeza medida (vegetacao, umidade, crescimento, ...) |
| `local`        | String  | Onde o sensor esta instalado no cortador                       |
| `unidade`      | String  | Unidade da medida (cm, %, C, graus, indice)                    |
| `limiteMinimo` | Double  | Valor minimo aceitavel para a leitura                          |
| `limiteMaximo` | Double  | Valor maximo aceitavel para a leitura                          |
| `ativo`        | Boolean | Indica se o sensor esta operacional                            |

### Sensores aplicaveis ao cortador autonomo

Estes sao os sensores que o sistema cadastra automaticamente na primeira
execucao (cada um representa um componente real do cortador):

| Sensor                                 | Tipo         | Faixa        | Funcao no cortador                                            |
|----------------------------------------|--------------|--------------|---------------------------------------------------------------|
| Sensor Ultrassonico HC-SR04            | vegetacao    | 5 a 30 cm    | Mede a altura da grama a frente do equipamento                |
| Sensor de Umidade do Solo Capacitivo   | umidade      | 20 a 80 %    | Detecta solo encharcado (evita atolar / danificar a area)     |
| Sensor de Inclinacao MPU6050           | inclinacao   | 0 a 25 graus | Mede a inclinacao do terreno para evitar tombamento           |
| Sensor de Temperatura DHT22            | temperatura  | 5 a 45 C     | Monitora temperatura do ambiente / componentes eletronicos    |
| Sensor LIDAR de Densidade Vegetal      | densidade    | 10 a 100 %   | Mede o quanto a vegetacao esta densa para regular a velocidade|
| Sensor Infravermelho de Obstaculos     | obstaculo    | 10 a 200 cm  | Detecta pessoas, animais e objetos no caminho                 |
| Sensor GPS NEO-6M                      | localizacao  | (sem faixa)  | Localizacao do cortador na rodovia (km / pista)               |
| Sensor de Crescimento NDVI             | crescimento  | 0,2 a 0,8    | Indice de vigor da vegetacao via camera multiespectral        |

---

## 3. Medicao e status (Sprint 3)

Uma **Medicao** e uma leitura produzida por um sensor. E a entidade que o app
lista na tela.

| Campo    | Tipo                        | Descricao                                                    |
|----------|-----------------------------|--------------------------------------------------------------|
| `id`     | Long                        | Identificador unico                                          |
| `sensor` | Sensor                      | Sensor que produziu a leitura (vai completo no JSON)         |
| `valor`  | Double                      | Valor lido, na unidade do sensor                             |
| `data`   | String (ISO 8601)           | Momento da coleta, ex.: `2026-09-21T21:40:00`                |
| `status` | NORMAL / ALERTA / CRITICO   | Classificacao calculada pelo servidor no momento do registro |

### Regra de status (inteligencia no servidor)

O status e calculado em `MedicaoService.calcularStatus` a partir da faixa de
operacao do proprio sensor (`limiteMinimo` e `limiteMaximo`):

- **CRITICO**: valor abaixo do limite minimo ou acima do limite maximo
- **ALERTA**: dentro da faixa, mas a menos de 15% da largura da faixa de um dos limites
- **NORMAL**: regiao central da faixa

Exemplo com o Sensor Ultrassonico (faixa 5 a 30 cm, margem de alerta 3,75 cm):
3 cm = CRITICO, 7 cm = ALERTA, 18 cm = NORMAL, 28 cm = ALERTA, 32 cm = CRITICO.

Sensores sem faixa cadastrada (GPS) ficam sempre NORMAL e nao participam da
simulacao. O status e gravado junto com a medicao; o app **nao recalcula**,
apenas exibe o que a API devolve.

### Simulacao de coleta

`POST /api/medicoes/simular` gera **uma leitura para cada sensor ativo com
faixa cadastrada** (7 dos 8 sensores iniciais; o GPS, sem faixa, fica de fora).
O valor e sorteado entre (minimo - 20% da largura da faixa) e (maximo + 20% da
largura da faixa). Exemplo: faixa 5 a 30 cm tem largura 25, entao o sorteio vai
de 0 a 35 cm. Assim aparecem os tres status; grandezas nao negativas nao ficam
abaixo de 0 e percentuais nao passam de 100. Tudo e persistido no H2, ou seja,
o historico sobrevive a reinicios.

---

## 4. Endpoints

Todos os endpoints respondem JSON em `http://localhost:8080`.

### Sensores (Sprint 1) - `/api/sensores`

| Metodo | Endpoint                       | Descricao                                    | Resposta esperada |
|--------|--------------------------------|----------------------------------------------|-------------------|
| POST   | `/api/sensores`                | Cria um novo sensor                          | 201 Created       |
| GET    | `/api/sensores`                | Lista todos os sensores                      | 200 OK            |
| GET    | `/api/sensores/{id}`           | Busca um sensor pelo `id`                    | 200 OK / 404      |
| PUT    | `/api/sensores/{id}`           | Atualiza um sensor existente                 | 200 OK / 404      |
| DELETE | `/api/sensores/{id}`           | Remove um sensor                             | 204 No Content / 404 |
| GET    | `/api/sensores/tipo/{tipo}`    | Lista sensores por tipo (ex.: `vegetacao`)   | 200 OK            |
| GET    | `/api/sensores/ativos`         | Lista apenas sensores ativos                 | 200 OK            |
| GET    | `/api/sensores/local/{local}`  | Busca sensores por trecho do local (parcial) | 200 OK            |

Exemplo de payload (POST / PUT):

```json
{
  "nome": "Sensor Ultrassonico HC-SR04",
  "tipo": "vegetacao",
  "local": "Frente do cortador - chassi inferior",
  "unidade": "cm",
  "limiteMinimo": 5.0,
  "limiteMaximo": 30.0,
  "ativo": true
}
```

### Medicoes (Sprint 3) - `/api/medicoes`

| Metodo | Endpoint                          | Descricao                                                             | Resposta esperada |
|--------|-----------------------------------|-----------------------------------------------------------------------|-------------------|
| GET    | `/api/medicoes`                   | Historico completo, mais recente primeiro (app: ao abrir e atualizar) | 200 OK            |
| POST   | `/api/medicoes/simular`           | Simula uma coleta: uma leitura por sensor ativo com faixa (botao do app) | 201 Created    |
| POST   | `/api/medicoes`                   | Registra uma leitura manual `{ "sensorId": 1, "valor": 18.5 }`        | 201 / 400 / 404   |
| GET    | `/api/medicoes/sensor/{sensorId}` | Historico de um sensor                                                | 200 OK / 404      |

Exemplo de resposta de `GET /api/medicoes`:

```json
[
  {
    "id": 12,
    "sensor": {
      "id": 1,
      "nome": "Sensor Ultrassonico HC-SR04",
      "tipo": "vegetacao",
      "local": "Frente do cortador - chassi inferior",
      "unidade": "cm",
      "limiteMinimo": 5.0,
      "limiteMaximo": 30.0,
      "ativo": true
    },
    "valor": 27.4,
    "data": "2026-09-21T21:40:00",
    "status": "ALERTA"
  }
]
```

### CORS

`SensorController` e `MedicaoController` tem `@CrossOrigin(origins = "*")`.
O app Expo roda em outra origem (navegador na porta 8081, emulador Android ou
celular) e sem essa liberacao o navegador bloqueia as chamadas para a API.

---

## 5. Integracao com o app (Sprint 3)

1. Suba esta API (secao "Como executar") e confirme que o JSON responde:
   `curl http://localhost:8080/api/medicoes`
2. Suba o app com `npx expo start` no repositorio do frontend.

Endereco da API (`BASE_URL`) que o app usa em cada ambiente:

| Onde o app roda                 | BASE_URL                       |
|---------------------------------|--------------------------------|
| Navegador (web) / simulador iOS | `http://localhost:8080`        |
| Emulador Android                | `http://10.0.2.2:8080`         |
| Celular fisico (Expo Go)        | `http://IP_DA_MAQUINA:8080`    |

O app detecta o ambiente sozinho; para forcar um endereco use a variavel
`EXPO_PUBLIC_API_URL` (detalhes no README do frontend). No celular fisico o
computador e o celular precisam estar na mesma rede Wi-Fi e o firewall do
Windows precisa liberar a porta 8080 para o `java.exe`.

- **Botao "Simular coleta"** do app: faz `POST /api/medicoes/simular`, o servidor
  gera e grava uma leitura por sensor ativo com faixa cadastrada (o GPS fica de
  fora), e o app refaz o `GET /api/medicoes`.
- **Backend parado**: o app mostra a mensagem "Nao foi possivel conectar ao
  backend em http://...:8080" com o botao **Tentar novamente**; nenhuma medicao
  e inventada no celular.

---

## Como executar

Pre-requisitos: **Java 17** e **Maven 3.8+** instalados.

```bash
mvn spring-boot:run
```

A aplicacao sobe em `http://localhost:8080`. Antes de abrir o app, confirme o
retorno em JSON:

```bash
curl http://localhost:8080/api/sensores
curl http://localhost:8080/api/medicoes
curl -X POST http://localhost:8080/api/medicoes/simular
```

Console H2 (visualizar os dados): `http://localhost:8080/h2-console`
- **JDBC URL:** `jdbc:h2:file:./database/monitoramento-db`
- **User Name:** `sa`
- **Password:** (em branco)

O banco fica na pasta `database/` (ignorada pelo git) e e criado na primeira
execucao. Se `GET /api/medicoes` responder 500 com erro de coluna, e porque a
pasta `database/` guarda uma tabela `medicoes` de outra versao do projeto:
pare a API, apague a pasta `database/` (ou rode `DROP TABLE MEDICOES` no
console H2) e suba de novo; os sensores iniciais sao recadastrados sozinhos.

Testes automatizados (regra de status, simulacao, persistencia, CORS e
validacoes do POST), em um banco H2 em memoria separado do banco da aplicacao:

```bash
mvn test
```

---

## Equipe

| RM     | Nome                          |
|--------|-------------------------------|
| 563318 | Christopher Takeuti           |
| 565268 | Erick Lima                    |
| 564390 | Luiz Henrique de Almeida      |
| 564021 | Mateus Bustamante             |
| 565575 | Pedro Lopes                   |
