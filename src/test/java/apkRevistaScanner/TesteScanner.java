package apkRevistaScanner;

import java.util.List;

import br.com.rca.apkRevista.bancoDeDados.beans.Cliente;
import br.com.rca.apkRevista.bancoDeDados.beans.Revista;
import br.com.rca.apkRevista.bancoDeDados.beans.enums.Status;
import br.com.rca.apkRevista.bancoDeDados.dao.DAOCliente;
import br.com.rca.apkRevista.bancoDeDados.dao.DAORevista;
import br.com.rca.apkRevista.bancoDeDados.excessoes.RevistaNaoEncontrada;
import br.com.rca.apkRevistaScanner.scanner.Scanner;

public class TesteScanner {
	public static void main(String[] args) {
		try {
			//Montado cenário
			String[] paramnsCliente     = {"clienteTeste"};
			List<Cliente> list          = DAOCliente.getInstance().get("user = ?",paramnsCliente);
			Cliente clienteTeste;
			if(list.isEmpty()){
				clienteTeste = new Cliente("clienteTeste","123");
				DAOCliente.getInstance().persist(clienteTeste);
			}else{
				clienteTeste = list.get(0);
			}
			Revista revistaTeste;
			try{
				String[] paramnsRevista = {"Dominando o Android"};			
				revistaTeste = clienteTeste.getRevistas("nomeDaRevista = ?", paramnsRevista).get(0);
			}catch(RevistaNaoEncontrada e){
				revistaTeste = new Revista(clienteTeste, "revistaTeste");
			}
			revistaTeste.setStatus(Status.AGUARDANDO_SCANNER);			
			DAORevista.getInstance().persist(revistaTeste);		
			
			//Inicio do teste
			Scanner scanner = Scanner.getInstance();
			scanner.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
