package com.friya.wurmonline.server.copypaste;

import java.util.logging.Logger;

import com.wurmonline.server.Point;
import com.wurmonline.server.Server;
import com.wurmonline.server.Servers;

class Area
{
	private static Logger logger = Logger.getLogger(Area.class.getName());

	int areaVersion = 1;
	String contentHash = "";

	long timestamp = 0;
	
	// If this is set to true, it will not save a copy to disk when copied
	boolean isTemporary = false;
	
	// TODO, populate these:
	String serverIP = "";
	String serverName = "";
	String owner = "";
	String steamId = "";
	String mayor = "";
	String deedName = "";		// get start location? (check perimeter even)
	boolean ownerIsGM = false;
	boolean isPublicServer = false;
	boolean isTamperProtected = false;
	
	// By default, paste these for this 'copy'
	boolean attachBridges = true;
	boolean attachBuildings = true;
	boolean attachFences = true;
	boolean attachLandscape = true;
	boolean attachTrees = true;
	boolean attachItems = true;

	// These are the original positions. Positions in structures below are stored as relative to origin (0,0), not startX/Y.
	short startAltitude = 0;
	int startX = 0;
	int startY = 0;
	int sizeX = 0;
	int sizeY = 0;

	private final TableData bridgeParts = new TableData("bridgeparts", "structure");
	private final TableData buildTiles = new TableData("buildtiles", "structureid");
	private final TableData doors = new TableData("doors", "structure");
	private final TableData floors = new TableData("floors", "structure");
	private final TableData structures = new TableData("structures", "wurmid");
	private final TableData walls = new TableData("walls", "structure");
	private final TableData fences = new TableData("fences", null);
	private final TableData items = new TableData("items", null);

	private short[][] altitudes = null;
	private byte[][] tileTypes = null;
	private byte[][] tileData = null;


	void attachAll()
	{
		attachBridges = true;
		attachBuildings = true;
		attachFences = true;
		attachLandscape = true;
		attachItems = true;
	}

	void attachNone()
	{
		attachBridges = false;
		attachBuildings = false;
		attachFences = false;
		attachLandscape = false;
		attachTrees = false;
		attachItems = false;
	}

	
	/**
	 * Dumb fix because I introduced boolean flags to indicate what should be pasted from this file. All nice and 
	 * dandy, except that I had a lot of saved areas that did not have these booleans and they are by default set
	 * to false.
	 * 
	 * This fixes that.
	 */
	void attachFix()
	{
		if(!attachBridges && !attachBuildings && !attachFences && !attachLandscape && !attachTrees && !attachItems) {
			attachAll();
		}
	}
	
	Area(int startX, int startY, int sizeX, int sizeY, short startAltitude)
	{
		this.startAltitude = startAltitude;
		this.startX = startX;
		this.startY = startY;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		
		timestamp = System.currentTimeMillis();
		serverIP = Servers.localServer.EXTERNALIP;
		serverName = Servers.getLocalServerName();
	}

	TableData getBridgeParts() {
		return bridgeParts;
	}

	TableData getBuildTiles() {
		return buildTiles;
	}

	TableData getDoors() {
		return doors;
	}

	TableData getFloors() {
		return floors;
	}

	TableData getStructures() {
		return structures;
	}

	TableData getWalls() {
		return walls;
	}

	TableData getFences() {
		return fences;
	}

/*
	TableData getGates() {
		return gates;
	}
*/

	Point getAbsolutePos(int tilex, int tiley)
	{
		return new Point(tilex + startX, tiley + startY);
	}

	short[][] getAltitudes() {
		return altitudes;
	}

	void setAltitudes(short[][] altitudes) {
		this.altitudes = altitudes;
	}

	byte[][] getTileTypes() {
		return tileTypes;
	}

	void setTileTypes(byte[][] tileTypes) {
		this.tileTypes = tileTypes;
	}

	byte[][] getTileData() {
		return tileData;
	}

	void setTileData(byte[][] tileData) {
		this.tileData = tileData;
	}

	TableData getItems() {
		return items;
	}

}
