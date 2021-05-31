package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

import server.ServerMT;

public class ClientMT {
    private static Socket socket;
    private static String myPath = getProjectPath().toString()+"/destClientFolder/";
    // methode qui retourne le chemin du projet
 	public static Path getProjectPath() {
 		Path path = null;
 		try{
 			path = Paths.get(ServerMT.class.getResource(".").toURI());               // <-- Parent directory
 			return path.getParent().getParent();// <=> Parent of parent directory from bin
 		} catch (URISyntaxException e) {
 			e.printStackTrace();
 		}
 		return path;
 	}  
    
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String ipServer="";
		int port=0;
		//------ Validation de l'adresse ip et du numero du port ------
		Scanner myObj = new Scanner(System.in);  // Create a Scanner object
	    boolean isValid = false;
		while(!isValid) {
		    isValid = true;
		    System.out.print("Entez l'adresse IP du serveur au format xxx.xxx.xxx.xxx : ");    
		    ipServer = myObj.nextLine();
		    int[] nbres  = Arrays.stream(ipServer.split("\\."))
		        		    .mapToInt(Integer::parseInt)
		        		    .toArray();
		    for(int i=0;i<nbres.length;i++) {	
		        if(nbres[i]<0 || nbres[i]>255) {
		        	 System.out.println("Adresse Ip invalid reessayer ....");
		        	 isValid = false;
		        	 break;
		        }
			}        
		}
		isValid = false;
		while(!isValid) {
			System.out.print("Entez le numero du port : ");
			port = myObj.nextInt();
			if(port<5000 || port>5050) {
				System.out.println("Numero du port doit etre compris entre 5000 et 5050 ..."); 
			}else {
				isValid =true; 
			}
		}		
		socket = new Socket(ipServer,port);
		DataInputStream in = new DataInputStream(socket.getInputStream());
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		int cmptMessage = 1;
		System.out.println("\n" +"\n" );
		while(cmptMessage<=9) {
			System.out.println(in.readUTF());
			cmptMessage++;
		}
	    while(true) {
	    	String cmd="";
            while(cmd.equals("")){
            	cmd = myObj.nextLine();
            }
			out.writeUTF(cmd);
			String subCmd = cmd.indexOf(" ")!= -1?cmd.substring(0,cmd.indexOf(" ")):cmd; 
			if(subCmd.equals("download")) {
				String fileName = cmd.substring(cmd.indexOf(" ")+1,cmd.length()); 
				// Create destination file : destFichier
				FileOutputStream fos = new FileOutputStream(myPath+"\\"+fileName);
				byte [] data = new byte[19200];
				for(int read; (read = in.read( data )) > 0; )
	                  fos.write( data, 0, read );
				fos.flush();
				fos.write(data, 0, data.length);
				//fos.close();
				System.out.println("Le fichier "+fileName+" a bien été téléchargé");
			}else if(subCmd.equals("upload")){
				String filename = cmd.substring(cmd.indexOf(" ")+1,cmd.length());
				String myPath = getProjectPath().toString()+"\\destClientFolder\\";
				FileInputStream fis = new FileInputStream(myPath+filename);
			    byte [] data=new byte[19200];
			    for(int read; (read = fis.read( data )) > -1; )
	                  out.write( data, 0, read );
	            out.flush();
	            System.out.println(in.readUTF());
			}else {
				String reponse = in.readUTF();
	            System.out.println(reponse);
			}            
	    }
	}	    
}
