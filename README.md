# Monitoramento de Sensores - API REST

FIAP - Mecatronica - Sprint 1 (Backend)

---

## 1. Descricao do sistema

Este projeto e a API REST do sistema de **monitoramento de vegetacao em
rodovias** que da suporte a um **cortador de grama autonomo**. A aplicacao foi
construida em **Spring Boot** e expoe endpoints HTTP que permitem cadastrar e
consultar os sensores instalados no equipamento.

O cortador autonomo percorre trechos de rodovia e precisa saber, em tempo real,
informacoes sobre o ambiente (altura da vegetacao, umidade do solo, inclinacao
do terreno, presenca de obstaculos, etc.) para decidir quando e onde realizar o
corte da vegetacao. A primeira etapa do sistema, entregue nesta Sprint, e a
**base de dados e a API REST que representam os sensores fisicos do
equipamento**.

**Stack utilizada:**

- Java 17
- Spring Boot 3.2 (Spring Web + Spring Data JPA)
- H2 Database em modo arquivo (dados persistem apos reiniciar)
- Maven

**Arquitetura em camadas (conforme padrao da aula):**

```
src/main/java/com/fiap/mecatronica/monitoramento/
├── controller/   -> endpoints REST
├── service/      -> regras de negocio
├── repository/   -> acesso ao banco (Spring Data JPA)
└── model/        -> entidade JPA (Sensor)
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

Estes sao os sensores que o sistema ja cadastra automaticamente na primeira
execucao (cada um representa um componente real do cortador):

| Sensor                                 | Tipo         | Funcao no cortador                                            |
|----------------------------------------|--------------|---------------------------------------------------------------|
| Sensor Ultrassonico HC-SR04            | vegetacao    | Mede a altura da grama a frente do equipamento                |
| Sensor de Umidade do Solo Capacitivo   | umidade      | Detecta solo encharcado (evita atolar / danificar a area)     |
| Sensor de Inclinacao MPU6050           | inclinacao   | Mede a inclinacao do terreno para evitar tombamento           |
| Sensor de Temperatura DHT22            | temperatura  | Monitora temperatura do ambiente / componentes eletronicos    |
| Sensor LIDAR de Densidade Vegetal      | densidade    | Mede o quanto a vegetacao esta densa para regular a velocidade|
| Sensor Infravermelho de Obstaculos     | obstaculo    | Detecta pessoas, animais e objetos no caminho                 |
| Sensor GPS NEO-6M                      | localizacao  | Localizacao do cortador na rodovia (km / pista)               |
| Sensor de Crescimento NDVI             | crescimento  | Indice de vigor da vegetacao via camera multiespectral        |

---

## 3. Endpoints

Caminho base: `http://localhost:8080/api/sensores`

### CRUD completo

| Metodo | Endpoint              | Descricao                            | Resposta esperada |
|--------|-----------------------|--------------------------------------|-------------------|
| POST   | `/api/sensores`       | Cria um novo sensor                  | 201 Created       |
| GET    | `/api/sensores`       | Lista todos os sensores              | 200 OK            |
| GET    | `/api/sensores/{id}`  | Busca um sensor pelo `id`            | 200 OK / 404      |
| PUT    | `/api/sensores/{id}`  | Atualiza um sensor existente         | 200 OK / 404      |
| DELETE | `/api/sensores/{id}`  | Remove um sensor                     | 204 No Content    |

### Consultas auxiliares

| Metodo | Endpoint                       | Descricao                                      |
|--------|--------------------------------|------------------------------------------------|
| GET    | `/api/sensores/tipo/{tipo}`    | Lista sensores por tipo (ex.: `vegetacao`)     |
| GET    | `/api/sensores/ativos`         | Lista apenas sensores ativos                   |
| GET    | `/api/sensores/local/{local}`  | Busca sensores por trecho do local (parcial)   |

### Exemplo de payload (POST / PUT)

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

### Exemplos de teste no Postman

1. **POST** `http://localhost:8080/api/sensores` (com o payload acima no `Body > raw > JSON`)
2. **GET**  `http://localhost:8080/api/sensores`
3. **GET**  `http://localhost:8080/api/sensores/1`
4. **PUT**  `http://localhost:8080/api/sensores/1` (com payload atualizado)
5. **DELETE** `http://localhost:8080/api/sensores/1`

---

## Como executar

Pre-requisitos: **Java 17** e **Maven 3.8+** instalados.

```bash
mvn spring-boot:run
```

A aplicacao sobe em `http://localhost:8080`.

Console H2 (visualizar os dados): `http://localhost:8080/h2-console`
- **JDBC URL:** `jdbc:h2:file:./database/monitoramento-db`
- **User Name:** `sa`
- **Password:** (em branco)

---

## Equipe

| RM     | Nome                          |
|--------|-------------------------------|
| 563318 | Christopher Takeuti           |
| 565268 | Erick Lima                    |
| 564390 | Luiz Henrique de Almeida      |
| 564021 | Mateus Bustamante             |
| 565575 | Pedro Lopes                   |
| 562129 | Tiago Ferreira                |
