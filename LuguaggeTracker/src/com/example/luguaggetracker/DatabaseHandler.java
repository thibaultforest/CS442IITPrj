package com.example.luguaggetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import com.estimote.sdk.Beacon;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static DatabaseHandler singleton;
	
	public static DatabaseHandler getInstance(final Context context) {
		if (singleton == null)
			singleton = new DatabaseHandler(context);
		
		return singleton;
	}
	
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "ItemsDataBase";//"TestDB";

    // Items table name
    private static final String TABLE_ITEMS = "items";

    // Items Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_PICTURE = "picture";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_TRACK = "isTracked";
    private static final String KEY_LOST = "isLost";
    private static final String KEY_MACADDRESS = "macAddress";
    private static final String KEY_UUID = "UUID";
    private static final String KEY_MAJOR = "major";
    private static final String KEY_MINOR = "minor";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//        context.deleteDatabase(DATABASE_NAME);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_ITEMS_TABLE = "CREATE TABLE " + TABLE_ITEMS + "("
                + KEY_ID 			+ " INTEGER PRIMARY KEY," 
        		+ KEY_NAME 			+ " TEXT,"
                + KEY_PICTURE 		+ " TEXT,"
        		+ KEY_DESCRIPTION 	+ " TEXT,"
                + KEY_LATITUDE 		+ " DOUBLE,"
                + KEY_LONGITUDE 	+ " DOUBLE,"
                + KEY_TRACK 		+ " INTEGER,"
                + KEY_LOST 			+ " INTEGER,"
                + KEY_MACADDRESS 	+ " TEXT," 
        		+ KEY_UUID 			+ " TEXT," 
                + KEY_MAJOR 		+ " INTEGER," 
                + KEY_MINOR 		+ " INTEGER"
                +")";
        db.execSQL(CREATE_ITEMS_TABLE);
    }

    // Upgrade database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);

        // Create tables again
        onCreate(db);
    }

    // Add new item
    void addItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, item.getName());
        values.put(KEY_PICTURE, item.getPicture());
        values.put(KEY_DESCRIPTION, item.getDescription());
        values.put(KEY_LATITUDE, item.getLatitude());
        values.put(KEY_LONGITUDE, item.getLongitude());
        if (item.getTrackEnabled())	// SQLite doesn't handle boolean
        	values.put(KEY_TRACK, 1);// true
        else
        	values.put(KEY_TRACK, 0);// false
        if (item.getLostStatus())
        	values.put(KEY_LOST, 1);
        else
        	values.put(KEY_LOST, 0);
        values.put(KEY_MACADDRESS, item.getMacAddress());
        values.put(KEY_UUID, item.getUUID());
        values.put(KEY_MAJOR, item.getMajor());
        values.put(KEY_MINOR, item.getMinor());
        
        // Inserting Row
        assert db != null;
        long id = db.insert(TABLE_ITEMS, null, values);
        if(id != -1)
            item.setId((int) id);
        db.close(); // Closing database connection
    }

    // Getting All Items
    public ArrayList<Item> getAllItems() {
        ArrayList<Item> itemList = new ArrayList<Item>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_ITEMS;
        
        SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
            	boolean isTracked = true;
            	if (cursor.getInt(6) == 0)
            		isTracked = false;
            	boolean iLost = true;
            	if (cursor.getInt(7) == 0)
            		iLost = false;
            	Item item = new Item(
                        Integer.parseInt(cursor.getString(0)), // id
                        cursor.getString(1), 	// name
                        cursor.getString(2), 	// picture
                        cursor.getString(3), 	// description
                        cursor.getDouble(4), 	// latitude
                        cursor.getDouble(5),	// longitude
                        isTracked,				// isTracked
                        iLost,					// iLost
                        cursor.getString(8),	// macAddress
                        cursor.getString(9),	// UUID
                        cursor.getInt(10),		// major
                        cursor.getInt(11)		// minor
                );
                // Adding item to list
                itemList.add(item);
            } while (cursor.moveToNext());
        }

        return itemList;
    }

    // Updating single item
    public int updateItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, item.getId());
        values.put(KEY_NAME, item.getName());
        values.put(KEY_PICTURE, item.getPicture());
        values.put(KEY_DESCRIPTION, item.getDescription());
        values.put(KEY_LATITUDE, item.getLatitude());
        values.put(KEY_LONGITUDE, item.getLongitude());
        if (item.getTrackEnabled())
        	values.put(KEY_TRACK, 1);
        else
        	values.put(KEY_TRACK, 0);
        if (item.getLostStatus())
        	values.put(KEY_LOST, 1);
        else
        	values.put(KEY_LOST, 0);
        values.put(KEY_MACADDRESS, item.getMacAddress());
        values.put(KEY_UUID, item.getUUID());
        values.put(KEY_MAJOR, item.getMajor());
        values.put(KEY_MINOR, item.getMinor());
        

        // updating row
        assert db != null;
        return db.update(TABLE_ITEMS, values, KEY_ID + " = ?",
                new String[] { String.valueOf(item.getId()) });
    }

    // Deleting single item
    public void deleteItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;
        db.delete(TABLE_ITEMS, KEY_ID + " = ?", new String[]{String.valueOf(item.getId())});
        db.close();
    }

    public void setAllItemToLost(ArrayList<Item> itemList) {
        SQLiteDatabase db = this.getWritableDatabase();
        assert db != null;
        ContentValues value = new ContentValues();
        value.put(KEY_LOST, 1);
        db.update(TABLE_ITEMS, value, KEY_LOST + " = " + 0, null);
        db.close();
    }
    
    // Getting items Count
    public int getItemsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_ITEMS;
        SQLiteDatabase db = this.getReadableDatabase();
        assert db != null;
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();

        return count;
    }

	public boolean beaconAlreadyExist(Beacon associatedBeacon) {

		String query = "SELECT * FROM " 
							+ TABLE_ITEMS 
							+ " WHERE " + KEY_MACADDRESS + " = \"" + associatedBeacon.getMacAddress() + "\"";
        SQLiteDatabase db = this.getReadableDatabase();
        assert db != null;
        Cursor cursor = db.rawQuery(query, null);
        int count = cursor.getCount();
        cursor.close();
        if (count > 0)
        	return true;
		return false;
	}
}
