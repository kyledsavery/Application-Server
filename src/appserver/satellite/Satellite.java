package appserver.satellite;

import appserver.job.Job;
import appserver.comm.ConnectivityInfo;
import appserver.job.UnknownToolException;
import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;
import appserver.job.Tool;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import utils.PropertyHandler;

/**
 * Class [Satellite] Instances of this class represent computing nodes that execute jobs by
 * calling the callback method of tool a implementation, loading the tool's code dynamically over a network
 * or locally from the cache, if a tool got executed before.
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class Satellite extends Thread {

    private ConnectivityInfo satelliteInfo = new ConnectivityInfo();
    private ConnectivityInfo serverInfo = new ConnectivityInfo();
    private HTTPClassLoader classLoader = null;
    private Hashtable<String, Tool> toolsCache = null;

    public Satellite(String satellitePropertiesFile, String classLoaderPropertiesFile, String serverPropertiesFile) {

        // read this satellite's properties and populate satelliteInfo object,
        // which later on will be sent to the server
        // ...
        try {
            PropertyHandler satelliteConfig = new PropertyHandler(satellitePropertiesFile);
            satelliteInfo.setPort( Integer.parseInt(satelliteConfig.getProperty("PORT")) );
            satelliteInfo.setName( satelliteConfig.getProperty("NAME") );
        }
        catch (IOException e) {
            System.err.println("No config file for the satellite server found, exiting...");
            System.exit(1);
        }
        
        // read properties of the application server and populate serverInfo object
        // other than satellites, the as doesn't have a human-readable name, so leave it out
        // ...
        try {
            PropertyHandler serverConfig = new PropertyHandler(serverPropertiesFile);
            serverInfo.setPort( Integer.parseInt(serverConfig.getProperty("PORT")) );
            serverInfo.setHost( serverConfig.getProperty("HOST") );
        }
        catch (IOException e) {
            System.err.println("No config file for the app server found, exiting...");
            System.exit(1);
        }
        
        
        // This is the HTTPClassLoader, given to us
        // read properties of the code server and create class loader
        // -------------------
        // ...
        try {
            PropertyHandler loaderConfig = new PropertyHandler(classLoaderPropertiesFile);
            classLoader = new HTTPClassLoader( loaderConfig.getProperty("HOST"), Integer.parseInt(loaderConfig.getProperty("PORT")) );
        }
        catch (Exception e) {
            System.out.println("Server Config data invalid: " + e);
            System.exit(1);
        }

        
        // create tools cache
        // -------------------
        // ...
        toolsCache = new Hashtable<String, Tool>();
          
    }

    @Override
    public void run() {

        // register this satellite with the SatelliteManager on the server
        // ---------------------------------------------------------------
        try{
            Socket server = new Socket(serverInfo.getHost(), serverInfo.getPort());
            
            Message message = new Message(REGISTER_SATELLITE, satelliteInfo);
            ObjectOutputStream writeToNet = new ObjectOutputStream(server.getOutputStream());
            writeToNet.writeObject(message);
            
        }catch(IOException ioe){
            System.err.println("[Satellite.run] Exiting after error connecting to app server: " + ioe);
            System.exit(1);
        }
        
        try {
            
            ServerSocket serverSocket = new ServerSocket(satelliteInfo.getPort());
            System.out.println("Satellite <"+satelliteInfo.getName()
                    +"> running on port #" +satelliteInfo.getPort());

            // start taking job requests in a server loop
            // ---------------------------------------------------------------
            // ...
            while (true) {
                (new SatelliteThread(serverSocket.accept(), this)).start();
                System.out.println("\n[Satellite.run] New Job Received");
            }
            
        }
        catch(Exception e) {
            System.out.println("Server Socket creation invalid: " + e);
        }
        
    }

    // inner helper class that is instanciated in above server loop and processes single job requests
    private class SatelliteThread extends Thread {

        Satellite satellite = null;
        Socket jobRequest = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;

        SatelliteThread(Socket jobRequest, Satellite satellite) {
            this.jobRequest = jobRequest;
            // pointer to outside class (this is an inner class)
            this.satellite = satellite;
        }

        @Override 
        public void run() {
            // setting up object streams
            // use try catch
            // ...

            try {
                readFromNet = new ObjectInputStream( jobRequest.getInputStream() );
                writeToNet = new ObjectOutputStream( jobRequest.getOutputStream() );

            } catch(Exception e){
                System.out.println("[SatelliteThread.run] Error creating input/output streams: " + e);
            }
            
            // reading message
            // this is a job request
            // ...
            try {
                message = (Message)readFromNet.readObject();
            }
            catch (Exception e) {
                System.out.println("Error reading message in " + e);
            }
            
            switch (message.getType()) {
                case JOB_REQUEST:
                    // Get content
                    
                    Job job = (Job) message.getContent();

                    // get tool name
                    String toolName = job.getToolName();

                    // get parameters
                    Object parameters = job.getParameters();

                    // call get tool object
                    Tool toolObj = null;
                    Object toReturn = null;
                    
                    try {
                        toolObj = getToolObject(toolName);
                        toReturn = toolObj.go(parameters);
                        System.out.println("[SatelliteThread.run] Job ["
                                + message.getId() + "] Complete");
                        writeToNet.writeObject(toReturn);
                    }
                    catch(Exception e) {
                        System.err.println("Cannot get tool object " + e);
                    }
                                        
                    break;

                default:
                    System.err.println("[SatelliteThread.run] Warning: Message type not implemented");
            }
        }
    }

    // Strong parallels to getoperation in dynamic calculator
    /**
     * Aux method to get a tool object, given the fully qualified class string
     * If the tool has been used before, it is returned immediately out of the cache,
     * otherwise it is loaded dynamically
     */
    public Tool getToolObject(String toolClassString) throws UnknownToolException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        Tool toolObject = null;

        //if not in cache
        if ( (toolObject = toolsCache.get(toolClassString)) == null) {
            
            if (toolClassString == null) {
                throw new UnknownToolException();
            }
            
            System.out.println("Requesting " + toolClassString + " from server..");
            
            Class<?> toolClass = classLoader.loadClass(toolClassString);
            try {
                toolObject = (Tool) toolClass.getDeclaredConstructor().newInstance();
            }
            catch (Exception e) {
                System.err.println("Error creating tool object in getToolObject: " + e);
            }

            toolsCache.put(toolClassString, toolObject);

        }
        else{
            System.out.println("Tool: " + toolClassString + " already in cache");
        }
        
        return toolObject;
    }

    public static void main(String[] args) {
        //Configure and start the satellite
        Satellite satellite = null;
        
        //Allow satellite to take all three paths to property files
        if(args.length == 3){
            satellite = new Satellite(args[0], args[1], args[2]);
        } else {
            String satProperties = "";
            //Satellite also accepts a satellite name
            if(args.length == 1){
                satProperties = "../../config/Satellite."+args[0]+".properties";
            }else{
                System.err.println("[Satellite.main] No satellite specified, exiting...");
                System.exit(1);
            }
            satellite = new Satellite(satProperties,
                                      "../../config/WebServer.properties",
                                      "../../config/Server.properties");      
        }
        
        satellite.run();
    }
}