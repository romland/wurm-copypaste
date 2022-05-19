package com.friya.wurmonline.server.copypaste;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.mesh.MeshIO;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.Players;
import com.wurmonline.server.Point;
import com.wurmonline.server.Server;
import com.wurmonline.server.WurmId;
import com.wurmonline.server.behaviours.MethodsStructure;
import com.wurmonline.server.behaviours.TileRockBehaviour;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.DbBridgePart;
import com.wurmonline.server.structures.DbDoor;
import com.wurmonline.server.structures.DbFence;
import com.wurmonline.server.structures.DbFenceGate;
import com.wurmonline.server.structures.DbFloor;
import com.wurmonline.server.structures.DbWall;
import com.wurmonline.server.structures.NoSuchStructureException;
import com.wurmonline.server.structures.PlanBridgeMethods;
import com.wurmonline.server.structures.Proxy;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;
import com.wurmonline.server.structures.Wall;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.VirtualZone;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.BridgeConstants;
import com.wurmonline.shared.constants.BridgeConstants.BridgeType;
import com.wurmonline.shared.constants.StructureConstants.FloorMaterial;
import com.wurmonline.shared.constants.StructureConstants.FloorState;
import com.wurmonline.shared.constants.StructureConstants.FloorType;
import com.wurmonline.shared.constants.StructureConstantsEnum;
import com.wurmonline.shared.constants.StructureMaterialEnum;
import com.wurmonline.shared.constants.StructureStateEnum;
import com.wurmonline.shared.constants.StructureTypeEnum;
import com.wurmonline.mesh.Tiles.TileBorderDirection;

class Paste
{
	private static Logger logger = Logger.getLogger(Paste.class.getName());

	private static Paste instance = null;
	
	static Paste getInstance()
	{
		if(instance == null) {
			instance = new Paste();
		}

		return instance; 
	}

	
	public boolean area(Creature creator, int pasteStartX, int pasteStartY, Area area)
	{
		boolean ignoreRotation = false;		// possibly make a flag to pass in (for undo)
		
		Matrix m = Paste.getInstance().getSquareMatrix(area);
		if(ignoreRotation == false) {
			int rot = ClipboardSettings.getRotation(creator.getName());
			if(rot == 90) {
				m.rotate90();
			} else if(rot == 180) {
				m.rotate180();
			} else if(rot == 270) {
				m.rotate270();
			} else {
				// it's either 0 or ... wrong. So, no rotation.
			}
		}
		
		logger.info("Rotation: " + m.degrees + " degrees");
		logger.info("Rotated matrix:\r\n" + m.toString());

		
		if(area.attachBridges || area.attachBuildings || area.attachFences) {
			Delete.getInstance().fences(pasteStartX, pasteStartY, area.sizeX, area.sizeY);
			Delete.getInstance().structures(pasteStartX, pasteStartY, area.sizeX, area.sizeY);
		}
		
		HashMap<Long, Long> idTranslationTable = new HashMap<Long, Long>();
		
		boolean setTerrainHeights = true;
		boolean setTileTypes = true;

		TableData structureData = area.getStructures();

		// Paste landscape
		if(area.attachLandscape) {
			createLandscape(area, pasteStartX, pasteStartY, setTerrainHeights, setTileTypes, m);
		}

		// Paste structures
		for(int i = 0; i < structureData.getRowCount(); i++) {
			if(structureData.getFieldAsInt(i, "structuretype") == 0) {
				// building
				if(area.attachBuildings) {
					createBuilding(creator, area, pasteStartX, pasteStartY, structureData.getRow(i, m), m);
				}
				
			} else if(structureData.getFieldAsInt(i, "structuretype") == 1) {
				// bridge
				if(area.attachBridges) {
					long oldBridgeId = structureData.getFieldAsLong(i, "wurmid");
					long newBridgeId = createBridge(creator, area, pasteStartX, pasteStartY, structureData.getRow(i, m), m);
					logger.info("Old bridge: " + oldBridgeId + " new bridge: " + newBridgeId);
	
					if(newBridgeId > 0) {
						idTranslationTable.put(oldBridgeId, newBridgeId);
					}
				}
			} else {
				throw new RuntimeException("No idea what type of structure " + structureData.getFieldAsInt(i, "structuretype") + " is"); 
			}
		}
		
		// Paste fences
		if(area.attachFences) {
			createFences(area, pasteStartX, pasteStartY, m);
		}
		
		// Paste items
		if(area.attachItems) {
			createItems(area, pasteStartX, pasteStartY, idTranslationTable, m, creator);
		}
		
		return true;
	}
	
	
	// -------------------------------------

	/*
	- we need to get maxx/maxy when copying OR do an iteration over all buildtiles and see if any of them span outside original area
	- always create a square matrix for quick lookup (biggest axis determines)
	
	- able to rotate to set rotation to 0-4
	- create the matrix for this area (it depends on size): 90, 180 or 270 degrees
	
	- if house wall lives at 1,1 call getRotatedCoord with 1,1 and take values IN those array indices

	Create: Point getRotatedCoord(Point org)
	Create: getRotatedRow() which would do a replacement of data on that row, NOTE: Make it a copy and return that!
	
	
	
	 */
	Matrix getSquareMatrix(Area area)
	{
		TableData bts = area.getBuildTiles();

		int startX = 0;
		int startY = 0;
		int endX = area.sizeX;
		int endY = area.sizeY;
		
		int tileX = bts.getDataIndexByName("tilex");
		int tileY = bts.getDataIndexByName("tiley");

		// We need to do this because start and size might be lying in the area, some structures might span outside the copied area
		// because we do not allow for partial structures.
		for(int i = 0; i < bts.getRowCount(); i++) {
			if(startX > bts.getFieldAsInt(i, tileX))
				startX = bts.getFieldAsInt(i, tileX);
			
			if(startY > bts.getFieldAsInt(i, tileY)) 
				startY = bts.getFieldAsInt(i, tileY);
			
			if(endX < bts.getFieldAsInt(i, tileX)) 
				endX = bts.getFieldAsInt(i, tileX);
			
			if(endY < bts.getFieldAsInt(i, tileY)) 
				endY = bts.getFieldAsInt(i, tileY);
		}


		// We need one more because some points will undoubtedly need to reference outside the matrix. Every structure 
		// does an endx/endy, which might point to outside matrix if it sits on the edge.
		startX--;
		startY--;
		endX++;
		endY++;

//		logger.info("start: " + startX + "," + startY + " end: " + endX + "," + endY);

		int matrixSize = Math.max(endX - startX, endY - startY);
		
		logger.info("matrix size: " + matrixSize);
		
		Point matrix[][] = new Point[matrixSize][matrixSize];

		for(int r = 0; r < matrixSize; r++) {
			for(int c = 0; c < matrixSize; c++) {
				matrix[r][c] = new Point(r + startX, c + startY);
			}
		}
		
		// 2nd arg (base) is to make sure we base it on a proper origin (0,0) despite us using a matrix bigger than 
		// the actual area pasted. We might, however, do lookups at lower indices than 0,0.
		Matrix m = new Matrix(matrix, new Point(Math.abs(startX), Math.abs(startY)));
		
		logger.info("Default matrix:\r\n" + m.toString());
		
		return m;
	}

	
	// -------------------------------------
	
