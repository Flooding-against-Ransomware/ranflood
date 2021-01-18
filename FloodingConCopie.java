import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.nio.channels.FileChannel;

public class FloodingConCopie {

	public static void main(String[] args) {
		
		//Il primo parametro è il path della cartella che è stata attaccata
		File dir = new File(args[0]);
		
		FloodingConCopie.startingFunction(dir);

	}
	

	
	//Inondo la cartella di file
	public static void floodDirectory(File dir) {
		int i = 0;
		
		//Faccio solo la prima volta --> Prendo la lista dei file presenti non floodati
		List<File> fileOriginali = new ArrayList<>();
		for(File file : dir.listFiles()) {
			if(file.isFile())
				fileOriginali.add(file);
		}
		
		
		while(true) {
		
			
			/***
			 * 
			 * 
			 * Leggo tutti i file di una cartella
			 * Faccio il flooding copiando quei file
			 * Così quando il rans cripta il file originale io ho un'altra copia
			 * Se non ci sono file faccio il flooding normale
			 * 
			 * Una volta copiato il file aggiorno la mia lista e copio solamente l'ultima copia invece di copiare anche i file vecchi che possono essere stati criptati
			 * 
			 */
			if(fileOriginali.size() == 0) {
				//Non ci sono file, normale flooding
				//System.out.println("Flooding normale...");
				
				
				File floodFile = new File(dir.getAbsolutePath()+"/floodFile"+i);
				System.out.println("File creato : "+ dir.getAbsolutePath()+"/floodFile"+i+".txt");
				i++;
				//Scrivo qualcosa nel file
				try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(floodFile));
				for(int k = 0; k<50;k++) {
					//Non metto delle variabili dentro per evitare di effetturare chiamate runtime e perdere potenza
					bw.write("fhalòjkaojdshfalkfahdf9a8sdyh034nhsaklhdfjkh0234lmdsnaso");
				}
				bw.close();
				}catch(Exception e) {
					//Probabilmente non visibiel da nessuna parte
					System.out.println("Errore apertura/scrittura" + floodFile.getAbsolutePath());
				}
				
				
			}else {
				//Copio i file
				List<File> tempList = new ArrayList<>();
				for(File f : fileOriginali) {
					
					if(f.isFile()) {
						// -------- Setto il nome corretto del file senza ripetizioni
						String name = "floodFile"+f.getName()+"Copy"+i;
						if(f.getName().startsWith("floodFile")) {
							//è gia una copia
							int val = f.getName().lastIndexOf("Copy");
							//val comprende floodFile+nome
							if(val < 0) {
								//WTF
								name = "floodFile"+f.getName()+"Copy"+i;
							}else {
								name  = f.getName().substring(0,val);
								name += "Copy"+i;
							}
							
						}
						
						//----- name è il nome corretto da usare per il file
						String newFilePath = dir.getAbsolutePath()+"/"+name;
						File newFile = new File(newFilePath);
						try {
							System.out.println("nome copia " + name + " in "+f.getCanonicalPath());
						//Copio da f a newFile
							FileChannel inChannel = new FileInputStream(f).getChannel();
							FileChannel outChannel = new FileOutputStream(newFile).getChannel();
				        
					        try
					        {
					            // Try to change this but this is the number I tried.. for Windows, 64Mb - 32Kb)
					            int maxCount = (64 * 1024 * 1024) - (32 * 1024);
					            long size = inChannel.size();
					            long position = 0;
					            while ( position < size )
					            {
					               position += inChannel.transferTo( position, maxCount, outChannel );
					            }
					            System.out.println("File Successfully Copied " + newFile.getName());
					            //File Copiato correttamente
					            //Lo aggiungo ai fileOriginali e rimuovo quello vecchio
					            //cosi copiero solamente l'ultima copia per evitare di perdere tempo a copiare file
					            //che possono essere criptati
					            tempList.add(newFile);
					            
					            //Salto la rimozione tanto non lo faccio due volte e finito il primo giro resetto la lista dei File
					            //fileOriginali.remove(fileOriginali.indexOf(f));
					            
					            
					            //Fatto
					            
					        }
					        finally
					        {
					            if ( inChannel != null )
					            {
					               inChannel.close();
					            }
					            if ( outChannel != null )
					            {
					                outChannel.close();
					            }
					        }
				        }catch (Exception e) {
				        	System.out.println("Errore con file: "+newFile.getName());
				        	e.printStackTrace();
				        }
						i++;
					}
				}
				
				//Modifico fileOriginali
				fileOriginali = tempList;
				tempList = new ArrayList<>();
				
				
			}
			
			
		}
	}
	
	//Funzione di partenza usata dal main e dai figli
	public static void startingFunction(File dir) {
		File []listFile = dir.listFiles();
		for(File file : listFile) {
			if(file.isDirectory() && file.getName() != "." && file.getName() != "..") {
				//Figlio
				FloodingChild child = new FloodingChild(file);
				new Thread(child).start();
				System.out.println("Figlio creato in "+file.getName());
			}
		}
		//Ho creato tutti i figli nelle sotto cartella ora posso partire a inondare
		System.out.println("Flooding partito in " + dir.getName());
		FloodingConCopie.floodDirectory(dir);
	}

}

//figlio creato per sottocartelle
class FloodingChild implements Runnable{
	
	private File dir;
	
	public FloodingChild(File dir) {
		this.dir = dir;
	}
	
	@Override
	public void run() {
		System.out.println("Figlio run in " + dir.getName());
		FloodingConCopie.startingFunction(dir);
	}

}















//sdfg
