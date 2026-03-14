package producer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import producer.config.RabbitMQConfig;
import producer.service.TransactionFetcher;
import producer.service.TransactionPublisher;
import producer.model.Lote;
import producer.model.Transaccion;
import java.util.List;


public class Main {

	public static void main(String[] args) throws Exception {
		
		String host = System.getenv().getOrDefault("RABBIT_HOST", "localhost");
		int port = Integer.parseInt(System.getenv().getOrDefault("RABBIT_PORT", "5672"));
		String username = System.getenv().getOrDefault("RABBIT_USER", "guest");
		String password = System.getenv().getOrDefault("RABBIT_PASSWORD", "guest");
		String apiUrl = System.getenv().getOrDefault("API_URL", "https://hly784ig9d.execute-api.us-east-1.amazonaws.com/default/transacciones");
		
		RabbitMQConfig config = new RabbitMQConfig(host, port, username, password);
		 try (Connection connection = config.crearConexion();
		Channel channel = connection.createChannel()) {
		
		TransactionFetcher fetcher = new TransactionFetcher(apiUrl);
		TransactionPublisher publisher = new TransactionPublisher(channel);
		
		Lote lote = fetcher.obtenerLote();
		List<Transaccion> transacciones = lote.getTransacciones();
		
		for(Transaccion transaccion : transacciones) {
			publisher.publicar(transaccion);
			
		}
		
		System.out.println("Transacciones Completadas. Total publicadas: " + transacciones.size()+ "/100");
		 }

	}

}