	private int getInt(HashMap<String, Object> d, String name) { return ((Double)d.get(name)).intValue(); }
	private short getShort(HashMap<String, Object> d, String name) { return ((Short)d.get(name)).shortValue(); }
	private long getLong(HashMap<String, Object> d, String name) { return ((Double)d.get(name)).longValue(); }
	private float getFloat(HashMap<String, Object> d, String name) { return ((Double)d.get(name)).floatValue(); }
	private byte getByte(HashMap<String, Object> d, String name) { return ((Double)d.get(name)).byteValue(); }
	private boolean getBool(HashMap<String, Object> d, String name) { return getInt(d, name) == 1 ? true : false; }
	private boolean isNull(HashMap<String, Object> d, String name) { return d.get(name) == null; }

	private boolean createItems(Area area, int pasteStartX, int pasteStartY, HashMap<Long, Long> idTranslationTable, Matrix m, Creature creator)
	{
		int pasteTile = Server.surfaceMesh.getTile(pasteStartX, pasteStartY);
		int heightDiff = Tiles.decodeHeight(pasteTile) - area.startAltitude;				// TODO: Forgot to get a relative height when copying (which is where this SHOULD be done)
		
		logger.info("Height diff: " + heightDiff);
		logger.info("paste height int  : " + Tiles.decodeHeight(pasteTile));
		logger.info("paste height float: " + Tiles.decodeHeightAsFloat(pasteTile));
		
		HashMap<Long, Long> newItemsWithOldParent = new HashMap<Long, Long>();
		
		TableData itd = area.getItems();
		long bridgeId = -10;

/*
			if(ret.containsKey("posx")) {
				logger.info("\tTODO: posx / posy " + getFloat(ret, "posx") + "," + getFloat(ret, "posy"));
			}
			
			if(ret.containsKey("rotation")) {
				logger.info("\tTODO: rotation " + getInt(ret, "rotation"));
			}
*/		

		float posX = 0;
		float posY = 0;
		float rotation = 0;

		//DecimalFormat df = new DecimalFormat("#.###");
		//df.setRoundingMode(RoundingMode.CEILING);

		for(int i = 0; i < itd.getRowCount(); i++) {
			//itd.outputRow(i);

			HashMap<String, Object> itemData = itd.getRow(i, m);
			
			long oldItemId = getLong(itemData, "wurmid");
			long oldParentId = getLong(itemData, "parentid");

			bridgeId = getLong(itemData, "onbridge");
			if(bridgeId > 0 && idTranslationTable.containsKey(bridgeId)) {
				//logger.info("Moved " + item.getName() + " onto new bridge!");
				bridgeId = idTranslationTable.get(bridgeId);
			}
			
			if(!isNull(itemData, "posx")) {
				posX = getFloat(itemData, "posx");
			}

			if(!isNull(itemData, "posy")) {
				posY = getFloat(itemData, "posy");
			}
			rotation = getFloat(itemData, "rotation");
/*
			// rotation fix
			int tmpX = (int)posX;
			int tmpY = (int)posY;
			Point p = m.getPoint(tmpX / 4, tmpY / 4);
			
			logger.info("\t rotation: " + m.degrees);
			logger.info("\t tmp pos: " + tmpX + "," + tmpY);
			logger.info("\t org pos: " + posX + "," + posY);

			// posX/Y now contains only the fraction
			posX -= tmpX;
			posY -= tmpY;

			// Rotate the fractions on the tile, as we don't have a look-up table for this.
			if(m.degrees == 90) {
				// TODO
			}

			logger.info("\t mod pos: " + posX + "," + posY);
			
			// Get the right rotated tile
			posX += p.getX();
			posY += p.getY();

			logger.info("\t fin pos: " + posX + "," + posY);

			rotation = (rotation + m.degrees) % 360;
*/

/*
float centerX = (area.sizeX * 4) / 2;
float centerY = (area.sizeY * 4) / 2;
double x1 = posX - centerX;
double y1 = posY - centerY;
double rads = Math.toRadians(m.degrees);

double x2 = x1 * Math.cos(rads) - y1 * Math.sin(rads);
double y2 = x1 * Math.sin(rads) + y1 * Math.cos(rads);
posX = (float)(x2 + centerX);
posY = (float)(y2 + centerY);
*/

/*			
[08:01:44 PM] INFO com.friya.wurmonline.server.copypaste.Paste:            Size: 40, 32
[08:01:44 PM] INFO com.friya.wurmonline.server.copypaste.Paste:          Center: 20.0, 16.0
[08:01:44 PM] INFO com.friya.wurmonline.server.copypaste.Paste:          Before: 29.752685546875, 8.6485595703125
[08:01:44 PM] INFO com.friya.wurmonline.server.copypaste.Paste:           After: 27.0, 25.0
[08:01:44 PM] INFO com.friya.wurmonline.server.copypaste.Paste:          Floats: 27.0, 25.0
 */

			Pointf cp = new Pointf((double)((double)area.sizeX * 4d) / 2d, (double)((double)area.sizeY * 4d) / 2d);
			Pointf pt = new Pointf(posX, posY);
			//logger.info("\t   Size: " + (area.sizeX * 4) + ", " + (area.sizeY * 4));
			//logger.info("\t Center: " + cp.x + ", " + cp.y);
			//logger.info("\t Before: " + pt.x + ", " + pt.y);

			rotatePoint(pt, cp, m.degrees);
			// TODO: why is the above turning into integers?!

			//logger.info("\t  After: " + pt.x + ", " + pt.y);
			
			posX = (float)pt.x;
			posY = (float)pt.y;

			//logger.info("\t Floats: " + posX + ", " + posY);
			
			// Ugh. I arrived at this through testing :( I thought this would work, so... not sure why I need to adjust this.
			if(area.sizeX % 2 == 0) {
				if(m.degrees == 90) {
					posX += 4;
				} else if(m.degrees == 180) {
					// ...
				} else if(m.degrees == 270) {
					posX -= 4;
				}
			} else {
				// odd sizes are no problem.
			}
			
			if(area.sizeY % 2 == 0) {
				if(m.degrees == 90) {
					posY += 4;
				} else if(m.degrees == 180) {
					posY += 8;
				} else if(m.degrees == 270) {
					posY += 4;
				}
			} else {
				// odd sizes are no problem.
			}
			
			// [00:42:05] Pasting 11x11 tiles...
			
/*			
			// get fraction, cet center to 0.5, pass in fractions to rotate point, add integers
			logger.info("\t orgX/Y: " + posX + "," + posY);

			int tmpX = (int)posX;
			int tmpY = (int)posY;
			logger.info("\t tmpX/Y: " + tmpX + "," + tmpY);
			
			Point p = m.getPoint((int)((float)posX / 4f), (int)((float)posY / 4f));
			logger.info("\t  p.x/y: " + p.getX() + "," + p.getY());
			
			Pointf pf = new Pointf(posX - tmpX, posY - tmpY);
			logger.info("\t pf.x/y: " + pf.getX() + "," + pf.getY());

			pf = rotatePoint(pf, new Pointf(0.5, 0.5), m.degrees);
			logger.info("\t pf.x/y: " + pf.getX() + "," + pf.getY());

			posX = (float)(p.getX() * 4) + (float)pf.getX();
			posY = (float)(p.getY() * 4) + (float)pf.getY();
			logger.info("\t posX/Y: " + posX + "," + posY);
*/
			// end of rotation fix
			
			rotation = (rotation + m.degrees) % 360;

			Item item = null;
			try {
				if(oldParentId != -10) {
					item = ItemFactory.createItem(
							getInt(itemData, "templateid"), 
							getFloat(itemData, "qualitylevel"), 
							getByte(itemData, "material"), 
							getByte(itemData, "rarity"), 
							(String)itemData.get("creator")
					);
				} else {
					item = ItemFactory.createItem(
							getInt(itemData, "templateid"),
							getFloat(itemData, "qualitylevel"),
							(pasteStartX * 4) + posX,
							(pasteStartY * 4) + posY,
						    rotation, 
						    true,												// TODO: hardcoded -- always on surface for now 
						    getByte(itemData, "rarity"),
						    bridgeId,
						    (String)itemData.get("creator")
					);
				}
			} catch(NoSuchTemplateException | FailedException e) {
				//throw new RuntimeException("Failed to create item", e);
				logger.log(Level.WARNING, "Failed to create item " + itemData.get("name"), e);
				continue;
			}

			idTranslationTable.put(oldItemId, item.getWurmId());

			item.creationDate = getLong(itemData, "creationdate");
			
			if(itemData.get("data1") != null) {
				item.setData1(getInt(itemData, "data1"));
			}
			
			if(itemData.get("data2") != null) {
				item.setData2(getInt(itemData, "data2"));
			}

			item.setAuxData(getByte(itemData, "auxdata"));

			item.setName((String)itemData.get("name"));
			item.setDescription((String)itemData.get("description"));
			item.setDamage(getFloat(itemData, "damage"));
			item.setMailed(getBool(itemData, "mailed"));
			item.setMailTimes(getByte(itemData, "mailtimes"));
			item.setHidden(getBool(itemData, "hidden"));

			if(!isNull(itemData, "posz")) {
				// TODO: I forgot to get a relative posZ when copying (we now calculate it at start of this method)
				float newPosZ = getFloat(itemData, "posz") + (heightDiff / 10f);
				item.setPosZ(newPosZ);
				//logger.info("'" + item.getName() + "' old posZ: " + getFloat(itemData, "posz") + " -- new posZ: " + newPosZ);
			}
			
			item.setOriginalQualityLevel(getFloat(itemData, "originalqualitylevel"));
			item.setLastMaintained(getInt(itemData, "lastmaintained"));
			item.setCreationState(getByte(itemData, "creationstate"));
			//item.setTemperature(getShort(itemData, "temperature"));
			item.setMaterial(getByte(itemData, "material"));
			item.setColor(getInt(itemData, "color"));
			item.setSettings(getInt(itemData, "settings"));

			if(!isNull(itemData, "realtemplate"))
				item.realTemplate = getInt(itemData, "realtemplate");

			item.setSizes(getInt(itemData, "sizex"), getInt(itemData, "sizey"), getInt(itemData, "sizez"));

			if(!isNull(itemData, "temperature"))
				item.setTemperature((short)getFloat(itemData, "temperature"));


			item.setWeight(getInt(itemData, "weight"), false, false);
			
			// If it has a parent, we need to revisit this item and update it to point to the new ID after a full loop of this.
			if(oldParentId != -10) {
				logger.info("item " + item.getName() + " had parentid: " + oldParentId);
				newItemsWithOldParent.put(item.getWurmId(), oldParentId);
			}
			
			/*
			 * TODO, fields not dealt with (v = done)
			 *  	capacity
			 *  v	parentid				(dealt with after this loop)
			 *  v	sizex
			 *  v	sizey
			 *  v	sizez
			 *  v	weight
			 *  	price
			 *  v	realtemplate
			 *  	female
			 *  	transferred
			 *  v	temperature			-- TODO: throws an exception due to nulls in data (we probably have more situations where this CAN happen)
			 *  v	settings			-- seems planted status goes in here?
			 *  
			 *  --- skipped ---
			 *  x	ownerid
			 *  x	lastownerid
			 *  x	zoneid
			 *  x	bless
			 *  x	enchant
			 *  x	banked
			 *  x	lockid
			 *  x	wornarmour
			 *  x	place				-- seems to be right/left hand
			 */

			// We need this to get an updated posZ, it seems
			item.updateIfGroundItem();
			logger.info("Pasted item: " + item.getWurmId());
		}
		
		
		for (Map.Entry<Long, Long> entry : newItemsWithOldParent.entrySet()) {
			long newItemId = entry.getKey();
			long oldParentId = entry.getValue();
			
			if(!idTranslationTable.containsKey(oldParentId))
				continue;
			
			try {
				Item child = Items.getItem(newItemId);
				long newParentId = idTranslationTable.get(oldParentId);
				Item parent = Items.getItem(newParentId);
				boolean res = parent.insertItem(child, true, false);
				
				logger.info("Inserted \"" + child.getName() + "\" into \"" + parent.getName() + "\" with res: " + res);
				
				if(parent.getName().equals("item hook")) {
					logger.info("Should send update to watchers about item: " + child.getName() + ", but skipping that for now and waiting for vanilla to add stuff-on-stuff...");
				}
				
			} catch (NoSuchItemException e) {
				logger.severe("Failed to insert item into item...");
			}
		}
		
		return true;
	}

