package br.com.rca.apkRevistaScanner.scanner;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.ghost4j.document.DocumentException;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.RendererException;
import org.ghost4j.renderer.SimpleRenderer;

import br.com.rca.apkRevista.Parametros;
import br.com.rca.apkRevista.bancoDeDados.beans.Cliente;
import br.com.rca.apkRevista.bancoDeDados.beans.Pagina;
import br.com.rca.apkRevista.bancoDeDados.beans.Revista;
import br.com.rca.apkRevista.bancoDeDados.beans.enums.Status;
import br.com.rca.apkRevista.bancoDeDados.dao.DAOCliente;
import br.com.rca.apkRevista.bancoDeDados.dao.DAOPagina;
import br.com.rca.apkRevista.bancoDeDados.dao.DAORevista;
import br.com.rca.apkRevista.bancoDeDados.excessoes.ClienteNaoEncontrado;
import br.com.rca.apkRevista.bancoDeDados.excessoes.RevistaNaoEncontrada;

public class Scanner{
	private static Scanner instance = Scanner.getInstance(); 
		
	private Scanner(){
		File main               = new File(Parametros.PASTA_RAIZ);
		if(!main.isDirectory()) 
			throw new RuntimeException("O endereço informado para pasta raiz não corresponde a uma pasta!");
	}
	
	public void run() {
		boolean parar = false;
		while(parar==false){
			try{
				List<Cliente> todosOsClientes     = DAOCliente.getInstance().get();
				
				if(todosOsClientes.isEmpty())
					throw new ClienteNaoEncontrado("");
				
				
				for (Cliente cliente : todosOsClientes) {
					String status[] = {Status.AGUARDANDO_SCANNER + ""};
					List<Revista> revistasNaoProcessadas;
					try {
						revistasNaoProcessadas = cliente.getRevistas("Status = ?", status);
						for (Revista revista : revistasNaoProcessadas) {
							try{
								revista.setStatus(Status.EM_PROCESSAMENTO);
								DAORevista.getInstance().persist(revista);

								File arquivo              = new File(revista.getFolder() + ".pdf");
								PDFDocument pdf           = new PDFDocument();
								
								SimpleRenderer renderer   = new SimpleRenderer();
								pdf.load(arquivo);
								renderer.setAntialiasing(SimpleRenderer.OPTION_ANTIALIASING_HIGH);
								renderer.setResolution(Parametros.RESOLUCAO_PADRAO);
								revista.setNPaginas(pdf.getPageCount());
								for (int i = 0; i < revista.getNPaginas() ; i++){
									System.out.println("Gerando página " + (i+1) + " de " + revista.getNPaginas());
									Image imagem = renderer.render(pdf,i,i).get(0);
									if(i==0){				
										revista.setLargura(imagem.getWidth(null));
										revista.setAltura(imagem.getHeight(null));							
										DAORevista.getInstance().persist(revista);
									}
									gerarImagem(revista, imagem, i + 1);
									Thread.sleep(200);
								}
								revista.setStatus(Status.DISPONIVEL);
								DAORevista.getInstance().persist(revista);
							}catch(FileNotFoundException e){
								revista.setStatus(Status.PDF_NAO_ENCONTRADO);
								DAORevista.getInstance().persist(revista);
								e.printStackTrace();
								parar = true;					
							} catch (IOException e) {
								revista.setStatus(Status.ERRO_IO);
								DAORevista.getInstance().persist(revista);								
								e.printStackTrace();
								parar = true;
							} catch (DocumentException e) {
								revista.setStatus(Status.ERRO_NO_PDF);
								DAORevista.getInstance().persist(revista);								
								e.printStackTrace();
								parar = true;
							} catch (RendererException e) {
								revista.setStatus(Status.ERRO_NO_RENDERER);
								DAORevista.getInstance().persist(revista);								
								e.printStackTrace();
								parar = true;
							}
						}
					} catch (RevistaNaoEncontrada e) {
						parar = true;
					}
				}								
			}catch(ClienteNaoEncontrado e){
				parar = true;
			} catch (Exception e1) {
				e1.printStackTrace();
				parar = true;
			}
		}
		
	}

	private void gerarImagem(Revista revista, Image imagem, int nPagina) throws IOException {
		Pagina pagina = new Pagina(revista,nPagina);
		ImageIO.write((RenderedImage) imagem,Parametros.FORMATO_PADRAO,new File(pagina.getFolder()));
		DAOPagina.getInstance().persist(pagina);
	}

	public static Scanner getInstance(){
		if(instance==null)
			instance = new Scanner();
		return instance;
	}
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner();
		while(true){
			//System.out.println("Iniciada varredura!");
			scanner.run();
			try {
				//System.out.println("Varredura completa!");
				Thread.sleep(Parametros.INTERVALO_ENTRE_VARREDURAS_DO_SCANNER*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
	}
}
