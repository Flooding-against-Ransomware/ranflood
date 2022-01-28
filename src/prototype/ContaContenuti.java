/******************************************************************************
 * Copyright 2021 (C) by Loris Onori                                          *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU Library General Public License as            *
 * published by the Free Software Foundation; either version 2 of the         *
 * License, or (at your option) any later version.                            *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU Library General Public          *
 * License along with this program; if not, write to the                      *
 * Free Software Foundation, Inc.,                                            *
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                  *
 *                                                                            *
 * For details about the authors of this software, see the AUTHORS file.      *
 ******************************************************************************/

package org.ranflood.prototype;

import java.io.File;

public class ContaContenuti {

	private static String extension = ".enc";

	public static void main( String[] args ) {


		File dir = new File( args[ 0 ] );
		System.out.println( dir.getAbsolutePath() );
		if ( args.length == 2 )
			extension = args[ 1 ];


		int[] c = new int[ 2 ];
		c[ 0 ] = 0;
		c[ 1 ] = 0;

		//Conta tutti i file e tutte le directory presenti
		c = contaFiles( dir, c );
		System.out.println( "Contati Files : " + c[ 0 ] + " Dir : " + c[ 1 ] );
		System.out.println( "NF " + c[ 0 ] );
		System.out.println( "NC " + c[ 1 ] );

		//Conta tutti i file criptati (Quelli che finiscono con .enc)
		long encryptedFiles = countEncryptedFiles( dir );
		System.out.println( "File criptati (sia quelli con di flooding che quelli originali)" + encryptedFiles + " +/- 1" );
		System.out.println( "FC " + encryptedFiles );

		//Conta i file di flooding criptati (Quelli che finiscono con .enc e inizioano son floodFile)
		long encryptedFilesFlood = countEncryptedFilesFlooding( dir );
		System.out.println( "File di flooding criptati " + encryptedFilesFlood + " +/- 1" );
		System.out.println( "FFC " + encryptedFilesFlood );

		//Conta la dimensione di tutti i file in kB
		long totalDim = totalDimension( dir );
		System.out.println( "Dimensione totale file : " + totalDim );
		System.out.println( "DT " + totalDim );


		//Conta tutti i file del sistema non criptati (Quelli che NON finiscono con .enc e che NON iniziano con floodFile)
		long totalNonEncryptedNonFloodingFiles = countNonEncryptedNonFloodingFiles( dir );
		System.out.println( "File originali del sistema non criptati (non sono contati i file di flooding)" + totalNonEncryptedNonFloodingFiles + "\n" );
		System.out.println( "FONC " + totalNonEncryptedNonFloodingFiles );


		//Conto tutti i file di floodingCreatiCopie di quelli originali

		long[] copie = new long[ 2 ];
		copie[ 0 ] = 0;
		copie[ 1 ] = 0;

		copie = contaCopieTotali( dir, copie );

		long fileCopieTotaliCreati = copie[ 0 ];
		System.out.println( "File copie totali create (Considero anche quelle criptate) " + fileCopieTotaliCreati );
		System.out.println( "CC " + fileCopieTotaliCreati );

		//Conto tutti i file di flooding di copia di quelli originali non criptati
		//Questi file svolgono una doppia funzione :
		//Effettuano il flooding per rallentare il ransomware
		//Fanno da backup per i file
		//Anche se quelli originali vengono criptati ci sono questi che fanno da backup
		long fileCopieDiQuelliOriginaliNonCriptati = copie[ 1 ];
		System.out.println( "File copie di quelli orginali non criptati " + fileCopieDiQuelliOriginaliNonCriptati );
		System.out.println( "CNC " + fileCopieDiQuelliOriginaliNonCriptati );

		//Guardo se per ogni file originale criptato (non floodFile, non copiato, e criptato)
		//ho una copia di quel file che ancora non è stata criptata
		//Questo verifica mi serve per vedere se facendo così posso fermare il ransomware 
		//Lo rallento e salvo i file 
		long fileSalvatiGrazieAlFloodingConCopie = salvatiConFloodingCopie( dir );
		System.out.println( "Dati tutti i file di cui sopra, dati tutti i file criptati questo è il numero di file che sono stati salvati grazie a questa tecnica " + fileSalvatiGrazieAlFloodingConCopie );
		System.out.println( "FSC " + fileSalvatiGrazieAlFloodingConCopie );

		/* ----- Questi due metodi sono complementari, il primo mi dice quanti file sono stati salvati e il secondo quanti file ho perso---- */
		/* ----- Per ora implemento entrambi i metodi poi se vedo che questi combaciano allora posso implentarne solo uno ------- */
		//Per ogni file controllo se questo non è stato criptato
		//Controllo se il file orginale non è criptato
		//Se non criptatto
		//Autmento contatore
		//Se criptato
		//Controllo se c'è un file copia non criptato

		//Facendo questo vedo se quanto questa tecnica mi permette di rallentare/risolvere il ransomware
		//Così guardo quanti file ho salvato

		long fileSalvati = fileSalvati( dir );
		System.out.println( "File salvati : Prendo in considerazione i file non criptati o file copie di quelli orginali non criptati " + fileSalvati );
		System.out.println( "FS " + fileSalvati );
		//Devo controllare quanti file ho perso completamente
		//Per ogni file origianle che leggo vedo se questo è stato criptato
		//Se criptato controllo se ho una copia non criptata
		//Se il file criptato non ha una copia non criptata allora il file è perso
		//Aumento il contatore
		long filePersi = filePersi( dir );
		System.out.println( "File persi : Per ogni file originale criptato controllo se non ho una copia non criptata in quel caso aumento il contatore: " + filePersi );
		System.out.println( "FP " + filePersi );

		System.out.println( "File originali che non sono stati criptati e quindi salvi non grazie alla copia ma  forse solamente grazie al flooding o bho : fileSalvati - fileSalvatiGrazieAQuestaTecnica :" + ( fileSalvati - fileSalvatiGrazieAlFloodingConCopie ) );
		System.out.println( "FSSC " + ( fileSalvati - fileSalvatiGrazieAlFloodingConCopie ) );

	}

