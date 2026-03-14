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

#  ConsumerP1 


## Descripción General

**ConsumerP1** es una aplicación Java que actúa como el **consumidor de mensajes** dentro de un sistema distribuido de procesamiento de transacciones bancarias. Su responsabilidad es leer las transacciones desde las colas de **RabbitMQ**, agregar los datos del estudiante (`nombre`, `carnet`) y enviarlas mediante una petición **HTTP POST** a un API de almacenamiento.

---

## 2. Arquitectura

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

---

## 3. Estructura del Proyecto

```text
ConsumerP1/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── consumer/
                ├── Main.java
                ├── config/
                │   └── RabbitMQConfig.java
                ├── model/
                │   ├── Transaccion.java
                │   ├── Detalle.java
                │   └── Referencias.java
                └── service/
                    └── TransactionConsumer.java
```

---

## 4. Descripción de Clases

### `config/`

#### `RabbitMQConfig.java`
Centraliza la configuración de conexión a RabbitMQ para el Consumer. Recibe `host`, `port`, `username` y `password` en el constructor y expone el método `crearConexion()` que retorna una `Connection` activa hacia el broker.

---

### `model/`

#### `Transaccion.java`
Modelo principal del Consumer. Es similar a la transacción del Producer pero agrega dos campos extra:

- `nombre` (nombre del estudiante)  
- `carnet` (carnet del estudiante)  

Mantiene además:

- `idTransaccion`  
- `monto`  
- `moneda`  
- `cuentaOrigen`  
- `bancoDestino`  
- `detalle` (`Detalle`)  

Esto permite deserializar correctamente el JSON que llega desde RabbitMQ y reenviarlo con la información adicional del estudiante.

#### `Detalle.java`
Idéntico al del Producer. Contiene:

- `nombreBeneficiario`  
- `tipoTransferencia`  
- `descripcion`  
- `referencias` (`Referencias`)  

#### `Referencias.java`
Idéntico al del Producer. Contiene:

- `factura`  
- `codigoInterno`  

Estas clases sirven para que Jackson pueda reconstruir el árbol completo de objetos a partir del JSON recibido.

---

### `service/`

#### `TransactionConsumer.java`

Responsabilidades principales:

- Recibir un `Channel` de RabbitMQ (inyectado desde `Main`).  
- Declarar y escuchar las colas de los bancos (`BANRURAL`, `GYT`, `BAC`, `BI`).  
- Definir un `DeliverCallback` (lambda) que:
  - Convierte el `byte[]` del mensaje en `String` JSON.  
  - Deserializa el JSON a un objeto `Transaccion` con Jackson.  
  - Asigna `nombre = "Gerson Leonel Jimenez Gonzalez"` y `carnet = "0905-24-7000"`.  
  - Llama a `enviarPost(t)` para enviar la transacción al API de almacenamiento.  
  - Si el POST es exitoso, llama a `basicAck()` para confirmar el mensaje.  
  - Si hay error, llama a `basicNack()` con `requeue = true` para reencolar el mensaje.  

Además:

- Usa un `HttpClient` compartido (`HttpClient.newHttpClient()`) para todas las peticiones POST.  
- Usa `ObjectMapper` de Jackson para serializar y deserializar JSON.  
- Mantiene viva la aplicación con `Thread.currentThread().join()` para seguir escuchando mensajes indefinidamente.

---

## 5. Flujo de Ejecución

1. `Main` lee las variables de entorno con valores por defecto (`RABBIT_HOST`, `RABBIT_PORT`, `RABBIT_USER`, `RABBIT_PASSWORD`, `POST_URL`).  
2. `RabbitMQConfig` crea la conexión TCP con RabbitMQ mediante `crearConexion()`.  
3. Se abre un `Channel` sobre la conexión dentro de un bloque `try-with-resources`.  
4. Se instancia `TransactionConsumer` pasando `postUrl` en el constructor.  
5. Se llama a `consumer.startConsuming(channel)`:
   - Se declaran las colas `BANRURAL`, `GYT`, `BAC`, `BI` como `durable`.  
   - Se suscribe a cada cola usando `basicConsume(cola, autoAck = false, deliverCallback, ...)`.  
   - Se imprime en consola que el Consumer está esperando mensajes.  
6. Cuando llega un mensaje a cualquier cola:
   - El `DeliverCallback` se ejecuta automáticamente.  
   - Obtiene el cuerpo del mensaje (`delivery.getBody()`), lo convierte a `String` JSON.  
   - Jackson deserializa el JSON a `Transaccion`.  
   - Se asignan `nombre` y `carnet` del estudiante.  
   - `enviarPost(t)` construye una petición `HttpRequest` con método `POST` hacia `POST_URL`, con header `Content-Type: application/json`.  
   - El `HttpClient` envía la petición y recibe un `HttpResponse<String>`.  
   - Si el `statusCode` es `201`, se imprime la respuesta y se llama `basicAck()` para confirmar el mensaje.  
   - Si ocurre una excepción o un código de error, se imprime el error y se llama `basicNack()` con `requeue = true` para reencolar el mensaje.  
7. `Thread.currentThread().join()` mantiene el hilo principal bloqueado para que la aplicación no termine.  
8. Al salir del bloque `try-with-resources`, `Channel` y `Connection` se cierran automáticamente.

---

## 6. Variables de Entorno

| Variable          | Valor por defecto       | Descripción                                       |
|-------------------|-------------------------|---------------------------------------------------|
| `RABBIT_HOST`     | `localhost`             | Host del servidor RabbitMQ                        |
| `RABBIT_PORT`     | `5672`                  | Puerto AMQP de RabbitMQ                           |
| `RABBIT_USER`     | `guest`                 | Usuario de RabbitMQ                               |
| `RABBIT_PASSWORD` | `guest`                 | Contraseña de RabbitMQ                            |
| `POST_URL`        | _(endpoint AWS Lambda)_ | URL del API de almacenamiento para el POST        |

---

## 7. Dependencias

| Librería            | Versión  | Uso                                          |
|---------------------|----------|----------------------------------------------|
| `amqp-client`       | `5.21.0` | Conexión y manejo de colas RabbitMQ         |
| `jackson-databind`  | `2.17.2` | Serialización / Deserialización JSON        |
| `exec-maven-plugin` | `3.5.0`  | Ejecución con `mvn exec:java`              |
| Java nativo `HttpClient` | Java 17 | Peticiones HTTP POST                      |

---





