package consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import consumer.model.Transaccion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TransactionConsumer {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String postUrl;

    public TransactionConsumer(String postUrl) {
        this.postUrl = postUrl;
    }

    public void startConsuming(Channel channel) throws Exception {

        String[] colas = {"BANRURAL", "GYT", "BAC", "BI"};

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String json = new String(delivery.getBody());
            try {
                Transaccion t = mapper.readValue(json, Transaccion.class);
                t.setNombre("Gerson Leonel Jimenez Gonzalez");
                t.setCarnet("0905-24-7000");
                enviarPost(t);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                System.err.println("Error procesando mensaje: " + e.getMessage());
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
        };

        for (String cola : colas) {
            channel.queueDeclare(cola, true, false, false, null);
            channel.basicConsume(cola, false, deliverCallback, tag -> {});
        }

        System.out.println("Esperando mensajes de las colas...");
        Thread.currentThread().join();
    }

    private void enviarPost(Transaccion t) throws Exception {
        String json = mapper.writeValueAsString(t);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.postUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Respuesta POST: " + response.statusCode() + " - " + response.body());
    }
}
