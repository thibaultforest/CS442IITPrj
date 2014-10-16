package com.example.luguaggetracker;

import java.util.ArrayList;

import com.estimote.sdk.Beacon;

public class Params {

	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;
	// Update frequency in seconds
	private static final int UPDATE_INTERVAL_IN_SECONDS = 30;
	// Update frequency in milliseconds
	public static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
	        * UPDATE_INTERVAL_IN_SECONDS;
	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
	// A fast frequency ceiling in milliseconds
	public static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND
	        * FASTEST_INTERVAL_IN_SECONDS * 5;
	
	// Minimum accountable distance in meters
	public static float MAXIMUM_DISTANCE = 15;

	public static ArrayList<Beacon> Beacons = new ArrayList<Beacon>();
	
	
	
	/**
	 * Suppress default constructor for noninstantiability
	 */
	private Params() {
	    throw new AssertionError();
	}
}
