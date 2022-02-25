package applicationserver;

import appserver.comm.ConnectivityInfo;

import java.util.ArrayList;

/* Holds list of available satellites and provides 
functionality to register and use them */
public class SatelliteManager {
    public static ArrayList<ConnectivityInfo> registeredSatellites = null;
    
    public SatelliteManager(){
        registeredSatellites = new ArrayList<>();
    }
    
    // Add a new Satellite to the list of available satellites
    public void register(ConnectivityInfo newSatellite){
        registeredSatellites.add(newSatellite);
    }

    // Return a satellite to be given a job
    public ConnectivityInfo use(int index){
        return registeredSatellites.get(index);
    } 
}