	private Pointf rotatePoint(Pointf pt, Pointf center, double angleDeg)
	{
	    double angleRad = ((angleDeg/180)*Math.PI);
	    double cosAngle = Math.cos(angleRad );
	    double sinAngle = Math.sin(angleRad );
	    double dx = (pt.x-center.x);
	    double dy = (pt.y-center.y);

	    pt.x = center.x + (dx*cosAngle-dy*sinAngle);
	    pt.y = center.y + (dx*sinAngle+dy*cosAngle);
	    return pt;
	}


	private boolean createFences(Area area, int pasteStartX, int pasteStartY, Matrix m)
	{
		TableData ftd = area.getFences();
		
		try {
			for(int i = 0; i < ftd.getRowCount(); i++) {
				
				HashMap<String, Object> fenceData = ftd.getRow(i, m);
				
				int dir = getInt(fenceData, "dir");
				
				int currX = pasteStartX + getInt(fenceData, "tilex");
				int currY = pasteStartY + getInt(fenceData, "tiley");
				
				// Rotation fix
				if(m.degrees == 90 || m.degrees == 270) {
					dir = (dir == 2 ? 0 : 2);
					
					if(dir == 2) {
						currX++;
					}
				}
				// End of rotation fix
				
				Zone zone = Zones.getZone(currX, currY, true);
				// May2018: arg1 was just: getInt(fenceData, "type"),
				DbFence fence = new DbFence(
					StructureConstantsEnum.getEnumByValue( (short) getInt(fenceData, "type") ),
					currX,
					currY,
					getInt(fenceData, "heightoffset"),
					getFloat(fenceData, "currentql"),
					(dir == 0 ? TileBorderDirection.DIR_HORIZ : TileBorderDirection.DIR_DOWN),
					(int)zone.getId(),
					getInt(fenceData, "layer")
				);
		
				// May2018: WAS: fence.setState(getByte(fenceData, "state"));
				fence.setState(StructureStateEnum.getStateByValue(getByte(fenceData, "state")));
				
				/*
				 * TODO missing:
				 * 		settings
				 */
				fence.setLastUsed(getLong(fenceData, "lastmaintained"));
				fence.setOriginalQualityLevel(getFloat(fenceData, "originalql"));
				fence.setDamage(getFloat(fenceData, "damage"));
				// Wurm patch May2018: WAS: fence.setState(getByte(fenceData, "state"));
				fence.setState(StructureStateEnum.getStateByValue(getByte(fenceData, "state")));
				fence.setColor(getInt(fenceData, "color"));
				
				fence.save();
				zone.addFence(fence);

				// Wurm patch May2018: WAS: if(fence.getState() == 100 && (fence.isGate() || fence.isDoor())) {
				if(fence.getState() == StructureStateEnum.STATE_100_NEEDED && (fence.isGate() || fence.isDoor())) {
                    DbFenceGate gate = new DbFenceGate(fence);
                    gate.addToTiles();
                    VolaTile vtile = gate.getInnerTile();
                    Village village = vtile.getVillage();
                    if (village != null) {
                        village.addGate(gate);
                    } else {
                        vtile = gate.getOuterTile();
                        village = vtile.getVillage();
                        if (village != null) {
                            village.addGate(gate);
                        }    
                    }    
				}
			}
		} catch (IOException | NoSuchZoneException e) {
			throw new RuntimeException("Failed to create fence", e);
		}
		
		return true;
	}
	

