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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


public class DetailsItem extends Activity implements OnClickListener {
	
	static final int REQUEST_IMAGE_CAPTURE = 1872;
	private Item item;
	private ImageView capture;
	EditText editName;
	EditText editDescription;
	private Button save;
	private Button delete;
	final String dir = Environment
			.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
			+ "/picFolder/";
	String lastImgName;
	
	ActionMode.Callback mCallback;
	ActionMode mMode;
	
	/*****************Used for beacon distance****************/
	 private static final String TAG = DetailsItem.class.getSimpleName();
	
	  // Y positions are relative to height of bg_distance image.
	  private static final double RELATIVE_START_POS = 320.0 / 1110.0;
	  private static final double RELATIVE_STOP_POS = 885.0 / 1110.0;
	
	  private BeaconManager beaconManager;
//	  private Beacon beacon;
	  private Region region;
	
	  private View dotView;
	  private int startY = -1;
	  private int segmentLength = -1;
	/***********************************************************/
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_details_item);
		dotView = findViewById(R.id.dot);
		
		if(this instanceof DetailsItem) {
		    if (getActionBar().isShowing()) getActionBar().hide();
		} else {
		    if (getActionBar().isShowing()) getActionBar().hide();
		}
		
		editName = (EditText) findViewById(R.id.editBagName);
		editDescription = (EditText) findViewById(R.id.editDescription);
		
		editName.setFocusableInTouchMode(true);
		editName.setFocusable(true);
		editDescription.setFocusableInTouchMode(true);
		editDescription.setFocusable(true);
		
		//icon up button enabled
		getActionBar().setDisplayHomeAsUpEnabled(true);
	    
		item = getIntent().getParcelableExtra("item");
		
		save = (Button) findViewById(R.id.save);
		save.setOnClickListener(this);
		delete = (Button) findViewById(R.id.delete);
		delete.setOnClickListener(this);
		
		capture = (ImageView) findViewById(R.id.btnCapture);
		capture.setOnClickListener(this);
		
		if (item != null) {
			editName.setText(item.getName());
			if (!item.getDescription().equals(""))
				editDescription.setText(item.getDescription());
		}
		
		// Set the item picture
		if (!item.getPicture().equals("Nodescription")){
			File imgFile = new  File(item.getPicture());
			if(imgFile.exists()){
			    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
			    capture.setImageBitmap(RoundedImageView.getCroppedBitmap(myBitmap, 100));
			}
		}
		
		/*************Contextual menu*******************/
		mCallback = new ActionMode.Callback() {        	
			private boolean saveDetails = true; 
        	
			/** Invoked whenever the action mode is shown. This is invoked immediately after onCreateActionMode */ 
			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {		
				return false;
			}
						
			/** Called when user exits action mode */			
			@Override
			public void onDestroyActionMode(ActionMode mode) {
				mMode = null;
				editName.setFocusableInTouchMode(false);
				editName.setFocusable(false);
				editDescription.setFocusableInTouchMode(false);
				editDescription.setFocusable(false);
				
				if(saveDetails){				
					DatabaseHandler db = DatabaseHandler.getInstance(getBaseContext());
					updateBasicInfoItem(item);
					db.updateItem(item);
				}
			}
			
			/** This is called when the action mode is created. This is called by startActionMode() */
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mode.setTitle("Edit Mode");				
				getMenuInflater().inflate(R.menu.context_menu, menu);
				return true;
			}
			
			/** This is called when an item in the context menu is selected */
			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch(item.getItemId()){
					case R.id.cancel:
						Toast.makeText(getBaseContext(), "Edit Canceled!", Toast.LENGTH_LONG).show();
						saveDetails = false;
						mode.finish();	// Automatically exists the action mode, when the user selects this action
						break;					
				}
				return false;
			}
		};
		/*************************************************/
		
		/*******************initiate distance process**************/

	    region = new Region("regionid", item.getUUID(), item.getMajor(), item.getMinor());
	    
	    beaconManager = new BeaconManager(this);
	    beaconManager.setRangingListener(new BeaconManager.RangingListener() {
	      @Override
	      public void onBeaconsDiscovered(Region region, final List<Beacon> rangedBeacons) {
	        // Note that results are not delivered on UI thread.
	        runOnUiThread(new Runnable() {
	          @Override
	          public void run() {
	            // Just in case if there are multiple beacons with the same uuid, major, minor.
	            Beacon foundBeacon = null;
	            for (Beacon rangedBeacon : rangedBeacons) {
	              if (rangedBeacon.getMacAddress().equals(item.getMacAddress())) {
	                foundBeacon = rangedBeacon;
	              }
	            }
	            if (foundBeacon != null) {
	              updateDistanceView(foundBeacon);
	            }else{
	            	//Hide dotView if beacon not found......
	            	dotView.setVisibility(View.INVISIBLE);
	            }
	            	
	          }
	        });
	      }
	    });

	    final View view = findViewById(R.id.sonar);
	    view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
	      @Override
	      public void onGlobalLayout() {
	        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

	        startY = (int) (RELATIVE_START_POS * view.getMeasuredHeight());
	        int stopY = (int) (RELATIVE_STOP_POS * view.getMeasuredHeight());
	        segmentLength = stopY - startY;
	      }
	    });

	    /**************************************************************/
	    
	}
	
	
	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.btnCapture){
			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
		        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
		    }
		} 
		else if(id == R.id.save)
		{
			DatabaseHandler db = DatabaseHandler.getInstance(getBaseContext());
			updateBasicInfoItem(item);
			db.updateItem(item);
			
			//Need to rebuild main screen item list!
			Intent intent = new Intent(getBaseContext(), MainActivity.class);
    		// set flag to prevent from pushing multiple MainActivity on the stack
    		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    		startActivity(intent);
		}
		else if(id == R.id.delete)
		{
			buildAlertMessage();
		}
	}
	
	private void buildAlertMessage() {
		 AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
	        alertDialogBuilder.setMessage("Are you sure remove this item?")
	        .setCancelable(false)
	        .setPositiveButton("Yes",
	                new DialogInterface.OnClickListener(){
	            public void onClick(DialogInterface dialog, int id){
	            	DatabaseHandler db = DatabaseHandler.getInstance(getBaseContext());
	        		db.deleteItem(item);

	        		Toast.makeText(getBaseContext(), "Removed one item!", Toast.LENGTH_SHORT).show();
	        		Intent intent = new Intent(getBaseContext(), MainActivity.class);
	        		// set flag to prevent from pushing multiple MainActivity on the stack
	        		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        		startActivity(intent);
	            }
	        });
	        alertDialogBuilder.setNegativeButton("No",
	                new DialogInterface.OnClickListener(){
	            public void onClick(DialogInterface dialog, int id){
	                dialog.cancel();
	            }
	        });
	        AlertDialog alert = alertDialogBuilder.create();
	        alert.show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
	        Bitmap imageBitmap = (Bitmap) extras.get("data");
	        capture.setImageBitmap(RoundedImageView.getCroppedBitmap(imageBitmap, 100));
	        
		    // here,we are making a folder named picFolder to store pics taken
			// by the camera using this application
			File newdir = new File(dir);
			newdir.mkdirs();
	
			// here,counter will be incremented each time,and the picture
			// taken by camera will be stored as 1.jpg,2.jpg and likewise.

			if (lastImgName == null){
				Calendar c = Calendar.getInstance();
				String cameraImageFileName = "IMG_" + c.get(Calendar.YEAR) + "-" +
			               ((c.get(Calendar.MONTH)+1 < 10) ? ("0" + (c.get(Calendar.MONTH)+1)) : (c.get(Calendar.MONTH)+1)) + "-" +
			               ((c.get(Calendar.DAY_OF_MONTH) < 10) ? ("0" + c.get(Calendar.DAY_OF_MONTH)) : c.get(Calendar.DAY_OF_MONTH)) + "_" +
			               ((c.get(Calendar.HOUR_OF_DAY) < 10) ? ("0" + c.get(Calendar.HOUR_OF_DAY)) : c.get(Calendar.HOUR_OF_DAY)) + "-" +
			               ((c.get(Calendar.MINUTE) < 10) ? ("0" + c.get(Calendar.MINUTE)) : c.get(Calendar.MINUTE)) + "-" +
			               ((c.get(Calendar.SECOND) < 10) ? ("0" + c.get(Calendar.SECOND)) : c.get(Calendar.SECOND)) + ".jpg";
				String file = dir + cameraImageFileName;
				lastImgName = file;
			}
			
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
			item.setPicture(lastImgName);
			DatabaseHandler db = DatabaseHandler.getInstance(getBaseContext());
			db.updateItem(item);
		}
	}
	
	//Update item details
	private void updateBasicInfoItem(Item item) {
		// Get item name from EditText
		String itemName = null;
		if (editName.length() != 0)
			itemName = editName.getText().toString();
		else
			itemName = "My item";
		// Get description from EditText
		String itemDescription = null;
		if (editDescription.length() != 0)
			itemDescription = editDescription.getText().toString();
		else
			itemDescription = "";

		item.setName(itemName);
		item.setDescription(itemDescription);
	}
	

	 /****************distance update**********************/
	 private void updateDistanceView(Beacon foundBeacon) {
	    if (segmentLength == -1) {
	      return;
	    }
	    dotView.animate().translationY(computeDotPosY(foundBeacon)).start();
	    if (!dotView.isShown())
	    	dotView.setVisibility(View.VISIBLE);
	  }

	  private int computeDotPosY(Beacon beacon) {
	    // Let's put dot at the end of the scale when it's further than 6m.
	    double distance = Math.min(Utils.computeAccuracy(beacon), 6.0);
	    return startY + (int) (segmentLength * (distance / 6.0));
	  }
	  
	  @Override
	  protected void onStart() {
	    super.onStart();

	    beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
	      @Override
	      public void onServiceReady() {
	        try {
	          beaconManager.startRanging(region);
	        } catch (RemoteException e) {
	          Toast.makeText(DetailsItem.this, "Cannot start ranging, something terrible happened",
	              Toast.LENGTH_LONG).show();
	          Log.e(TAG, "Cannot start ranging", e);
	        }
	      }
	    });
	  }
	  
	  @Override
	  protected void onStop() {
	    beaconManager.disconnect();

	    super.onStop();
	  }
	  /****************************************************/
}
