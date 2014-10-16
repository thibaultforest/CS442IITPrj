package com.example.luguaggetracker;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


public class ItemList extends ListFragment {
	
	//custom adapter
	private MenuItemAdapter itemAdapter;
	public ArrayList<Item> itemList;
	ItemListReceiver myReceiver;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (itemList == null) {
        	DatabaseHandler db = DatabaseHandler.getInstance(getActivity());
            //get items from database
            itemList = db.getAllItems();
        }
        
        //set custom item adapter to the item list
        itemAdapter = new MenuItemAdapter(getActivity(), R.layout.item_view, itemList);  
        setListAdapter(itemAdapter);        
    }
  
    // Fragment's UI 
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.item_list, container, false);
       
        return rootview;
    }
    
    @Override
    public void onStart() {
	  //Register BroadcastReceiver
	  //to receive event from our service
	  myReceiver = new ItemListReceiver();
	  IntentFilter intentFilter = new IntentFilter();
	  intentFilter.addAction(Beacon_Service.UPDATE_LIST);
	  getActivity().registerReceiver(myReceiver, intentFilter);
	  
	  DatabaseHandler db = DatabaseHandler.getInstance(getActivity());
      itemList = db.getAllItems();
	  itemAdapter.items = itemList;
	  itemAdapter.notifyDataSetChanged();
     super.onStart();
    }
    
    @Override
    public void onStop() {
     // TODO Auto-generated method stub
     getActivity().unregisterReceiver(myReceiver);
     super.onStop();
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	
    	Intent intent = new Intent(getActivity(), DetailsItem.class);
    	intent.putExtra("item", itemList.get(position));
    	startActivity(intent);
    }
	
    private class ItemListReceiver extends BroadcastReceiver{
    	 
    	 @Override
    	 public void onReceive(Context arg0, Intent arg1) {
    		 itemList = arg1.getParcelableArrayListExtra("myItemList");
    		 itemAdapter.items = itemList;
    		 itemAdapter.notifyDataSetChanged();
    	 }
    	 
    }
    
}
