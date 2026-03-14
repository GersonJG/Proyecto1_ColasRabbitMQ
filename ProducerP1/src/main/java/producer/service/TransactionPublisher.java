package producer.service;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;

import producer.model.Transaccion;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TransactionPublisher {

	private Channel channel;
	private ObjectMapper mapper;
	
	public TransactionPublisher(Channel channel) {
		this.channel = channel;
		this.mapper = new ObjectMapper();
	}
	
	public void publicar(Transaccion transaccion) throws Exception{
		String bancoDestino = transaccion.getBancoDestino();
		channel.queueDeclare(bancoDestino, true, false,false, null);
		byte[] payload = mapper.writeValueAsBytes(transaccion);
		channel.basicPublish("", bancoDestino, MessageProperties.PERSISTENT_TEXT_PLAIN, payload);
		
		System.out.println("Transaccion publicada=" + transaccion.getIdTransaccion() + " se encuentra en cola= " + bancoDestino);
	}
}
