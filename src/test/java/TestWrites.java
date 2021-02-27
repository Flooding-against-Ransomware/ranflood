import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

public class TestWrites {

	public static void main( String[] args ) {
		String root =  "/Users/thesave/Desktop/testQuickCheck/test_folder/files/";
		long start = System.currentTimeMillis();
		int file_number = 1000;
		IntStream.range( 0, file_number )
						.parallel()
						.forEach( i -> {
							try {
								FileOutputStream f = new FileOutputStream( root + String.valueOf( i ) + ".txt" );
								byte[] b = new byte[ new Random().nextInt( Double.valueOf( Math.pow( 2, 22 ) ).intValue() ) + Double.valueOf( Math.pow( 2, 7 ) ).intValue() ];
								new Random().nextBytes( b );
								f.write( b );
								f.close();
							} catch ( IOException e ) {
								e.printStackTrace();
							}
						});
		System.out.println( "File generation took: " + ( System.currentTimeMillis() - start ) + "ms" );
	}
}