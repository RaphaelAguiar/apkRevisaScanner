package br.com.rca.apkRevistaScanner.webService;


import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.rca.apkRevista.bancoDeDados.beans.Cliente;
import br.com.rca.apkRevista.bancoDeDados.beans.Revista;
import br.com.rca.apkRevista.bancoDeDados.beans.enums.Status;
import br.com.rca.apkRevista.bancoDeDados.dao.DAOCliente;
import br.com.rca.apkRevista.bancoDeDados.dao.DAORevista;
import br.com.rca.apkRevista.bancoDeDados.excessoes.ClienteNaoEncontrado;
import br.com.rca.apkRevista.bancoDeDados.excessoes.RevistaNaoEncontrada;
import br.com.rca.apkRevista.bancoDeDados.excessoes.SenhaIncorreta;

@Path("/")
public class WebService extends Service{	
	
	@POST
	@Path("/enviarRevista")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public void enviarImagem(@FormDataParam("arquivo") InputStream inputStream,
							 @FormDataParam("request") String request) throws JSONException, 
																			  ClienteNaoEncontrado, 
																			  RevistaNaoEncontrada, 
																			  SenhaIncorreta
	{
		try {
			JSONObject obj        = new JSONObject(request);
			String clienteStr     = obj.getString("cliente");
			String[] clienteParam = {clienteStr};
			List<Cliente> list    = DAOCliente.getInstance().get("user = ?", clienteParam);
			if(list.isEmpty()){
				throw new ClienteNaoEncontrado(clienteStr);
			}else{
				Cliente cliente             = list.get(0);
				String  senha               = obj.getString("senha");
				if(cliente.senhaCorreta(senha)){
					String  nomeDaRevista       = obj.getString("nomeDaRevista");
					String[] paramNomeDaRevista = {nomeDaRevista};
					cliente.getRevistas("nomeDaRevista = ?", paramNomeDaRevista);
					Revista revista = new Revista(cliente, nomeDaRevista);
					salvarImagem(inputStream, revista.getFolder() + ".pdf");
					revista.setStatus(Status.AGUARDANDO_SCANNER);
					DAORevista.getInstance().persist(revista);
				}else{
					throw new SenhaIncorreta(cliente);
				}
			}
		} catch (JSONException e) {
			throw e;
		} catch (ClienteNaoEncontrado e) {
			throw e;
		} catch (RevistaNaoEncontrada e) {
			throw e;
		} catch (SenhaIncorreta e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}		
