package com.example.luguaggetracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.example.flatui.FlatUI;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class AddItem extends Activity implements View.OnClickListener {

	private Button save;
	private Button delete;
	private Button associate;
	private ImageView capture;
	EditText editName;
	EditText editDescription;
	private ProgressBar mProgress;

//	int TAKE_PHOTO_CODE = 0;
	public static int count = 0;
	final String dir = Environment
			.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
			+ "/picFolder/";
	String lastImgName = "";

	// Beacons variables
	private static final int REQUEST_ENABLE_BT = 1234;
	static final int REQUEST_IMAGE_CAPTURE = 1852;
	private static final String TAG = "Ranging beacons";
	public static final String EXTRAS_BEACON = "extrasBeacon";
	private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid",
			null, null, null);
	private BeaconManager beaconManager;
	private Beacon associatedBeacon = null;
	private int checkBeacon = 0;
	private boolean isConnectedToService = false;
	private Item newItem = null;
	private Bitmap imageBitmap;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Default theme should bew set before content view is added
		
		if(this instanceof AddItem) {
		    if (getActionBar().isShowing()) getActionBar().hide();
		} else {
		    if (getActionBar().isShowing()) getActionBar().hide();
		}

        setContentView(R.layout.activity_bag_details);
        // if you are using standard action bar (not compatibility library) use this
        // FlatUI.setActionBarTheme(this, theme, false, true);

        // if you are using ActionBar of Compatibility library (like this activity), get drawable
        // and set it manually to support action bar.
        getActionBar().setBackgroundDrawable(FlatUI.getActionBarDrawable(FlatUI.DEEP, false));

		save = (Button) findViewById(R.id.save);
		save.setOnClickListener(this);
		delete = (Button) findViewById(R.id.delete);
		delete.setOnClickListener(this);
		capture = (ImageView) findViewById(R.id.btnCapture);
		capture.setOnClickListener(this);
		associate = (Button) findViewById(R.id.associatebtn);
		associate.setOnClickListener(this);
		editName = (EditText) findViewById(R.id.editTextBagName);
		editDescription = (EditText) findViewById(R.id.editTextDescription);
		mProgress = (ProgressBar) findViewById(R.id.progressBar);
		mProgress.setVisibility(View.INVISIBLE);

		beaconManager = new BeaconManager(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == Activity.RESULT_OK) {
				connectToService();
			} else {
				Toast.makeText(this, R.string.error_bluetooth_not_enabled,
						Toast.LENGTH_LONG).show();
				getActionBar()
						.setSubtitle(R.string.error_bluetooth_not_enabled);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
	        imageBitmap = (Bitmap) extras.get("data");
	        imageBitmap = RoundedImageView.getCroppedBitmap(imageBitmap, 100);
	        capture.setImageBitmap(imageBitmap);
		}
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent(AddItem.this, MainActivity.class);
		// set flag to prevent from pushing multiple MainActivity on the stack
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		if (v.getId() == R.id.save) {
			// add new item to show in the menu
			if (newItem != null) {
				// Update name and description
				updateBasicInfoItem(newItem);
				// Add item to SQLite DB
				DatabaseHandler db = DatabaseHandler.getInstance(this);
				db.addItem(newItem);
				startActivity(intent);
			} else
				Toast.makeText(this, "Cannot add the bag properly.", Toast.LENGTH_LONG).show();
		} else if (v.getId() == R.id.delete) {
			startActivity(intent);
		} else if (v.getId() == R.id.btnCapture) {
			
			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
		        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
		    }

		} else if (v.getId() == R.id.associatebtn) {

			// Check if device supports Bluetooth Low Energy.
			if (!beaconManager.hasBluetooth()) {
				Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
				return;
			}
			// If Bluetooth is not enabled, let user enable it.
			if (!beaconManager.isBluetoothEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			} else {
				connectToService();
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		// setUpBluetooth();
	}

	@Override
	protected void onStop() {
		try {
			beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
		} catch (RemoteException e) {
			Log.d(TAG, "Error while stopping ranging", e);
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		beaconManager.disconnect();
		super.onDestroy();
	}

	private void connectToService() {
		associate.setBackgroundColor(Color.parseColor("#7f8c8d"));
		associate.setClickable(false);
		mProgress.setVisibility(View.VISIBLE);
		if (!isConnectedToService) {
			beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
				@Override
				public void onServiceReady() {
					try {
						beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
						getActionBar().setSubtitle("");
						isConnectedToService = true;
					} catch (RemoteException e) {
						Toast.makeText(AddItem.this, "Cannot start ranging, something terrible happened", Toast.LENGTH_LONG).show();
						Log.e(TAG, "Cannot start ranging", e);
						isConnectedToService = false;
						mProgress.setVisibility(View.INVISIBLE);
					}
				}
			});
		} 
		if (!isConnectedToService) {
			beaconManager.setRangingListener(new BeaconManager.RangingListener() {
				@Override
				public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
					// Note that results are not delivered on UI thread.
					// WARNING HERE! I put in comment THE ASSOCIATION PROCESS because
					// You can't add a bag without beacon now XD
/**/						runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// Note that beacons reported here are
							// already sorted by estimated
							// distance between device and beacon.
							if (!beacons.isEmpty()) {
								Beacon closestBeacon = beacons.get(0);
								if (associatedBeacon == null) 
									associatedBeacon = closestBeacon;
								
								if (Utils.computeAccuracy(closestBeacon) < 0.2f && associatedBeacon.equals(closestBeacon)) {
									checkBeacon++;
									Log.d(TAG, "Ready to association: d=" + Utils.computeAccuracy(beacons.get(0)));
									if (checkBeacon > 4) {
										finishAssociatingProcess();
										Thread.currentThread().interrupt();
									}
								} else {
									checkBeacon = 0;
									associatedBeacon = null;
								}
							}
						}
					});
/*					finishAssociatingProcess();*/
				}
			});
		}
	}

	private void finishAssociatingProcess() {
		
		if (associatedBeacon.getMacAddress() != null && associatedBeacon.getProximityUUID() != null){
			DatabaseHandler db = DatabaseHandler.getInstance(getBaseContext());
			if(!db.beaconAlreadyExist(associatedBeacon)){
				try {
					beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
				} catch (RemoteException e) {
					Log.d(TAG, "Error while stopping ranging", e);
				}
				mProgress.setVisibility(View.INVISIBLE);
				newItem = new Item("DefaultName", "Nodescription", lastImgName, associatedBeacon);
				updateBasicInfoItem(newItem);
				displayAssociationDone();
				associate.setText("Beacon associated!");
				save.setBackgroundColor(Color.parseColor("#27ae60"));
				associate.setBackgroundColor(Color.parseColor("#2ecc71"));
				
				//Used to beacon distance test
				Params.Beacons.add(associatedBeacon);
				
			}
			else{
				checkBeacon = 0;
				// TODO Notify the user that this beacon is already used!
				// Using DialogAlert?!
				Dialog alert = new AlertDialog.Builder(this)
	            .setIcon(android.R.drawable.ic_dialog_alert)
	            .setTitle("Association Failed!")
	            .setMessage("This device is already used.")
	            .setNegativeButton("Stop",
	                    new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                        	try {
	            					beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
	            				} catch (RemoteException e) {
	            					Log.d(TAG, "Error while stopping ranging", e);
	            				}
	                    		mProgress.setVisibility(View.INVISIBLE);
	                        }
	                    })
	            .setPositiveButton("Retry",
	                    new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                            // TODO when Retry click
	                        }
	                    }).create();
				associate.setBackgroundColor(Color.parseColor("#f1c40f"));
				alert.show();
			}
			
			
		}
		else
			checkBeacon = 0;
	}

	private void updateBasicInfoItem(Item item) {
		// Get bag name from EditText
		String itemName = null;
		if (editName.length() != 0)
			itemName = editName.getText().toString();
		else
			itemName = "My bag";
		// Get description from EditText
		String itemDescription = null;
		if (editDescription.length() != 0)
			itemDescription = editDescription.getText().toString();
		else
			itemDescription = "";

		item.setName(itemName);
		item.setDescription(itemDescription);
		
		File newdir = new File(dir);
		newdir.mkdirs();

		if (lastImgName.equals("") && imageBitmap != null){
			Calendar c = Calendar.getInstance();
			String cameraImageFileName = "IMG_" + c.get(Calendar.YEAR) + "-" +
		               ((c.get(Calendar.MONTH)+1 < 10) ? ("0" + (c.get(Calendar.MONTH)+1)) : (c.get(Calendar.MONTH)+1)) + "-" +
		               ((c.get(Calendar.DAY_OF_MONTH) < 10) ? ("0" + c.get(Calendar.DAY_OF_MONTH)) : c.get(Calendar.DAY_OF_MONTH)) + "_" +
		               ((c.get(Calendar.HOUR_OF_DAY) < 10) ? ("0" + c.get(Calendar.HOUR_OF_DAY)) : c.get(Calendar.HOUR_OF_DAY)) + "-" +
		               ((c.get(Calendar.MINUTE) < 10) ? ("0" + c.get(Calendar.MINUTE)) : c.get(Calendar.MINUTE)) + "-" +
		               ((c.get(Calendar.SECOND) < 10) ? ("0" + c.get(Calendar.SECOND)) : c.get(Calendar.SECOND)) + ".jpg";
			String file = dir + cameraImageFileName;
			lastImgName = file;
		
		File newfile = new File(lastImgName);
		try {
			newfile.createNewFile();
		} catch (IOException e) {
		}
        
        OutputStream outStream = null;
        if (newfile.exists()) {
        	newfile.delete();
        	newfile = new File(lastImgName);
        }
        try {
           // make a new bitmap from your file
           outStream = new FileOutputStream(lastImgName);
           imageBitmap.compress(Bitmap.CompressFormat.PNG, 90, outStream);
           outStream.flush();
           outStream.close();
        } catch (Exception e) {
           e.printStackTrace();
        }
        Log.e("file", "" + lastImgName);
		newItem.setPicture(lastImgName);
		}
		else
		{
			newItem.setPicture("Nodescription");
		}
	}

	private void displayAssociationDone() {
		
	}
}