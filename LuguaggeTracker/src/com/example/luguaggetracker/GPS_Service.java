package com.example.luguaggetracker;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


public class GPS_Service extends Service implements 
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener, 
	LocationListener {

	private final IBinder mBinder = new GPSBinder();

	private LocationRequest mLocationRequest;  
	private LocationClient mLocationClient;  
	// Flag that indicates if a request is underway.
	private boolean mInProgress;
	private Boolean servicesAvailable = false;
	public Location mLocation;

	 private static final int TWO_MINUTES = 1000 * 60 * 2;

	 /**
	 * Called before service onStartCommand method is called. 
	 * All Initialization part goes here
	 **/ 
	 @Override
	 public void onCreate() {
		 
		 // Create the LocationRequest object
		 mLocationRequest = LocationRequest.create();
		 // Use high accuracy
		 mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
//		 mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		 // Set the update interval (30 seconds)
		 mLocationRequest.setInterval(Params.UPDATE_INTERVAL);
		 // Set the fastest update interval to 5 second
		 mLocationRequest.setFastestInterval(Params.FASTEST_INTERVAL);
		 
		 servicesAvailable = servicesConnected();
		 
		 setUpLocationClientIfNeeded();
	
	 }

	 /**
	  * Check the availability of the google service 
	  */
	 private boolean servicesConnected() {

		    // Check that Google Play services is available
		    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

		    // If Google Play services is available
		    if (ConnectionResult.SUCCESS == resultCode)
		        return true;
		    else
	    		return false;
		    
	 }
	 
	 /**
	  * To be executed on service start. Sometime due to memory congestion DVM kill the 
	  * running service but it can be restarted when the memory is enough to run the service again.
	 **/
	 public int onStartCommand(Intent intent, int flags, int startId) {
		    super.onStartCommand(intent, flags, startId);
		    
		    setUpLocationClientIfNeeded();

		    if (!servicesAvailable || mLocationClient.isConnected() || mInProgress)
		        return START_STICKY;
		    
		    if (!mLocationClient.isConnected() || !mLocationClient.isConnecting()
		            && !mInProgress) {
		    	
		        mLocationClient.connect();
		        mInProgress = true;
		    }

		    return START_NOT_STICKY;
		}
	 
	 /**
	  * Create a new location client
	  **/
	 private void setUpLocationClientIfNeeded() {
	     if (mLocationClient == null)
	         mLocationClient = new LocationClient(getApplicationContext(), this, this);
	 }
	 
	 /**
	  *This is override method of interface GooglePlayServicesClient.ConnectionCallbacks which is called
	  *when locationClient is connected to google service.
	  *You can receive GPS reading only when this method is called. So request for location updates from this
	  *method rather than onStartCommand() 
	 **/
	 
	 @Override
	 public void onConnected(Bundle arg0) {
		 Log.i("info", "Location Client is Connected");
		 mLocationClient.requestLocationUpdates(mLocationRequest, this);
	 }

	 @Override
	 public void onDisconnected() {
		 Log.i("info", "Location Client is Disconnected");
	 }
	 
	 /**
	  *Override method of the interface GooglePlayServicesClient.OnConnectionFailedListener .
	  *called when connection to the Google Play Service are not able to connect 
	 **/
	 @Override
	 public void onConnectionFailed(ConnectionResult connectionResult) {
		  /*
		   * Google Play services can resolve some errors it detects. If the error
		   * has a resolution, try sending an Intent to start a Google Play
		   * services activity that can resolve error.
		   */
		  if (connectionResult.hasResolution()) {
			  	// To do something 
		  } else {
		   // If no resolution is available, display a dialog to the user with
		   // the error.	   
			  Toast.makeText(getApplicationContext(), "No resolution is available", Toast.LENGTH_SHORT).show();
		  }
	 }
	 
	
	 /**
	  *Overridden method of interface LocationListener called when location of gps device is changed.
	  *Location Object is received as a parameter.
	 **/
	 @Override
	 public void onLocationChanged(Location location) {
		 
		  if (isBetterLocation(location, mLocation))
			  mLocation = location;
	 }
	 

	 /** Determines whether one Location reading is better than the current Location fix
	   * @param location  The new Location that you want to evaluate
	   * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	   */
	 protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	     if (currentBestLocation == null) {
	         // A new location is always better than no location
	         return true;
	     }

	     // Check whether the new location fix is newer or older
	     long timeDelta = location.getTime() - currentBestLocation.getTime();
	     boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	     boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	     boolean isNewer = timeDelta > 0;

	     // If it's been more than two minutes since the current location, use the new location
	     // because the user has likely moved
	     if (isSignificantlyNewer) {
	         return true;
	     // If the new location is more than two minutes older, it must be worse
	     } else if (isSignificantlyOlder) {
	         return false;
	     }

	     // Check whether the new location fix is more or less accurate
	     int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	     boolean isLessAccurate = accuracyDelta > 0;
	     boolean isMoreAccurate = accuracyDelta < 0;
	     boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	     // Check if the old and new location are from the same provider
	     boolean isFromSameProvider = isSameProvider(location.getProvider(),
	             currentBestLocation.getProvider());

	     // Determine location quality using a combination of timeliness and accuracy
	     if (isMoreAccurate) {
	         return true;
	     } else if (isNewer && !isLessAccurate) {
	         return true;
	     } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	         return true;
	     }
	     return false;
	 }

	 /** Checks whether two providers are the same */
	 private boolean isSameProvider(String provider1, String provider2) {
	     if (provider1 == null) {
	       return provider2 == null;
	     }
	     return provider1.equals(provider2);
	 }

	/**
	  *Called when Service running in background is stopped.
	  *Remove location update to stop receiving gps data
	 **/
	 @Override
	 public void onDestroy() {
	  // TODO Auto-generated method stub
	  Log.i("info", "Service is destroyed");
	  mLocationClient.removeLocationUpdates(this);
	  super.onDestroy();
	 }

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
    
	public class GPSBinder extends Binder {
	    public GPS_Service getService() {
	        return GPS_Service.this;
	    }
	}
    
}
