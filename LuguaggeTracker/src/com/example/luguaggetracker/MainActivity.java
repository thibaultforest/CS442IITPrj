package com.example.luguaggetracker;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements View.OnClickListener{
	
	static final int NUM_SECTIONS = 1;

	ArrayList<Item> itemList;
	private static final int REQUEST_ENABLE_BT = 1234;

	ImageButton addItemBtn;
	ImageButton showMapBtn;
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			ItemList iL = new ItemList();
        	iL.itemList = itemList;
			getSupportFragmentManager().beginTransaction().add(R.id.list, iL).commit();
		}
		
		if (itemList == null) {
	        DatabaseHandler db = DatabaseHandler.getInstance(this);
			itemList = db.getAllItems();
		}

		
		if(this instanceof MainActivity) {
		    if (getActionBar().isShowing()) getActionBar().hide();
		} else {
		    if (getActionBar().isShowing()) getActionBar().hide();
		}
		
		showMapBtn = (ImageButton) findViewById(R.id.show_map);
		showMapBtn.setOnClickListener(this);
		addItemBtn = (ImageButton) findViewById(R.id.add_item);
		addItemBtn.setOnClickListener(this);
		
		//Check GPS enabled or not
		isDeviceGPSEnabled();
		//Start GPS background service
		Intent intent = new Intent(this, GPS_Service.class);
		startService(intent);

		// Initializes Bluetooth adapter.
		final BluetoothManager bluetoothManager =  (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
		
		// Ensures Bluetooth is available on the device and it is enabled. If not,
		// displays a dialog requesting user permission to enable Bluetooth.
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		else {
			startBeaconService();
		}
	}
	
	private void startBeaconService(){

		if (itemList != null){
	        Intent beaconServiceIntent = new Intent(this, Beacon_Service.class);
	 		beaconServiceIntent.putParcelableArrayListExtra("myItemList", itemList);
	 		this.startService(beaconServiceIntent);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_ENABLE_BT) {
		
		     if(resultCode == -1) { // RESULT_OK
		        startBeaconService();
		     }
			if (resultCode == 0) { // RESULT_CANCELED
			     // Alert the user that the tracking cannot work without BLE
				Toast.makeText(this, "Please turn ON the Bluetooth to track your devices.", Toast.LENGTH_LONG).show();
			}
		}
	}
	
/******************Check device GPS status*************/	
	private void isDeviceGPSEnabled(){
		 final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

		if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
		    buildAlertMessageNoGps();
		}
	 }
	
	private void buildAlertMessageNoGps() {
		 AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
	        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
	        .setCancelable(false)
	        .setPositiveButton("Goto Settings GPS",
	                new DialogInterface.OnClickListener(){
	            public void onClick(DialogInterface dialog, int id){
	                Intent callGPSSettingIntent = new Intent(
	                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	                startActivity(callGPSSettingIntent);
	            }
	        });
	        alertDialogBuilder.setNegativeButton("Cancel",
	                new DialogInterface.OnClickListener(){
	            public void onClick(DialogInterface dialog, int id){
	                dialog.cancel();
	            }
	        });
	        AlertDialog alert = alertDialogBuilder.create();
	        alert.show();
	}
/*****************************************************/

	
   @Override
   protected void onDestroy() {
		super.onDestroy();
		
		DatabaseHandler db = DatabaseHandler.getInstance(this);
		db.setAllItemToLost(itemList);
		
		stopService(new Intent(this, GPS_Service.class));
		stopService(new Intent(this, Beacon_Service.class));
   }
   
	@Override
	public void onClick(View v) {
	
		int rId = v.getId();
		if (rId == R.id.show_map){
			if (itemList == null){
				DatabaseHandler db = DatabaseHandler.getInstance(this);
				itemList = db.getAllItems();
			}
			Intent intent = new Intent(this, ItemStatus.class);
			intent.putParcelableArrayListExtra("myItemList", itemList);
			startActivity(intent);
		} else if (rId == R.id.add_item){
			Intent intent = new Intent(this, AddItem.class);
			startActivity(intent);
		}
	}
}
