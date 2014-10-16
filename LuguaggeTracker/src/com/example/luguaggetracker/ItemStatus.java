package com.example.luguaggetracker;

import java.util.ArrayList;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class ItemStatus extends FragmentActivity implements LocationListener {
	
	private GoogleMap googleMap;
	private boolean firstView = true;
	private Marker [] lostPoint;
	private Circle circle;
	private double radius;
	MyReceiver myReceiver;
	ArrayList<Item> itemList;
	LatLng currentLatLng;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.item_status);
		
		if(this instanceof ItemStatus) {
		    if (getActionBar().isShowing()) getActionBar().hide();
		} else {
		    if (getActionBar().isShowing()) getActionBar().hide();
		}
		      
        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        // Showing status
        if(status!=ConnectionResult.SUCCESS){ // Google Play Services are not available
        	
        	int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
            
        }else {	// Google Play Services are available	
		
			// Getting reference to the SupportMapFragment of activity_main.xml
			SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
			
			// Getting GoogleMap object from the fragment
			googleMap = fm.getMap();
			
			// Enabling MyLocation Layer of Google Map
			googleMap.setMyLocationEnabled(true);				
					
			
			 // Getting LocationManager object from System Service LOCATION_SERVICE
	        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	
	        // Creating a criteria object to retrieve provider
	        Criteria criteria = new Criteria();
	
	        // Getting the name of the best provider
	        String provider = locationManager.getBestProvider(criteria, true);
	
	        // Getting Current Location
	        Location location = locationManager.getLastKnownLocation(provider);
	
	        if(location!=null){
	                onLocationChanged(location);
	        }
	        //update my location every 20sec	        	       
	       	locationManager.requestLocationUpdates(provider, 10*1000, 0, this);
	       
        }
        
        /***************************************/

        itemList = getIntent().getParcelableArrayListExtra("myItemList");
        
        lostPoint = new Marker[itemList.size()];
        
        for (int i=0; i<itemList.size(); i++)
			addMarkersToMapIfNeeded(itemList.get(i), i);
        
        /****************************************/
	}

	@Override
	protected void onStart() {
		
        //Register BroadcastReceiver
        //to receive event from our service
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Beacon_Service.UPDATE_LIST);
        registerReceiver(myReceiver, intentFilter);
        
		 super.onStart();
	}
	
	@Override
	protected void onStop() {
		unregisterReceiver(myReceiver);
		super.onStop();
	}
	
	@Override
	public void onLocationChanged(Location location) {
		
		// Getting latitude of the current location
		double latitude = location.getLatitude();
		
		// Getting longitude of the current location
		double longitude = location.getLongitude();		
		
		// Creating a LatLng object for the current location
		LatLng latLng = new LatLng(latitude, longitude);
		currentLatLng = latLng;
		
		if(firstView){
			// Showing the current location in Google Map
			googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
			
			// Zoom in the Google Map
			googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
			
			firstView = false;
		}
	}
	
	private void addMarkersToMapIfNeeded(Item item, int index) {
		this.radius = 10.0;
		
		if (item != null) {
	        if (item.getTrackEnabled() && item.getLostStatus()){
	        	LatLng latLng = item.getLocation();
	        	if (currentLatLng != null){
		        	if (latLng.latitude == 0 && latLng.longitude == 0){
		        		latLng = currentLatLng;
		        		item.updateLatLng(latLng);
		        		DatabaseHandler db = DatabaseHandler.getInstance(this);
		        		db.updateItem(item);
		        	}
	        	}
	        	lostPoint[index] = googleMap.addMarker(new MarkerOptions()
		        .position(latLng)
		        .title(item.getName())
				.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
				circle = googleMap.addCircle(new CircleOptions()
	            .center(latLng)
	            .radius(radius)
	            .strokeWidth(1)
	            .strokeColor(Color.RED)
	            .fillColor(0x99ff0000));
				// Showing the item last location in Google Map
				googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
	        }
        }
	}
	
	private void remvMarkerFromMap(int index){
		if (lostPoint != null){
			if (lostPoint[index] != null){
				lostPoint[index].remove();
				circle.remove();
			}
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
    
	private class MyReceiver extends BroadcastReceiver{
		 
		 @Override
		 public void onReceive(Context arg0, Intent arg1) {
			 
//			 item = (Item) arg1.getParcelableExtra("myItem");
			 itemList = arg1.getParcelableArrayListExtra("myItemList");

			 if (lostPoint == null)
				 lostPoint = new Marker[itemList.size()];
			 
			 for (int i=0; i<itemList.size(); i++){
				 Item item = itemList.get(i);
				 if (item.getLostStatus() && item.getTrackEnabled())
					 addMarkersToMapIfNeeded(item, i);
				 else
					 remvMarkerFromMap(i);
		 	}
		}
	}
}