	public static long filePersi( File dir ) {
		File[] fileList = dir.listFiles();
		long result = 0;
		for ( File file : fileList ) {
			if ( file.isDirectory() && file.getName() != "." && file.getName() != ".." ) {
				result += filePersi( file );
			} else {
				//Controllo che non sia di flooding e che sia criptato, il controllo sulla copia può anche non essere fatto perchè se sono copie comunque iniziano con floodFile però vabè
				if ( !file.getName().startsWith( "floodFile" ) && file.getName().endsWith( ".enc" ) && file.getName().lastIndexOf( "Copy" ) < 0 ) {
					//File originale criptato
					//Controllo se effettivamente ho perso quel file
					//Controllo se nella lista dei file non ho nessuna copia disponibile
					boolean presente = false;
					for ( File f : fileList ) {
						//Controllo che la copia non abbia .enc all'interno quindi non sia criptata o che non sia una copia del file criptato
						//Prendo solamente il nome originale del file e non l'estensione .enc che indica che è stato criptato
						String nome = file.getName().substring( 0, file.getName().length() - 4 );
						if ( f.getName().startsWith( "floodFile" ) && f.getName().lastIndexOf( nome ) >= 0 && f.getName().lastIndexOf( ".enc" ) < 0 && f.getName().lastIndexOf( "Copy" ) >= 0 ) {
							presente = true;
						}
					}
					if ( !presente )
						result += 1;
				}
			}
		}
		return result;
	}

	public static long fileSalvati( File dir ) {
		File[] fileList = dir.listFiles();
		long result = 0;
		for ( File file : fileList ) {
			if ( file.isDirectory() && file.getName() != "." && file.getName() != ".." ) {
				result += fileSalvati( file );
			} else {
				if ( !file.getName().startsWith( "floodFile" ) && file.getName().lastIndexOf( ".enc" ) < 0 ) {
					//Non criptato
					result += 1;
				} else if ( !file.getName().startsWith( "floodFile" ) && file.getName().endsWith( ".enc" ) ) {
					//File criptato 
					//Controllo se ho un file copia non criptato
					for ( File f : fileList ) {
						String name = file.getName().substring( 0, file.getName().length() - 4 );
						if ( f.getName().startsWith( "floodFile" ) && f.getName().lastIndexOf( ".enc" ) < 0 && f.getName().lastIndexOf( name ) >= 0 && f.getName().lastIndexOf( "Copy" ) >= 0 ) {
							//Trovato file flooddato di copia non criptato
							result += 1;
							break;
							//Metto il break così da contare solo la prima copia valida
							//Non conto più copie perchè inutile e mi sfalsa la misurazione
						}
					}
				}
			}
		}
		return result;
	}

