package com.friya.wurmonline.server.copypaste;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.mesh.Tiles.TileBorderDirection;
import com.wurmonline.server.Items;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.MethodsStructure;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.Fence;
import com.wurmonline.server.structures.NoSuchStructureException;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

// NOTE:	Do not throw anything into Undo buffer in this class. Delete is also called when cutting and pasting, and they will use the Undo buffer.
//			If the "Delete action is used, then just throw things into Undo buffer in the action (before calling anything in here).
class Delete implements ItemGroups
{
	private static Logger logger = Logger.getLogger(Delete.class.getName());

	private static Delete instance = null;
	
	static Delete getInstance()
	{
		if(instance == null) {
			instance = new Delete();
		}

		return instance; 
	}

	boolean area(int startX, int startY, int sizeX, int sizeY)
	{
		tiletypes(startX, startY, sizeX, sizeY);
		fences(startX, startY, sizeX, sizeY);
		structures(startX, startY, sizeX, sizeY);
		decorations(startX, startY, sizeX, sizeY);

		return true;
	}

	void decorations(int startX, int startY, int sizeX, int sizeY)
	{
		// delete decorations/furniture (we want to do this after items "sank" to the ground -- that is structures were deleted before)
		for(int x = 0; x < sizeX; x++) {
			for(int y = 0; y < sizeY; y++) {
				int currX = startX + x;
				int currY = startY + y;
				
				VolaTile tile = Zones.getTileOrNull(currX, currY,true);
				
				if(tile == null) {
					continue;
				}
				
				Item[] items = tile.getItems();
				for(Item i : items) {
					if(Arrays.stream(decorationsGroup).anyMatch(j -> j == i.getTemplateId()) || Arrays.stream(furnitureGroup).anyMatch(j -> j == i.getTemplateId())) {
						Items.destroyItem(i.getWurmId());
					}
				}
			}
		}
	}

	void structures(int startX, int startY, int sizeX, int sizeY)
	{
		// delete structures
		HashSet<Long> structureData = Copy.getInstance().getStructures(startX, startY, sizeX, sizeY);

		for(long id : structureData) {
			try {
				Structure structure = Structures.getStructure(id);
				structure.totallyDestroy();
	
			//} catch (NoSuchStructureException | IllegalStateException e) {
			} catch (Exception e) {
				//throw new RuntimeException("Failed to delete structure", e);
				logger.log(Level.SEVERE, "Failed to delete structure " + id, e);
			}
		}
	}

	void fences(int startX, int startY, int sizeX, int sizeY)
	{
		// delete fences
		// add one more tile outside sizeX/Y
		for(int x = 0; x < sizeX + 1; x++) {
			for(int y = 0; y < sizeY + 1; y++) {
		    	Fence f = MethodsStructure.getFenceAtTileBorderOrNull(startX + x, startY + y, TileBorderDirection.DIR_HORIZ, 0, true);
		    	if(f != null) {
		    		f.destroy();
		    	}
		    	
		    	f = MethodsStructure.getFenceAtTileBorderOrNull(startX + x, startY + y, TileBorderDirection.DIR_DOWN, 0, true);
		    	if(f != null) {
		    		f.destroy();
		    	}
			}
		}
	}

	void tiletypes(int startX, int startY, int sizeX, int sizeY)
	{
		// replace non-natural tiletypes (pavements) with ... grass?
		// add one more tile outside sizeX/Y
		for(int x = 0; x < sizeX + 1; x++) {			//
			for(int y = 0; y < sizeY + 1; y++) {
				int currX = startX + x;
				int currY = startY + y;
				
				int tile = Server.surfaceMesh.getTile(currX, currY);
				
				short modifiedAltitude = Tiles.decodeHeight(tile);
				byte type = Tiles.decodeType(tile);
				byte data = Tiles.decodeData(tile);

				// Other than trees etc, also leave some strange tiles alone (such as minedoors)
				if(type == 1 || Tiles.isSolidCave(type) || Tiles.isReinforcedCave(type) || Tiles.isOreCave(type) || Tiles.isMineDoor(type) || Tiles.isEnchanted(tile) || Tiles.isVisibleMineDoor(type) || Tiles.isBush(type) || Tiles.isTree(type)) {
					continue;
				}

				// Force to 2 = grass, TODO: Perhaps make it mycelium if in HOTS territory
				type = 2;
				
				Server.surfaceMesh.setTile(currX, currY, Tiles.encode(modifiedAltitude, type, data));
			}
		}
		
		Players.getInstance().sendChangedTiles(startX, startY, sizeX + 1, sizeY + 1, true, false);
		Players.getInstance().sendChangedTiles(startX, startY, sizeX + 1, sizeY + 1, false, false);
	}
}
