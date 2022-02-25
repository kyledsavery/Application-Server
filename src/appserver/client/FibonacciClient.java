package appserver.client;

import appserver.comm.Message;
import appserver.comm.MessageTypes;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import appserver.job.Job;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Properties;
import utils.PropertyHandler;

public class FibonacciClient extends Thread implements MessageTypes{
    
    String host = null;
    int port;
    Integer fib_number;

    Properties properties;

    public FibonacciClient(String serverPropertiesFile, Integer fib_number) {
        // Set up port/host for appserver
        try {
            properties = new PropertyHandler(serverPropertiesFile);
            host = properties.getProperty("HOST");
            System.out.println("[FibonacciClient.FibonacciClient] Host: " + host);
            port = Integer.parseInt(properties.getProperty("PORT"));
            System.out.println("[FibonacciClient.FibonacciClient] Port: " + port);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.fib_number = fib_number;
    }
    
    public void run() {
        try { 
            // connect to application server
            Socket server = new Socket(host, port);
            
            // hard-coded string of class, aka tool name ... fibonacci argument
            String classString = "appserver.job.impl.Fibonacci";            
            
            // create job and job request message
            Job job = new Job(classString, fib_number);
            Message message = new Message(JOB_REQUEST, job);
            
            // sending job out to the application server in a message
            ObjectOutputStream writeToNet = new ObjectOutputStream(server.getOutputStream());
            writeToNet.writeObject(message);
            
            // reading result back in from application server
            // for simplicity, the result is not encapsulated in a message
            ObjectInputStream readFromNet = new ObjectInputStream(server.getInputStream());

            Integer fib_result = (Integer) readFromNet.readObject();
            System.out.println("Fibonacci of " + fib_number + ": " + fib_result);
            
            server.close();
            
        } catch (IOException | ClassNotFoundException ex) {
            System.err.println("[FibonacciClient.run] Error occurred: " + ex);
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //Calculate numerous fibonacci numbers to induce load upon the satellites
        for(int i = 46; i > 0; i--){
            (new FibonacciClient("../../config/Server.properties", (Integer) i)).start();
        }
    }  
}
        