	public static long[] contaCopieTotali( File dir, long[] copie ) {
		//Funzione per contare tutte le copie del flooding che sono state create
		//Ritorno sia quelle totali che quelle criptate

		//copie[0] = copie totali

		//copie[1] = copie di quelli originali non criptate

		File[] fileList = dir.listFiles();
		for ( File file : fileList ) {
			if ( file.isDirectory() && file.getName() != "." && file.getName() != ".." ) {
				contaCopieTotali( file, copie );
			} else if ( file.getName().startsWith( "floodFile" ) && file.getName().lastIndexOf( "Copy" ) > 0 ) {
				if ( file.getName().lastIndexOf( ".enc" ) < 0 ) {
					copie[ 1 ] += 1;
				}
				copie[ 0 ] += 1;
			}

		}
		return copie;

	}

	public static long salvatiConFloodingCopie( File dir ) {
		/**
		 * Per ogni file all'interno della cartella controllo se questo è un file originale criptato = NOME.enc
		 * NON deve contenere floodFile all'interno del nome e deve terminare in .enc
		 * Si suppone che non esistano file che si chiamino floodFileXXX
		 *
		 * Se incontro un file di questo tipo controllo se in questa cartella c'è un file che si chiama floodFileNOMECopy
		 * In quel caso incremento il contatore
		 *
		 * Questo significa che:
		 * Il file originale è stato criptato
		 * Grazie a questa tipologia di flooding sono riuscito a creare una copia che non è stata criptata
		 *
		 */
		File[] fileList = dir.listFiles();
		long counter = 0;
		for ( File file : fileList ) {
			if ( file.isDirectory() && file.getName() != "." && file.getName() != ".." ) {
				counter += salvatiConFloodingCopie( file );
			} else {
				if ( file.getName().endsWith( ".enc" ) && !file.getName().startsWith( "floodFile" ) ) {
					//File originale criptato

					//Controllo se c'è un file copia non criptato
					for ( File f : fileList ) {
						//Se lastIndex di .enc è = -1 allora non c'è .enc quindi non è stato criptato ; Controllo se contiene anche il valore Copy ; infine controllo che sia veramente il file di cui sopra
						if ( f.isFile() && f.getName().startsWith( "floodFile" ) && f.getName().lastIndexOf( ".enc" ) < 0 && f.getName().lastIndexOf( "Copy" ) >= 0 && f.getName().lastIndexOf( file.getName().substring( 0, file.getName().length() - 4 ) ) > 0 ) {
							counter++;
							//Metto un break per evitare di contare due copie non criptate
							//Mi interessa sapere se ce n'è una sola
							break;
						}
					}

				}
			}
		}
		return counter;
	}

	public static long totalDimension( File dir ) {
		File[] fileList = dir.listFiles();
		long dim = 0;
		for ( File file : fileList ) {
			if ( file.isDirectory() && file.getName() != "." && file.getName() != ".." ) {
				dim += totalDimension( file );
			} else {
				dim += file.length() / 1024;
			}
		}
		return dim;
	}

	public static int[] contaFiles( File dir, int[] c ) {
		File[] fileList = dir.listFiles();
		for ( File file : fileList ) {
			if ( file.isDirectory() && file.getName() != "." && file.getName() != ".." ) {
				c[ 1 ] += 1;
				contaFiles( file, c );
			} else {
				c[ 0 ] += 1;
			}
		}
		return c;
	}

	public static long countEncryptedFiles( File dir ) {
		File[] fileList = dir.listFiles();
		long count = 0;
		for ( File file : fileList ) {
			if ( file.isDirectory() && file.getName() != "." && file.getName() != ".." ) {
				count += countEncryptedFiles( file );
			} else {
				if ( file.getName().endsWith( ".enc" ) ) {
					count++;
				}
			}
		}
		return count;
	}

	public static long countEncryptedFilesFlooding( File dir ) {
		File[] fileList = dir.listFiles();
		long count = 0;
		for ( File file : fileList ) {
			if ( file.isDirectory() && file.getName() != "." && file.getName() != ".." ) {
				count += countEncryptedFilesFlooding( file );
			} else {
				if ( file.getName().endsWith( ".enc" ) && file.getName().startsWith( "floodFile" ) ) {
					count++;
				}
			}
		}
		return count;
	}

	public static long countNonEncryptedNonFloodingFiles( File dir ) {
		File[] fileList = dir.listFiles();
		long count = 0;
		for ( File file : fileList ) {
			if ( file.isDirectory() && file.getName() != "." && file.getName() != ".." ) {
				count += countNonEncryptedNonFloodingFiles( file );
			} else {
				if ( !file.getName().endsWith( ".enc" ) && !file.getName().startsWith( "floodFile" ) ) {
					count++;
				}
			}
		}
		return count;
	}


}


//