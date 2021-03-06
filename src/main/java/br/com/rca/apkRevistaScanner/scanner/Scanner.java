package br.com.rca.apkRevistaScanner.scanner;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
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
import br.com.rca.apkRevista.bancoDeDados.beans.Miniatura;
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
			throw new RuntimeException("O endere�o informado para pasta raiz n�o corresponde a uma pasta!");
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
								Miniatura miniatura = revista.getMiniatura();
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
									System.out.println("Gerando p�gina " + (i+1) + " de " + revista.getNPaginas());
									Image imagem = renderer.render(pdf,i,i).get(0);
									if(i==0){				
										revista.setAltura(imagem.getHeight(null));
										revista.setLargura(imagem.getWidth(null));
										miniatura.setAltura(Parametros.ALTURA_MINIATURA);
										miniatura.setLargura((Parametros.ALTURA_MINIATURA*revista.getLargura())/revista.getAltura());
										DAORevista.getInstance().persist(revista);
									}
									int nPagina = i + 1;
									gerarImagem(revista,      nPagina, imagem);									
									gerarMiniatura(miniatura, nPagina, imagem);
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
			} catch (Exception e) {
				e.printStackTrace();
				parar = true;
			}
		}
		
	}

	private void gerarMiniatura(Miniatura miniatura, int nPagina, Image imagem) throws IOException {
		Image toolkitImage     = imagem.getScaledInstance(miniatura.getLargura(), miniatura.getAltura(), Image.SCALE_AREA_AVERAGING);
		BufferedImage newImage = new BufferedImage(miniatura.getLargura(), miniatura.getAltura(),BufferedImage.TYPE_INT_RGB);
		Graphics g             = newImage.getGraphics();
		g.drawImage(toolkitImage, 0, 0, null);
		g.dispose();
		gerarImagem(miniatura,nPagina, newImage);
	}

	private void gerarImagem(Revista revista, int nPagina, Image imagem) throws IOException {
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