	private boolean createLandscape(Area area, int pasteStartX, int pasteStartY, boolean setTerrainHeights, boolean setTileTypes, Matrix m)
	{
		MeshIO mesh = Server.surfaceMesh;
		short pasteStartAltitude = Tiles.decodeHeight(mesh.getTile(pasteStartX, pasteStartY));

		// look at: Terraforming.flattenImmediately()
		float minDirtDist = 1.0f;		// This is how much dirt there should be at a minimum when we go below rock layer
		
		short[][] alts = area.getAltitudes();

		// rotation fix
		int landscapeRotationModifierX = 0;
		int landscapeRotationModifierY = 0;
		
		if(m.degrees == 90) {
			landscapeRotationModifierY = 1;
		}
		// end rotation fix

		for(int x = 0; x < alts.length - landscapeRotationModifierX; x++) {
			for(int y = 0; y < alts[x].length - landscapeRotationModifierY; y++) {
				// Before rotation fix:
				//int currX = pasteStartX + x;
				//int currY = pasteStartY + y;

				// After rotation fix:
				Point p = m.getPoint(x, y);
				int currX = p.getX() + pasteStartX;
				int currY = p.getY() + pasteStartY;
				// end fix
				
				short modifiedAltitude = 0;

				if(setTerrainHeights) {
					modifiedAltitude = (short)(pasteStartAltitude + alts[x + landscapeRotationModifierX][y + landscapeRotationModifierY]);
	
					int currentRock = Server.rockMesh.getTile(currX, currY);
					float currentRockHeight = Tiles.decodeHeightAsFloat(currentRock);
					if (currentRockHeight > (modifiedAltitude - minDirtDist)) {
						Server.rockMesh.setTile(
								currX, 
								currY, 
								Tiles.encode(modifiedAltitude - minDirtDist, (byte)4, TileRockBehaviour.prospect(currX, currY, false))
						);
					}
				} else {
					// Leave height as-is
					modifiedAltitude = Tiles.decodeHeight(mesh.getTile(currX, currY));
				}

				byte spawnedType = 0;
				byte spawnedData = 0;

				if(setTileTypes) {
					spawnedType = (byte) (area.getTileTypes()[x][y] & 255);
					spawnedData = (byte) (area.getTileData()[x][y] & 255);
				} else {
					spawnedType = Tiles.decodeType(mesh.getTile(currX, currY));		// (byte)2 = tiletype grass
					spawnedData = Tiles.decodeData(mesh.getTile(currX, currY));
				}

				Server.surfaceMesh.setTile(currX, currY, Tiles.encode(modifiedAltitude, spawnedType, spawnedData));
			}
		}
		
		Players.getInstance().sendChangedTiles(pasteStartX, pasteStartY, alts.length + 1, alts[0].length + 1, true, false);
		Players.getInstance().sendChangedTiles(pasteStartX, pasteStartY, alts.length + 1, alts[0].length + 1, false, false);
		
		return true;
	}


