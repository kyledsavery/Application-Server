package applicationserver;

import appserver.comm.ConnectivityInfo;

/* Keeps track of how many satellites are available 
and which one is up next to be assigned a job */
public class LoadBalancingManager {
    
    private int nextSatIndex;
    private int numSatellites;
    
    public LoadBalancingManager(){
        nextSatIndex = 0;
        numSatellites = 0;
    }
    
    public void inform(){
        //Increment satellite count to keep track of available satellites
        numSatellites += 1;
    }

    public ConnectivityInfo getNext(){
        // No satellite to assign a job
        if(numSatellites == 0){return null;}
        
        // Get next satellite round robin style
        int nextSatellite = nextSatIndex++;
        if(nextSatIndex >= numSatellites){nextSatIndex = 0;}
        
        return ApplicationServer.satelliteManager.use(nextSatellite);
    }
}
