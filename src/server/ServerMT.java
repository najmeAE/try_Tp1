package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class ServerMT {
	
	private static ServerSocket listener;
	
	public static void main(String[] args) throws IOException {		
		int clientNumber = 0;
		String serverAddress = "127.0.0.1";
		int serverPort = 5000;
		try {
			listener = new ServerSocket();
		} catch (IOException e) {
			e.printStackTrace();
		}
		listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);
		listener.bind(new InetSocketAddress(serverIP, serverPort));
		System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);
		try {
			while(true) {
				new ClientHandler(listener.accept(), clientNumber++).start();
			}
		}
		finally {
			listener.close();
		}	
	}
	private static class ClientHandler extends Thread{
		private Socket socket;
		private int clientNumber;
		private String myPath = getProjectPath().toString()+"\\resources\\";
		DataOutputStream out = null;
		DataInputStream in = null;
		public ClientHandler(Socket socket, int clientNumber) throws IOException {
			this.socket=socket;		
			this.clientNumber = clientNumber;
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());	
			System.out.println("New connection with client#"+ clientNumber+" at " + LocalDateTime.now());
		}
		
		public void run() {
			try {
				out.writeUTF("      Liste de commandes accepte par le serveur :");
				out.writeUTF("      ==========================================");
			    out.writeUTF(" 1- cd <Nom du répertoire sur le serveur> ");
			    out.writeUTF(" 2- ls  ");
			    out.writeUTF(" 3- mkdir <Nom du nouveau dossier>");
			    out.writeUTF(" 4- upload <Nom du fichier>");
			    out.writeUTF(" 5- download <Nom du fichier> ");
			    out.writeUTF(" 6- exit ");
			    out.writeUTF("Saisizez votre commande ...");
			    String fullCmd = "";
			    while(!fullCmd.equals("exit")) 
				{
					  while((fullCmd = in.readUTF()).equals("")) {}
					  if(!fullCmd.equals("exit")){        		
					      handleUserCmd(fullCmd);
					      fullCmd="";
					  }	    	
			    }			    
        	}catch(IOException e) {
				System.out.println("Error handling client#"+clientNumber+" : "+e);
			}
			finally {
				try {
					out.writeUTF("Vous avez été déconnecté avec succès.");
					socket.close();
				}
				catch(IOException e) {
					System.out.println("Coulnd't close a socket, what's going on?");
				}
				System.out.println("Connection with client# "+clientNumber+" closed");
			}
		}
		
		// methode qui retourne le chemin du projet
		public Path getProjectPath() {
			Path path = null;
			try{
				path = Paths.get(ServerMT.class.getResource(".").toURI());               // <-- Parent directory
				return path.getParent().getParent();// <=> Parent of parent directory from bin
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			return path;
		}
		

		private void handleUserCmd(String fullCmd) throws IOException {
			 String subCmd = fullCmd.indexOf(" ")!= -1?fullCmd.substring(0,fullCmd.indexOf(" ")):fullCmd;
	         DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd@HH:mm:ss");  
	         LocalDateTime myDateObj = LocalDateTime.now();
	         String formattedDate = myDateObj.format(myFormatObj);
	         String ipclient = this.socket.getRemoteSocketAddress().toString().substring(1); 
	         System.out.println("["+ipclient+"-"+formattedDate+"] : "+fullCmd);
			switch (subCmd) {
			case "cd": 
				String toFolder = fullCmd.substring(fullCmd.indexOf(" ")+1,fullCmd.length());
				if(!toFolder.equals("..")) {
					myPath = myPath+"\\"+toFolder;
					out.writeUTF("Vous êtes dans le dossier "+toFolder);
				}else {
					Path path = Paths.get(myPath).getParent();
					System.out.println("path = "+path);
					myPath = path.toString();
					String parentFolder = path.getFileName().toString();
					out.writeUTF("Vous êtes dans le dossier "+parentFolder);
				}	
				break;
			case "ls" :
				File files = new File(myPath);
				String resultat="";
			    List<File> subDirectories =files.listFiles()==null?Collections.<File>emptyList(): Arrays.stream(files.listFiles()).collect(Collectors.toList());
				if(subDirectories.isEmpty()) { 
					out.writeUTF("Ce repertoire est vide ...");
				}else {
			        for(File f : subDirectories) {
				    	if(f.isDirectory()) {
				    	   resultat += "[Folder] "+f.getName()+"\n";		    	   
				    	}else if(f.isFile()){
				    	resultat += "[File] "+f.getName()+"\n";
				    	}    	
				    }
				    out.writeUTF(resultat);
				}
				break;		
			case "mkdir":
				String foderToCreate = fullCmd.substring(fullCmd.indexOf(" ")+1,fullCmd.length()); 
				File newFolder = new File(myPath+foderToCreate);
				if(!newFolder.exists()) {
					if(newFolder.mkdir()) {
						out.writeUTF("Le dossier "+foderToCreate+" a été créé");
					}else {
						out.writeUTF("Echec de creation du dossier "+foderToCreate);
					}
				}else {
					out.writeUTF("Le dossier "+foderToCreate+" existe deja");
				}
				break;
			case "upload":	
				String fileName = fullCmd.substring(fullCmd.indexOf(" ")+1,fullCmd.length());
				// Create destination file : destFichier
				FileOutputStream fos = new FileOutputStream(myPath+"\\"+fileName);
				byte [] fdata = new byte[19200];
				for(int read; (read = in.read( fdata )) > -1; )
	                  fos.write( fdata, 0, read );
				out.writeUTF("Le fichier "+fileName+" a bien été téléversé");   
				break;
			case "download":
				String filename = fullCmd.substring(fullCmd.indexOf(" ")+1,fullCmd.length());				
				FileInputStream fis = new FileInputStream(myPath+filename);
			    byte [] data=new byte[19200];
			    for(int read; (read = fis.read( data )) > -1; )
	                  out.write( data, 0, read );
	            out.flush();
				break;
			case "exit":
				out.writeUTF("Vous avez été déconnecté avec succès.");
			default:
				out.writeUTF("Commande incorrect !!!");
				break;
			}
			
		}
	}
}

