package consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import consumer.config.RabbitMQConfig;
import consumer.service.TransactionConsumer;

public class Main {

    public static void main(String[] args) throws Exception {

        String host = System.getenv().getOrDefault("RABBIT_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("RABBIT_PORT", "5672"));
        String username = System.getenv().getOrDefault("RABBIT_USER", "guest");
        String password = System.getenv().getOrDefault("RABBIT_PASSWORD", "guest");
        String postUrl = System.getenv().getOrDefault("POST_URL", 
            "https://7e0d9ogwzd.execute-api.us-east-1.amazonaws.com/default/guardarTransacciones");

        RabbitMQConfig config = new RabbitMQConfig(host, port, username, password);

        try (Connection connection = config.crearConexion();
             Channel channel = connection.createChannel()) {

            TransactionConsumer consumer = new TransactionConsumer(postUrl);
            consumer.startConsuming(channel);
        }
    }
}
