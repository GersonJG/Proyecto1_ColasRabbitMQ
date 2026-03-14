package producer.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import producer.model.Lote;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TransactionFetcher {

	private String apiUrl;
	private HttpClient httpClient;
	private ObjectMapper mapper;
	
	public TransactionFetcher(String apiUrl) {
		this.apiUrl = apiUrl;
		this.httpClient = HttpClient.newHttpClient();
		this.mapper = new ObjectMapper();
	}
	
	public Lote obtenerLote() throws Exception{
		HttpRequest request = HttpRequest.newBuilder()
		.uri(URI.create(this.apiUrl))
		.GET()
		.build();
		
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		
		return mapper.readValue(response.body(), Lote.class);
	}
	
}
