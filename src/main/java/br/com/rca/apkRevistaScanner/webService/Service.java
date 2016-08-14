package br.com.rca.apkRevistaScanner.webService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import br.com.rca.apkRevistaScanner.scanner.Scanner;

public class Service {
	public void iniciarScanner(){
		Scanner instance = Scanner.getInstance();
		if(!instance.isActive())
			instance.run();
	}
	
	public Service(){
		iniciarScanner();
	}
	
	public void salvarImagem(InputStream inputStream, String caminho){
	    try {
	    	OutputStream outputStream = new FileOutputStream(new File(caminho));
	        int read = 0;
	        byte[] bytes = new byte[1024];
	        while ((read = inputStream.read(bytes)) != -1) {
	            outputStream.write(bytes, 0, read);
	        }
	        outputStream.flush();
	        outputStream.close();
	    }
	    catch (IOException ioe) {
	        ioe.printStackTrace();
	    }
	}
}
