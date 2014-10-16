package com.example.luguaggetracker;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class MenuItemAdapter extends ArrayAdapter<Item> {

	Context context;
	List<Item> items;
	int resource;
	DatabaseHandler db;

  public MenuItemAdapter(Context context, int resource, List<Item> items) {
    super(context, resource, items);
    this.resource = resource;
    this.items = items;
    this.context = context;
    this.db = DatabaseHandler.getInstance(context);
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    LinearLayout todoView; 

    if (convertView == null) {
     // Inflate a new view if this is not an update.
      todoView = new LinearLayout(getContext());
      String inflater = Context.LAYOUT_INFLATER_SERVICE;
      LayoutInflater li;
      li = (LayoutInflater)getContext().getSystemService(inflater);
      li.inflate(resource, todoView, true);
    } else {
    // Otherwise we'll update the existing View
      todoView = (LinearLayout) convertView;
    }

    // Set item to the corresponding row (see item_view.xml = list's row)
    Item item = items.get(position);

    String item_name = item.getName();
    String item_description = item.getDescription();
    
    TextView nameView = (TextView)todoView.findViewById(R.id.name);
    TextView descriptionView = (TextView)todoView.findViewById(R.id.description);
//    TextView lost = (TextView)todoView.findViewById(R.id.lost);
    CheckBox checkBox = (CheckBox)todoView.findViewById(R.id.checkbox_track);
    ImageView lostImg = (ImageView) todoView.findViewById(R.id.lost);
    
    //CheckBox click handler
    checkBox.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
        	boolean checked = ((CheckBox) v).isChecked();
        	// Update item in database.
        	Item item = items.get(position);
        	item.setTrackEnabled(checked);
        	item.setItemLost();
        	Intent beaconServiceIntent = new Intent(v.getContext(), Beacon_Service.class);
        	beaconServiceIntent.putParcelableArrayListExtra("myItemList", (ArrayList<? extends Parcelable>) items);
    		v.getContext().startService(beaconServiceIntent);
    		DatabaseHandler db = DatabaseHandler.getInstance(v.getContext());
        	db.updateItem(item);
        }
    });
    
    nameView.setText(item_name);
    descriptionView.setText(item_description);
    
    Resources res = context.getResources();	
    Drawable draw;
    if (item.getLostStatus()){
    	draw = res.getDrawable(R.drawable.warning);
    }
    else
    	draw = res.getDrawable(R.drawable.ranging);
    lostImg.setImageDrawable(draw);
    checkBox.setChecked(item.getTrackEnabled());

    return todoView;
  }

}