	private long createBridge(Creature creator, Area area, int pasteStartX, int pasteStartY, HashMap<String, Object> sData, Matrix m)
	{
		long oldId = getLong(sData, "wurmid");

		TableData bridgeData = area.getBridgeParts();
		HashMap<String, Object> bd;
		
		int currX = 0, currY = 0;

		short pasteHeight = Tiles.decodeHeight(Server.surfaceMesh.getTile(pasteStartX, pasteStartY));
		int heightDifference = pasteHeight - area.startAltitude;

		// TODO: get size of x and y
// 
//		Point start = new Point(0, 0);
//		Point end = new Point(0, 0);

		// TODO: Not sure how bad this is ...
		creator.setStructure(null);

		try {
			for(int i = 0; i < bridgeData.getRowCount(); i++) {
				if(bridgeData.getFieldAsLong(i, bridgeData.getStructureIdColumnName()) != oldId) {
					continue;
				}
				
				bd = bridgeData.getRow(i, m);
				
				currX = pasteStartX + getInt(bd, "tilex"); 
				currY = pasteStartY + getInt(bd, "tiley"); 
/*
				if(i == 0) {
					start.setX(currX);
					start.setY(currY);
				} else {
					end.setX(currX);
					end.setY(currY);
				}
*/

				VolaTile t = Zones.getOrCreateTile(currX, currY, true);
				creator.addStructureTile(t, (byte) 1);		// 1 = bridge
				t.addBridge(creator.getStructure());
			}
		} catch (NoSuchStructureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}

		Structure structure = null;
		try {
			// This is essentially the private method: PlanBridgeMethods.finaliseBridge(creator, (String)sData.get("name"));
			structure = creator.getStructure();
			structure.makeFinal(creator, (String)sData.get("name"));
			creator.getStatus().setBuildingId(structure.getWurmId());

		} catch (NoSuchStructureException | NoSuchZoneException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}

/*
 * - clear db
 * - build four bridges, 2 wide (and name them):
 * 		- north -> south
 * 		- south -> north
 * 		- west -> east
 * 		- east -> west
 * 
 */

		try {
			for(int i = 0; i < bridgeData.getRowCount(); i++) {
				if(bridgeData.getFieldAsLong(i, bridgeData.getStructureIdColumnName()) != oldId) {
					continue;
				}
				
				bd = bridgeData.getRow(i, m);
	
				currX = pasteStartX + getInt(bd, "tilex"); 
				currY = pasteStartY + getInt(bd, "tiley"); 
				
	            VolaTile t = Zones.getOrCreateTile(currX, currY, true);
	            
	            int nExit = getInt(bd, "northexit");
	            int eExit = getInt(bd, "eastexit");
	            int sExit = getInt(bd, "southexit");
	            int wExit = getInt(bd, "westexit");
	            byte slope = getByte(bd, "slope");
	            byte type = getByte(bd, "type");
	            byte dir = getByte(bd, "dir");
	            int heightOffset = getInt(bd, "heightoffset");

	            
	            // LEADS:
	            //		Why is dir different on the same bridge?
	            //		Think: The 'dir' is per piece, not entire bridge -- do all pieces need rotation?
	            //		NOTE: dir DOES influence the direction in which it's rendered, after accidentally having 'dir += ' instead of 'dir = ', i saw an offending piece being rendered correctly
	            //		Is Landscape not pasted correctly when rotated? That WOULD affect bridges....
	            
	            // FURTHER:
	            //		"examine" invidiual bridge tiles and output long info
	            //		be able to change bridge tiles live

	            // rotation fix TESTING
	            if(m.degrees == 90) {
	            	// dir is NO LONGER handled by TableData

	            	if(false) {
	            		// This gets the heights right of bridge parts...
	            		dir = (byte)((dir + 2) % 8);
	            		
		            	// NOTE: This will probably only work on bridges that are width 1 -- FIXME
		            	HashMap<String, Object> tmpBd = bridgeData.getRow(bridgeData.getRowCount() - (i + 1), m);
		            	slope = getByte( tmpBd, "slope" );
		            	heightOffset = getInt( tmpBd, "heightoffset" );
	            	}
	            	
	            	if(true) {
	            		// Almost ... on some bridges
            			dir = (byte)((dir + 2) % 8);

	            		// TESTING TO FLIP AROUND THE BRACINGS **HARDCODED** BECAUSE FUCK KNOWS!
            			// This DOES flip the bracings correctly -- but not much else!
	            		if(dir == 6) dir = 2; else if(dir == 2) dir = 6;

	            		// NOTE: This will probably only work on bridges that are width 1 -- FIXME
		            	HashMap<String, Object> tmpBd = bridgeData.getRow(bridgeData.getRowCount() - (i + 1), m);
		            	slope = getByte( tmpBd, "slope" );
		            	heightOffset = getInt( tmpBd, "heightoffset" );
	            	}

	            	if(false) {
	            		// Noes
            			dir = (byte)((dir + 2) % 8);

	            		if(dir == 6) dir = 2; else if(dir == 2) dir = 6;

	            	}

	            	if(false) {
	            		// Noes
            			dir = (byte)((dir + 2) % 8);

	            		// TESTING TO FLIP AROUND THE BRACINGS **HARDCODED** BECAUSE FUCK KNOWS!
            			// This DOES flip the bracings correctly -- but not much else!
	            		if(dir == 6) dir = 2; else if(dir == 2) dir = 6;

	            		// NOTE: This will probably only work on bridges that are width 1 -- FIXME
		            	HashMap<String, Object> tmpBd = bridgeData.getRow(bridgeData.getRowCount() - (i + 1), m);
		            	slope = getByte( tmpBd, "slope" );
	            	}
	            	
	            	/*
            		if(slope < 0) {
            			slope = (byte)Math.abs(slope);
            		} else if(slope > 0) {
            			slope *= -1;
            		}
            		*/

	            	
            		//logger.info("\t switching dir " + dir);
            		//logger.info("\t switching slope " + slope);



/*
                    // See PlanBridgeMethods.planBridge() ~138
                    // dir 0 = n/s, else: w/e
	            	if(dir == 0) {
			            byte rdir = dir;
	                    if (parts[yy] == 97 || parts[yy] == 98) {           // a or b
	                        rdir = (byte)((dir + 4) % 8);
	                    }
	                    byte ndir = rdir;
	                    
	                    // getBridgeType(byte dir, byte part, int left, int right, int pos, byte direction) 
	                    //BridgeConstants.BridgeType bridgetype = PlanBridgeMethods.getBridgeType(dir, parts[yy], end.getX(), start.getX(), x, ndir);
	                    BridgeConstants.BridgeType bridgetype = BridgeType.fromByte(type);
	                    if (!bridgetype.isAbutment() && !bridgetype.isBracing() && PlanBridgeMethods.onLeft(dir, end.getX(), start.getX(), x, ndir)) {
	                        ndir = (byte)((ndir + 4) % 8); 
	                    }
	                    dir = ndir;
	
	            	} else {
	                    byte rdir = dir;
	                    if (parts[xx] == 65 || parts[xx] == 66) {           // A or B
	                        rdir = (byte)((dir + 4) % 8);
	                    }
	                    byte ndir = rdir;
	                    //BridgeConstants.BridgeType bridgetype = PlanBridgeMethods.getBridgeType(dir, parts[xx], end.getY(), start.getY(), y, ndir);
	                    BridgeConstants.BridgeType bridgetype = BridgeType.fromByte(type);
	                    if (!bridgetype.isAbutment() && !bridgetype.isBracing() && PlanBridgeMethods.onLeft(dir, end.getY(), start.getY(), y, ndir)) {
	                        ndir = (byte)((ndir + 4) % 8);
	                    }
	            	}
*/

	            	int tmp = eExit;
	            	int tmp2 = sExit;

	            	sExit = eExit;
	            	eExit = nExit;
	            	
	            	wExit = tmp2;
	            	nExit = tmp;

	            	//dir = (byte)Server.rand.nextInt(6);

	            	/*
	            	logger.info("\t Slope was: " + slope);
	            	if(slope >= 0) {
	            		slope *= -1;
	            	} else {
	            		slope = (byte)Math.abs(slope);
	            	}
	            	logger.info("\t Slope now: " + slope);
	            	*/
	            	
	            	/*
	            	// this only influences bridges width width greater than 1
	            	logger.info("TYPE: " + type);
	            	switch(type) {
	            	case 14: type = 15; break;
	            	case 15: type = 14; break;

	            	case 16: type = 17; break;
	            	case 17: type = 16; break;
	            	}
	            	*/
	            	// TODO: what are the various exits, they can have a range of values
	            }
	            
	            // end of rotation fix
	            
	            // public DbBridgePart(BridgeConstants.BridgeType floorType, int tilex, int tiley, int heightOffset, float qualityLevel, long structure, 
				//					   BridgeConstants.BridgeMaterial material, byte dir, byte slope, int aNorthExit, int aEastExit, int aSouthExit, int aWestExit) {
	           	DbBridgePart bridgePart;
				bridgePart = new DbBridgePart(
					BridgeConstants.BridgeType.fromByte(type), 
					currX, 
					currY,
					heightDifference + heightOffset, 
					getFloat(bd, "currentql"),
					creator.getStructure().getWurmId(), 
					BridgeConstants.BridgeMaterial.fromByte(getByte(bd, "material")), 
					dir,
					slope, 
					nExit,
					eExit, 
					sExit, 
					wExit,
					(bd.containsKey("roadtype") ? getByte(bd, "roadtype") : (byte)0),				// May2018: ADDED
					(bd.containsKey("layer") ? getInt(bd, "layer") : 0)								// May2018: ADDED
				);
				
				bridgePart.setBridgePartState(BridgeConstants.BridgeState.fromByte(getByte(bd, "state")));
				bridgePart.setLastUsed(getLong(bd, "lastmaintained"));

				// TODO: stagecount in DB is not copied over -- where do we set stageCount! and is it relevant?
				//this is NOT it: bridgePart.setMaterialCount(getInt(bd, "stagecount"));
				
	           	t.addBridgePart(bridgePart);
			}
			
		} catch (NoSuchStructureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
// OMG, how much other structure data are we missing?
		structure.setFinished((getInt(sData, "finished") == 1 ? true : false));
		Proxy.setFinalFinished(structure, (getInt(sData, "finfinished") == 1 ? true : false));

		return structure.getWurmId();
	}



	private boolean createBuilding(Creature creator, Area area, int pasteStartX, int pasteStartY, HashMap<String, Object> sData, Matrix m)
	{
		long oldId = getLong(sData, "wurmid");

		logger.info("Creating: " + sData.get("name") + " was id: " + oldId);

		Structure struct = Structures.createStructure(
				(byte)0, 	// 0 is building (1 is bridge)
				(String)sData.get("name"), 
				WurmId.getNextPlanId(),
				getInt(sData, "centerx") + pasteStartX, 
				getInt(sData, "centery") + pasteStartY, 
				(getInt(sData, "surfaced") == 1 ? true : false)
		);
	
		try {
			// Add buildtiles
			TableData bts = area.getBuildTiles();
			
			//logger.info("Got " + bts.getRowCount() + " buildtiles");
			
			for(int i = 0; i < bts.getRowCount(); i++) {
				if(bts.getFieldAsLong(i, bts.getStructureIdColumnName()) != oldId) {
					continue;
				}

				HashMap<String, Object> bt = bts.getRow(i, m);
				
				VolaTile vtile = Zones.getOrCreateTile(
					getInt(bt, "tilex") + pasteStartX,
					getInt(bt, "tiley") + pasteStartY,
					(getInt(bt, "layer") == 0 ? true : false)			// 0 = above ground
				);
				struct.addBuildTile(vtile, false);
				struct.clearAllWallsAndMakeWallsForStructureBorder(vtile);
				
				//logger.info("Added build tile: " + vtile);
			}
			
			// NOTE: This changes it from a plan ID to a structure ID
			struct.makeFinal(creator, (String)sData.get("name"));
			
			logger.info("New id: " + struct.getWurmId() + " was id: " + oldId);

			// Add walls
			TableData walls = area.getWalls();

			createBuildingWalls(pasteStartX, pasteStartY, oldId, struct, walls, m);

			// TODO: change to use getBool
			struct.setFinished((getInt(sData, "finished") == 1 ? true : false));
			Proxy.setFinalFinished(struct, (getInt(sData, "finfinished") == 1 ? true : false));
			
			// Add floors (and roofs)
			TableData floors = area.getFloors();
			for(int i = 0; i < floors.getRowCount(); i++) {
				if(floors.getFieldAsLong(i, floors.getStructureIdColumnName()) != oldId) {
					continue;
				}
				
				HashMap<String, Object> floorData = floors.getRow(i, m);

				// DbFloor(StructureConstants.FloorType floorType, int tilex, int tiley, int heightOffset, float qualityLevel, long structure, StructureConstants.FloorMaterial material, int layer)
				DbFloor floor = new DbFloor(
					FloorType.fromByte(getByte(floorData, "type")),
					getInt(floorData, "tilex") + pasteStartX,
					getInt(floorData, "tiley") + pasteStartY,
					getInt(floorData, "heightoffset"),
					getFloat(floorData, "currentql"),
					struct.getWurmId(),
					FloorMaterial.fromByte(getByte(floorData, "material")),
					getInt(floorData, "layer")
				);
				
				floor.setFloorState(FloorState.fromByte(getByte(floorData, "state")));
				floor.setOriginalQualityLevel(getFloat(floorData, "originalql"));
				floor.setDamage(getFloat(floorData, "damage"));
				floor.setLayer(getByte(floorData, "layer"));
				floor.setLastUsed(getLong(floorData, "lastmaintained"));

				/*
				 * TODO missing:
				 * 		color
				 * 		slope
				 * 		stagecount
				 * 		settings
				 */
				
				VolaTile vt = getTileInStruct(struct, getInt(floorData, "tilex") + pasteStartX, getInt(floorData, "tiley") + pasteStartY, getInt(floorData, "layer"));

				if(vt == null) {
					throw new RuntimeException("Failed to create floor/roof, could not get VolaTile");
				}

				// possible values: 0, 2, 4, 6
				byte dir = getByte(floorData, "dir");
				if(dir != 0) {
					// this will trigger an update and a save
					floor.rotate(dir);
				} else {
					vt.addFloor(floor);
					floor.save();
				}
			}

			struct.setName((String)sData.get("name"), true);

		} catch (NoSuchZoneException | IOException e) {
			throw new RuntimeException("Error creating building", e);
		}

		return true;
	}

	
	private Wall getPlannedWall(Wall[] plannedWalls, int startX, int startY, int endX, int endY, int heightOffset)
	{
		for(Wall w : plannedWalls) {
			if(w.getStartX() == startX && w.getStartY() == startY && w.getEndX() == endX && w.getEndY() == endY && w.getHeight() == heightOffset) {
				return w;
			}
		}
		
		return null;
	}
	
	// /load Friyanouce-Building_Freds_-1491875610717.friyaclip
	private void createBuildingWalls(int pasteStartX, int pasteStartY, long oldId, Structure struct, TableData walls, Matrix m) throws IOException
	{
		getFlawedWalls(struct);
		
		Wall[] plannedWalls = struct.getWalls();
		Wall plannedWall = null;

		for(int i = 0; i < walls.getRowCount(); i++) {
			if(walls.getFieldAsLong(i, walls.getStructureIdColumnName()) != oldId) {
				continue;
			}

			HashMap<String, Object> wallData = walls.getRow(i, m);

			VolaTile vt = getTileInStruct(struct, getInt(wallData, "tilex") + pasteStartX, getInt(wallData, "tiley") + pasteStartY, getInt(wallData, "layer"));

			int degrees = m.degrees;
    		Point p = new Point(getInt(wallData, "startx") + pasteStartX, getInt(wallData, "starty") + pasteStartY); 
    		Point p2 = new Point(getInt(wallData, "endx") + pasteStartX, getInt(wallData, "endy") + pasteStartY); 

    		int heightOffset = getInt(wallData, "heightoffset");
    		
   			plannedWall = getPlannedWall(plannedWalls, p.getX(), p.getY(), p2.getX(), p2.getY(), heightOffset);
   			
   			logger.info("plannedWall: " + plannedWall);
    		
    		// rotation fix
    		// this works only? for 90 degrees now!
    		if(degrees != 0) {
    			/*
    			 * These measurements are at 0 degrees -- this is HOW WE MAKE WALLS
    			 * 
    			 * North wall offsets from tile:										name					type		tile			start			end
    			 * 	0,0 | 1,0			w/e, n/s	=	west-north -> east-north	=	stone arch wall		=	4		"1134"	"348"	"1134"	"348"	"1135"	"348"
    			 * 
    			 * East wall:
    			 * 	1,0 | 1,1						=	east-north -> east-south	=	stone door			=	2		"1134"	"348"	"1135"	"348"	"1135"	"349"
    			 * 
    			 * South wall:
    			 * 	0,1 | 1,1						=								=	stone window		=	1		"1134"	"348"	"1134"	"349"	"1135"	"349"
    			 * 
    			 * West wall:
    			 * 	0,0 | 0,1						=								=	stone wall			=	0		"1134"	"348"	"1134"	"348"	"1134"	"349"
    			 * 
    			 */
        		if(p.getY() == p2.getY()) {
        			// what we want to draw is horizontal; a north or south wall
        			logger.info("\tIs horizontal");
        			
           			//if(p2.getX() <= vt.tilex) {			// makes two south walls
           			//if(p.getX() <= vt.tilex) {			// makes two south walls
           			//if(p.getY() > vt.tiley) {				// works on four walls -- at 90 rotation -- not at 180, then we get two north	** 0,0 -> -1,0 AND 0,-1 -> -1, -1
           			//if(p.getX() > vt.tilex) {				// 2 north walls 0,1 -> -1,1	|	0,0 -> -1,0
           			if(p.getY() > vt.tiley) {				// 
        				logger.info("\twants to make an SOUTH wall");
        				p.setX(vt.tilex + 0);
        				p.setY(vt.tiley + 1);
        				p2.setX(vt.tilex + 1);
        				p2.setY(vt.tiley + 1);
        			} else {
        				logger.info("\twants to make an NORTH wall");
        				p.setX(vt.tilex + 0);
        				p.setY(vt.tiley + 0);
        				p2.setX(vt.tilex + 1);
        				p2.setY(vt.tiley + 0);
        			}

        		} else {
    				// what we want to draw is vertical; an east or west wall
        			logger.info("Is vertical - 90 degrees");

/*
 * 127
[01:58:32 AM] INFO com.friya.wurmonline.server.copypaste.TableData:     TRANSLATE tilex/y to 0, 0
[01:58:32 AM] INFO com.friya.wurmonline.server.copypaste.TableData:     TRANSLATE startx/y to 0, 0
[01:58:32 AM] INFO com.friya.wurmonline.server.copypaste.TableData:     TRANSLATE endx/y to 0, 1
[01:58:32 AM] INFO com.friya.wurmonline.server.copypaste.Paste: Is vertical - 90 degrees
[01:58:32 AM] INFO com.friya.wurmonline.server.copypaste.Paste:         should make a WEST wall
-----
[01:58:32 AM] INFO com.friya.wurmonline.server.copypaste.TableData:     TRANSLATE tilex/y to 0, 0
[01:58:32 AM] INFO com.friya.wurmonline.server.copypaste.TableData:     TRANSLATE startx/y to -1, 0
[01:58:32 AM] INFO com.friya.wurmonline.server.copypaste.TableData:     TRANSLATE endx/y to -1, 1
[01:58:32 AM] INFO com.friya.wurmonline.server.copypaste.Paste: Is vertical - 90 degrees
[01:58:32 AM] INFO com.friya.wurmonline.server.copypaste.Paste:         should make a WEST wall
 */
        			if(p2.getX() >= vt.tilex) {
        				logger.info("\twants to make an EAST wall");
        				p.setX(vt.tilex + 1);
        				p.setY(vt.tiley + 0);
        				p2.setX(vt.tilex + 1);
        				p2.setY(vt.tiley + 1);
        			} else {
        				logger.info("\twants to make a WEST wall");
        				p.setX(vt.tilex + 0);
        				p.setY(vt.tiley + 0);
        				p2.setX(vt.tilex + 0);
        				p2.setY(vt.tiley + 1);
        			}
        		}
			}
			// end rotation fix
    		
    		// May2018: getByte(wallData, "type"), CHANGED TO: StructureTypeEnum.getTypeByINDEX( (int)getByte(wallData, "type") )
    		//			NOTE: I expected to have a ByValue method in there, but only see ByINDEX
    		// May2018: getByte(wallData, "material"), CHANGED TO: StructureMaterialEnum.getEnumByMaterial( getByte(wallData, "material") ), 
			DbWall plan;
			
			if(plannedWall == null) {
				plan = new DbWall(
					StructureTypeEnum.getTypeByINDEX( (int)getByte(wallData, "type") ), 
	        		vt.getTileX(), 
	        		vt.getTileY(), 
	        		p.getX(),	//getInt(wallData, "startx") + pasteStartX, 
	        		p.getY(),	//getInt(wallData, "starty") + pasteStartY, 
	        		p2.getX(),	//getInt(wallData, "endx") + pasteStartX, 
	        		p2.getY(),	//getInt(wallData, "endy") + pasteStartY, 
	        		getFloat(wallData, "currentql"), 
	        		struct.getWurmId(), 
	        		StructureMaterialEnum.getEnumByMaterial( getByte(wallData, "material") ),
	        		getBool(wallData, "isindoor"),
	        		heightOffset, 
	        		struct.getLayer()
		        );
			
				// May2018: WAS: plan.setState((byte) 1); 
		        plan.setState(StructureStateEnum.getStateByValue( (byte) 1 ));
			} else {
				plan = (DbWall)plannedWall;
				plan.setType(StructureTypeEnum.getTypeByINDEX( (int)getByte(wallData, "type") ));
				plan.setTile(vt.getTileX(), vt.getTileY());
				plan.x1 = p.getX();
				plan.y1 = p.getY();
				plan.x2 = p2.getX();
				plan.y2 = p2.getY();
				plan.setQualityLevel(getFloat(wallData, "currentql"));
				plan.setStructureId(struct.getWurmId());
				plan.setMaterial(StructureMaterialEnum.getEnumByMaterial( getByte(wallData, "material") ));
				plan.setIndoor(getBool(wallData, "isindoor"));
				plan.setHeightOffset(heightOffset);
				// Layer should be okay...
			}
	        vt.addWall(plan);
	
			plan.setWallOrientation(getBool(wallData, "wallorientation"));
			plan.lastUsed = getLong(wallData, "lastmaintained");
			plan.setOriginalQualityLevel(getFloat(wallData, "originalql"));
			plan.setDamage(getFloat(wallData, "damage"));
			plan.setColor(getInt(wallData, "color"));
			
			// NOTE: outerwall is ALWAYS 0 on e.g. Wyvern PVE
			// NOTE: settings is permissions
			// NOTE: wallorientation seem to always be 0, but easy enough to copy over
	
			// May2018: WAS: plan.setState(getByte(wallData, "state"));
			plan.setState(StructureStateEnum.getStateByValue(getByte(wallData, "state")));
	
			vt.updateWall(plan);
			
			// TODO: do we need to visit Doors table for some data?
			if(plan.isDoor()) {
				DbDoor door = new DbDoor(plan);
				door.setStructureId(struct.getWurmId());
				struct.addDoor(door);
				door.save();
				door.addToTiles();
			}
		}

		getFlawedWalls(struct);
	}
	
	private void getFlawedWalls(Structure s)
	{
		// /load Friyanouce-Building_Freds_-1491875610717.friyaclip
		//
		// BOTTOM FLOOR WALLS NOT STICKING? This is the temporary solution:
		// this problem can be spotted by doubleclicking a bottom-floor just after pasting -- if its state is INITIALIZED -- it will fail later on (duplicate walls are in DB)
		//				delete from walls where type = 127 and originalql = 10.0 and currentql = 10.0 and outerwall = 0 and state = 1 and material = 0;
		Wall[] walls = s.getWalls();
		int i = 0;
		for(Wall w : walls) {
			logger.info("FlawedWall? "
					+ "\ti: " + (i++)
					+ "\tfinalState: " + w.getFinalState() 
					+ "\tstate: " + w.state
					+ "\tql: " + w.getCurrentQualityLevel()
			);
			
			// HUH this worked after reboot, so if we could update the internal structure, we might be good...
			if(w.state == StructureStateEnum.INITIALIZED) {
				logger.info("\tdelete above?");
				// a test to see if shit sticks!
//				w.delete();
			}
		}
/*		
		logger.info("----------- debugWalls -----------");
		i = 0;
		for(Wall w : debugWalls) {
			logger.info("FlawedWall? "
					+ "\ti: " + (i++)
					+ "\tfinalState: " + w.getFinalState() 
					+ "\tstate: " + w.state
					+ "\tql: " + w.getCurrentQualityLevel()
			);
		}
*/
	}
	
	
	private VolaTile getTileInStruct(Structure struct, int tilex, int tiley, int layer)
	{
		for(VolaTile vt : struct.getStructureTiles()) {
			if(vt.tilex == tilex && vt.tiley == tiley && vt.getLayer() == layer) {
				return vt;
			}
		}

		return null;
	}
}
