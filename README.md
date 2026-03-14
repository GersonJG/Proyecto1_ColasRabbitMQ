#  ProducerP1 — Sistema de Procesamiento de Transacciones Bancarias

>  **Video de demostración:** 

***

##  1. Descripción General

**ProducerP1** es una aplicación Java que actúa como el **productor de mensajes** dentro de un sistema distribuido de procesamiento de transacciones bancarias. Su responsabilidad es obtener un lote de transacciones desde una API REST externa y distribuirlas en colas de **RabbitMQ** según el banco destino de cada transacción.

***

##  2. Arquitectura

```text
API Externa (GET)
       ↓
   ProducerP1
       ↓
 RabbitMQ (colas por banco)
       ↓
   ConsumerP1
       ↓
API Almacenamiento (POST)
```

***

## 3. Estructura del Proyecto

```text
ProducerP1/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── producer/
                ├── Main.java
                ├── config/
                │   └── RabbitMQConfig.java
                ├── model/
                │   ├── Lote.java
                │   ├── Transaccion.java
                │   ├── Detalle.java
                │   └── Referencias.java
                └── service/
                    ├── TransactionFetcher.java
                    └── TransactionPublisher.java
```

***

## 4. Descripción de Clases

###  `config/`

#### `RabbitMQConfig.java`
Centraliza la configuración de conexión a RabbitMQ. Lee variables de entorno con valores por defecto y expone el método `crearConexion()` que retorna una `Connection` activa hacia el broker.

***

###  `model/`

#### `Lote.java`
Modelo raíz que representa la respuesta completa del API externo. Contiene `loteId`, `fechaGeneracion` y una `List<Transaccion>` con todas las transacciones del lote.

#### `Transaccion.java`
Modelo central del sistema. Contiene `idTransaccion`, `monto`, `moneda`, `cuentaOrigen`, `bancoDestino` y un objeto `Detalle` anidado. El campo `bancoDestino` determina la cola de RabbitMQ destino.

#### `Detalle.java`
Subobjeto de `Transaccion`. Contiene `nombreBeneficiario`, `tipoTransferencia`, `descripcion` y un objeto `Referencias` anidado.

#### `Referencias.java`
Subobjeto de `Detalle`. Contiene `factura` y `codigoInterno`. Es el nodo más profundo del árbol de modelos.

***

###  `service/`

#### `TransactionFetcher.java`
Realiza el `GET` al API externo usando `HttpClient` nativo de Java 17. Deserializa la respuesta JSON en un objeto `Lote` completo usando Jackson (`ObjectMapper.readValue()`).

#### `TransactionPublisher.java`
Recibe un `Channel` de RabbitMQ por inyección de dependencias. Por cada transacción declara la cola como `durable`, serializa la transacción a `byte[]` con Jackson y la publica usando `MessageProperties.PERSISTENT_TEXT_PLAIN`.

***

## 5. Flujo de Ejecución

1. `Main` lee las variables de entorno con valores por defecto
2. `RabbitMQConfig` crea la conexión TCP con RabbitMQ
3. Se abre un `Channel` sobre la conexión dentro de un `try-with-resources`
4. `TransactionFetcher` hace `GET` al API externo de AWS Lambda
5. Jackson deserializa el JSON → objeto `Lote` con `List<Transaccion>`
6. Por cada `Transaccion` en el lote:
   - Se obtiene `bancoDestino` (`BANRURAL` / `GYT` / `BAC` / `BI`)
   - Se declara la cola como `durable` si no existe aún
   - Jackson serializa `Transaccion` → `byte[]`
   - Se publica en la cola correspondiente con `basicPublish`
7. Al terminar el `for`, se imprime el total de transacciones publicadas
8. El `try-with-resources` cierra `Channel` y `Connection` automáticamente

***

## 6. Variables de Entorno

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `RABBIT_HOST` | `localhost` | Host del servidor RabbitMQ |
| `RABBIT_PORT` | `5672` | Puerto AMQP de RabbitMQ |
| `RABBIT_USER` | `guest` | Usuario de RabbitMQ |
| `RABBIT_PASSWORD` | `guest` | Contraseña de RabbitMQ |
| `API_URL` | _(endpoint AWS Lambda)_ | URL del API externo para el GET |

***

## 7. Dependencias

| Librería | Versión | Uso |
|---|---|---|
| `amqp-client` | `5.21.0` | Conexión y manejo de colas RabbitMQ |
| `jackson-databind` | `2.17.2` | Serialización / Deserialización JSON |
| `exec-maven-plugin` | `3.5.0` | Ejecución con `mvn exec:java` |
| Java nativo `HttpClient` | Java 17 | Peticiones HTTP GET |

***




