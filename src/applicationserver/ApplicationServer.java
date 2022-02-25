
package applicationserver;


import appserver.comm.ConnectivityInfo;
import java.io.*;
import java.net.*;
import utils.PropertyHandler;

public class ApplicationServer {
    
    public ConnectivityInfo appServer = null;
    public static SatelliteManager satelliteManager = null;
    public static LoadBalancingManager loadManager = null;
    public static int jobId = 0;
    
    public ApplicationServer(String appInfo){
        appServer = new ConnectivityInfo();
        try {
            //Set up application server information
            PropertyHandler appConfig = new PropertyHandler(appInfo);
            appServer.setPort(Integer.parseInt(appConfig.getProperty("PORT")));
            appServer.setName(appConfig.getProperty("NAME"));
            
            //Init satellite and load managers
            satelliteManager = new SatelliteManager();
            System.out.println("[ApplicationServer.ApplicationServer] SatelliteManager Created");
            loadManager = new LoadBalancingManager();
            System.out.println("[ApplicationServer.ApplicationServer] LoadBalancingManager Created");
            
        }catch(FileNotFoundException fnfe){
            System.err.println("[ApplicationServer.ApplicationServer] Properties File Not Found" + fnfe);
            System.exit(1);
        }catch(IOException ioe){
            System.err.println("[ApplicationServer.ApplicationServer] Error Reading Properties" + ioe);
            System.exit(1);
        }
    }
    
    public void run(){
        try{
            ServerSocket serverSocket = new ServerSocket(appServer.getPort());
            System.out.println("[ApplicationServer.run] Application Server Waiting for connections on port #" 
                                                         + appServer.getPort());
            
            //Listen for incoming jobs
            while(true){
                Socket client = serverSocket.accept();
                (new AppHandler(client)).start();
            }
        }catch(IOException e){
            System.err.println("[ApplicationServer.run] Error Starting Application Server: " + e);
        }
    }
    
    public static void main(String[] args){
        //Create appserver and listen for incoming jobs
        ApplicationServer appServer = null;
        if(args.length == 1){
            appServer = new ApplicationServer(args[0]);
        } else {
            appServer = new ApplicationServer("../../config/Server.properties");      
        }
        
        System.out.println("[ApplicationServer.main] Application Server Created");
        
        // Begin the server loop and wait for incoming messages
        appServer.run();
    }
}
