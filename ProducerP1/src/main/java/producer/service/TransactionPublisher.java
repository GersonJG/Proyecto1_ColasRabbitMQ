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
		String colaName; 
		String status;
		if (transaccion.getMonto() > 4000) {
			colaName = "cola_rechazados";
			status = "Rechazada";
		}else {
			colaName = transaccion.getBancoDestino();
			status = "Aceptada";
		}
		
		
		channel.queueDeclare(colaName, true, false,false, null);
		byte[] payload = mapper.writeValueAsBytes(transaccion);
		channel.basicPublish("", colaName, MessageProperties.PERSISTENT_TEXT_PLAIN, payload);
		
		System.out.println("Transaccion: " + transaccion.getIdTransaccion() + " Monto: " + transaccion.getMonto() + " Cola: " + colaName + " Status: " + status);
	}
}
