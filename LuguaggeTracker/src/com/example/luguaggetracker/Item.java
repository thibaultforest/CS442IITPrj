package com.example.luguaggetracker;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Region;
import com.google.android.gms.maps.model.LatLng;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class Item implements Parcelable {
	
	int _id;
    String _name = null;
    String _picture = null;
    String _description = null;
    // GPS
    double _latitude = 0;
    double _longitude = 0;
    // Set device tracked or not.
    boolean _trackEnabled = false;
    boolean _isLost = false;
    // Beacon crucial informations
    String _macAddress = null;
    String _UUID = null;
    int _major = -1;
    int _minor = -1;
    

    // Constructor needed when items got from database.
    public Item(int id,
    		String name,
    		String picture,
    		String description,
    		double latitude,
    		double longitude,
    		boolean trackEnabled,
    		boolean isLost,
    		String macAddress,
    		String UUID,
    		int major,
    		int minor)
    {
        _id = id;
        _name = name;
        _picture = picture;
        _description = description;
        _latitude = latitude;
        _longitude =longitude;
        _trackEnabled = trackEnabled;
        _isLost = isLost;
        _macAddress = macAddress;
        _UUID = UUID;
        _major = major;
        _minor = minor;
    }
    // Constructor needed when new item (will be added to Database).
    public Item(String name, String picture, String description, Beacon beacon){
        _id = -1;
        _name = name;
        _picture = picture;
        _description = description;
        if (beacon != null){
	        _macAddress = beacon.getMacAddress();
	        _UUID = beacon.getProximityUUID();
	        _major = beacon.getMajor();
	        _minor = beacon.getMinor();
        }
    }

    // Getters
    public int getId(){return _id;}
    public String getName(){return _name;}
    public String getPicture(){return _picture;}
    public String getDescription(){return _description;}
    public double getLatitude(){return _latitude;}
    public double getLongitude(){return _longitude;}
    public LatLng getLocation(){return new LatLng(_latitude, _longitude);}
    public  boolean getTrackEnabled(){return _trackEnabled;}
    public  boolean getLostStatus(){return _isLost;}
    public String getMacAddress(){return _macAddress;}
    public String getUUID(){return _UUID;}
    public int getMajor(){return _major;}
    public int getMinor(){return _minor;}
   
    // Setters
    public void setId(int id){_id = id;}
    public void setName(String name){_name = name;}
    public void setPicture(String picture){_picture = picture;}
    public void setDescription(String description){_description = description;}
    public void setTrackEnabled(boolean trackEnabled){_trackEnabled = trackEnabled;}
    public void setLostStatus(boolean isLost){_isLost = isLost;}
    public void setItemLost(){_isLost = true;}
    public void setItemFound(){_isLost = false;}
    public void setItemLocation(Location location){
    	_latitude = location.getLatitude();
    	_longitude = location.getLongitude();
    }
    public void updateLatLng(LatLng latLng) {
    	_latitude = latLng.latitude;
    	_longitude = latLng.longitude;
    }
    public boolean belongToRegion(Region region){
    	
    	if (_UUID.equals(region.getProximityUUID()) && _major == region.getMajor() && _minor == region.getMinor())
    		return true;
    	
    	return false;
    }

    public Item(){}
 // Parcel
    public Item(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private void readFromParcel(Parcel in) {
        _id = in.readInt();
        _name = in.readString();
        _picture = in.readString();
        _description = in.readString();
        _latitude = in.readDouble();
        _longitude = in.readDouble();
        _trackEnabled = in.readByte() != 0;
        _isLost = in.readByte() != 0;
        _macAddress = in.readString();
        _UUID = in.readString();
        _major = in.readInt();
        _minor = in.readInt();
    }
    
    @Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(_id);
        dest.writeString(_name);
        dest.writeString(_picture);
        dest.writeString(_description);
        dest.writeDouble(_latitude);
        dest.writeDouble(_longitude);
        dest.writeByte((byte) (_trackEnabled ? 1 : 0));
        dest.writeByte((byte) (_isLost ? 1 : 0));
        dest.writeString(_macAddress);
        dest.writeString(_UUID);
        dest.writeInt(_major);
        dest.writeInt(_minor);
	}

    public static final Parcelable.Creator<Item> CREATOR =
            new Parcelable.Creator<Item>() {
                public Item createFromParcel(Parcel in) {
                    return new Item(in);
                }

                public Item[] newArray(int size) {
                    return new Item[size];
                }
            };


	public boolean matchWithBeacon(Beacon beacon) {

		if (beacon.getMacAddress().equals(_macAddress))
			return true;

		return false;
	}

}
