package producer.config;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;

public class RabbitMQConfig {
	private String host;
	private int port;
	private String username;
	private String password;
	
	public RabbitMQConfig (String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}
	
	public Connection crearConexion() throws Exception{
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(this.host);
		factory.setPort(this.port);
		factory.setUsername(this.username);
		factory.setPassword(this.password);
		return factory.newConnection();
	}
}
