
package applicationserver;

import appserver.comm.ConnectivityInfo;
import appserver.comm.Message;
import static appserver.comm.MessageTypes.*;
import java.io.*;
import java.net.*;

// Handle incoming client job requests and satellites registering
public class AppHandler extends Thread{
    
    Socket client = null;
    
    public AppHandler(Socket socket){
        this.client = socket;
    }
    
    @Override
    public void run(){
        ObjectOutputStream writeToClient = null;
        ObjectInputStream readFromClient = null;
        ObjectOutputStream writeToSat = null;
        ObjectInputStream readFromSat = null;
        Message message = null;
        
        // Set up streams to/from client
        try{
            readFromClient = new ObjectInputStream(client.getInputStream());
            writeToClient = new ObjectOutputStream(client.getOutputStream());
            
        }catch(IOException ioe){
            System.err.println("[AppHandler.run] Error Connecting to Client: "+ioe);
            System.exit(1);
        }
        
        //Get message from client
        try {
            message = (Message) readFromClient.readObject();
        }catch (IOException ioe) {
            System.out.println("[AppHandler.run] Error reading message in " + ioe);
            System.exit(1);
        }catch(ClassNotFoundException cnfe){
            System.err.println("[AppHandler.run] Class not found: " + cnfe);
        }
        
        //Exit if the message is not valid
        if(message == null){
            System.err.println("[AppHandler.run] Invalid message from client");
            System.exit(1);
        }
        
        switch(message.getType()){
            
            /*Register a new satellite with the satellite manager
            and inform the load balancing manager*/
            case REGISTER_SATELLITE -> {
                //Add this new nextSatellite to the list of registered satellites
                ConnectivityInfo newSatellite = (ConnectivityInfo) message.getContent();
                ApplicationServer.satelliteManager.register(newSatellite);
                System.out.println("\n[AppHandler.run] A new satellite <"
                        + newSatellite.getName() + "> has registered.");

                //Inform the load manager a new satellite is available
                ApplicationServer.loadManager.inform();
            }
            
            /* Find a satellite for this job and send the result 
            back to the client once it is complete */
            case JOB_REQUEST -> {
                int thisJobId = ApplicationServer.jobId++;
                System.out.println("\n[ApplicationServer.run] New Job ["
                        + thisJobId + "] Request from client");
                
                //Use LoadBalaningManager to find the nextSatellite and send this job
                ConnectivityInfo nextSatellite = ApplicationServer.loadManager.getNext();
                System.out.println("[ApplicationServer.run] Job [" 
                        + thisJobId + "] being sent to satellite <"
                        + nextSatellite.getName() + ">");

                // Set up streams to/from satellite
                Socket satellite = null;
                try{
                    //Connect to the chosen satellite
                    satellite = new Socket(nextSatellite.getHost(), nextSatellite.getPort());
                    writeToSat = new ObjectOutputStream(satellite.getOutputStream());
                    readFromSat = new ObjectInputStream(satellite.getInputStream());

                    //Send this Job to the satellite
                    message.setId(thisJobId);
                    writeToSat.writeObject(message);

                    //Get result from the satellite and send back to client
                    writeToClient.writeObject(readFromSat.readObject());
                    System.out.println("[ApplicationServer.run] Result for job [" 
                            + thisJobId + "] sent back to client");
                    
                    satellite.close();
                }catch(IOException ioe){
                    System.err.println("[AppHandler.run] Error Sending/Receiving Job to <"
                            + nextSatellite.getName() + ">: " + ioe);
                }catch(ClassNotFoundException cnfe){
                    System.err.println("[AppHandler.run] Class not found: " + cnfe);
                }
            }
            
            default -> System.err.println("[AppHandler.run] MESSAGE TYPE NOT IMPLEMENTED");
        }
        
        try{
            client.close();
            
        }catch(IOException ioe){
            System.err.println("[AppHandler.run] Client socket closing failed: "+ioe);
        }
            
    }
}
