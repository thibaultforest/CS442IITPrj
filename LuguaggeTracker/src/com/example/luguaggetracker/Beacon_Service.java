package com.example.luguaggetracker;

import java.util.concurrent.TimeUnit;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.BeaconManager.MonitoringListener;
import com.estimote.sdk.Region;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Beacon_Service extends Service{

	final static String NOTIFY_FOUND = "NOTITY_FOUND";
	final static String UPDATE_LIST = "UPDATE_LIST";
	
	private static final String TAG = "Beacon_service";
	private static final int NOTIFICATION_ID = 123;
	
	private BeaconManager beaconManager;
	private NotificationManager notificationManager;
	private ArrayList<Item> itemList;
	Bitmap largeIcon;
	
	private GPS_Service gpsService;
	boolean mBound = false;
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
	    public void onServiceConnected(ComponentName className, IBinder service) {
	    	gpsService = ((GPS_Service.GPSBinder)service).getService();
	    	mBound = true;
	    }
		
		@Override
	    public void onServiceDisconnected(ComponentName className) {
	    	gpsService = null;
	    	mBound = false;
	    }
	};
	
	@Override
	  public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "BEACON_SERVICE onStartCommand");
		
		if (notificationManager == null)
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		if (!mBound)
			doBindService();
		itemList = intent.getParcelableArrayListExtra("myItemList");
		if (itemList == null){
			DatabaseHandler db = DatabaseHandler.getInstance(getBaseContext());
			itemList = db.getAllItems();
		}
		
		for (int k=0; k<itemList.size(); k++){
			Item itemToMonitor = itemList.get(k);
			if (itemToMonitor.getTrackEnabled()){
				// Note that the regionID needs to be different for each beacon.
				final Region region = new Region("regionId"+k, itemToMonitor.getUUID() , itemToMonitor.getMajor(), itemToMonitor.getMinor());
				
				if (region != null){
				    
				    beaconManager = new BeaconManager(this);
			
				    // Default values are 5s of scanning and 25s of waiting time to save CPU cycles.
				    // In order for this demo to be more responsive and immediate we lower down those values.
				    beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(5), 10);
			
				    beaconManager.setMonitoringListener(new MonitoringListener() {
				      @Override
				      public void onEnteredRegion(Region region, List<Beacon> beacons) {
			
			    		  Log.d(TAG, "Beacons " + beacons);
				    	  // Find the item in itemList which match the beacon found.
				    	  for (Item item : itemList){
				    		  Log.d(TAG, "Beacon " + item.getMacAddress() + item.getName());
				    		  for (Beacon beacon : beacons){
				    			  if (item.matchWithBeacon(beacon)){
					    			  if (item.getTrackEnabled() && item.getLostStatus()){
					    		    	  Log.d(TAG, "Item track ON and just FOUND");
					    				  item.setItemFound();
								    	  updateItemList(item, false);
					    			  }
					    			  break;
					    		  }
				    		  }
				    	  }
				      }
			
				      @Override
				      public void onExitedRegion(Region region) {
				    	  
				    	  for (Item item : itemList){
				    		  
					    	  if (item.getTrackEnabled()){
					    		  if (item.belongToRegion(region)){
							    	  
							    	  item.setItemLost();
							    	  
						    		  // set current location to the lost item
							    	  if (mBound && gpsService != null)
							    		  item.setItemLocation(gpsService.mLocation);
							    	  
							    	  updateItemList(item, true);
									  break;
						    	  }
					    	  }
					      }
				      }
				    });
				    
				    beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
				      @Override
				      public void onServiceReady() {
				        try {
				          beaconManager.startMonitoring(region);
				        } catch (RemoteException e) {
				          Log.d(TAG, "Error while starting monitoring");
				        }
				      }
				    });
				}
			}
		}
		
		return Service.START_REDELIVER_INTENT;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void updateItemList(Item item, Boolean bool){
		// Update DB
		DatabaseHandler db = DatabaseHandler.getInstance(getBaseContext());
		db.updateItem(item);
		// Broadcast the updated list
		Intent intent = new Intent();
		intent.setAction(UPDATE_LIST);
		intent.putParcelableArrayListExtra("myItemList", itemList);
		sendBroadcast(intent);
		// Trigger notification to the user
		postNotification(bool, item);
	}
	
	private void postNotification(Boolean isLost, Item item) {
		
		largeIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher_bw);
	    String msg = "Notification Message";
		
	    Intent notifyIntent = new Intent(Beacon_Service.this, MainActivity.class);
	    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    PendingIntent pendingIntent = PendingIntent.getActivities(
	    		Beacon_Service.this,
	    		0,
	    		new Intent[]{notifyIntent},
	    		PendingIntent.FLAG_UPDATE_CURRENT);
	    
	    if (item != null){
	    	if (isLost)
	    		msg = "Item " + item.getName() + " lost.";
	    	else
	    		msg = "Item " + item.getName() + " found.";
	    }
	    
	    Intent showMapIntent = new Intent(Beacon_Service.this, ItemStatus.class);
	    if (itemList != null)
	    	showMapIntent.putParcelableArrayListExtra("myItemList", itemList);
	    
	    PendingIntent showMapPendingIntent = PendingIntent.getActivity(
	    		Beacon_Service.this,
	    		5698416,
	    		showMapIntent,
	    		PendingIntent.FLAG_UPDATE_CURRENT);
	    
	    int numberItemLost = 0;
	    for (Item myItem : itemList){
	    	if (myItem.getLostStatus() && myItem.getTrackEnabled())
	    		numberItemLost++;
	    }
	    
	    Notification notification = new Notification.Builder(Beacon_Service.this)
	        .setSmallIcon(R.drawable.beacon_gray)
	        .setLargeIcon(largeIcon)
	        .setContentTitle("Luggage Tracker ALert")
	        .setContentText(msg)
	        .setAutoCancel(true)
	        .setVibrate(new long[] { 100, 100, 100, 100, 100, 100, 100, 100, 100})
	        .setLights(Color.RED, 300, 30)
	        .setNumber(numberItemLost)
	        .setContentIntent(pendingIntent)
	        .addAction(android.R.drawable.ic_dialog_info, "Show Map", showMapPendingIntent)
	        .build();
	    
	    notification.defaults |= Notification.DEFAULT_SOUND;
	    notification.defaults |= Notification.DEFAULT_LIGHTS;
	    notificationManager.notify(NOTIFICATION_ID, notification);
	}

	@Override
	public void onDestroy() {
		// Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
	    notificationManager.cancel(NOTIFICATION_ID);
	    beaconManager.disconnect();
	    super.onDestroy();
	}
	
	void doBindService() {
	    bindService(new Intent(this, GPS_Service.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	void doUnbindService() {
	    // Detach our existing connection.
	    unbindService(mConnection);
	}
}
