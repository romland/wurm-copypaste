package com.friya.wurmonline.server.copypaste;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.wurmonline.mesh.MeshIO;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.DbConnector;
import com.wurmonline.server.Server;
import com.wurmonline.server.utils.DbUtilities;

class Copy implements ItemGroups
{
	private static Logger logger = Logger.getLogger(Copy.class.getName());
	private static Copy instance = null;

	private static String structurePartTables[] = new String[]{"bridgeparts", "floors", "walls"};
	private static String relativizeTileFields[] = new String[]{"tilex", "tiley", "startx", "starty", "endx", "endy", "centerx", "centery", "posx", "posy"};
	private static String relativizeExactPosFields[] = new String[]{"posx", "posy"};
	

	static Copy getInstance()
	{
		if(instance == null) {
			instance = new Copy();
		}

		return instance; 
	}


	Area area(int startX, int startY, int sizeX, int sizeY)
	{
		// x,  w <-> e
		// y,  n <-> s

		// get height of nw corner of startx/starty (that's our 'normal')
		MeshIO mesh = Server.surfaceMesh;
		short startAltitude = Tiles.decodeHeight(mesh.getTile(startX, startY));

		Area area = new Area(startX, startY, sizeX, sizeY, startAltitude);

		HashSet<Long> structureIds = getStructures(startX, startY, sizeX, sizeY);

		setStructurePartsToTableData(startX, startY, startAltitude, structureIds, area.getBridgeParts());
		setStructurePartsToTableData(startX, startY, startAltitude, structureIds, area.getBuildTiles());
		setStructurePartsToTableData(startX, startY, startAltitude, structureIds, area.getDoors());
		setStructurePartsToTableData(startX, startY, startAltitude, structureIds, area.getFloors());
		setStructurePartsToTableData(startX, startY, startAltitude, structureIds, area.getStructures());
		setStructurePartsToTableData(startX, startY, startAltitude, structureIds, area.getWalls());

		setFencesToTableData(startX, startY, sizeX, sizeY, startAltitude, area.getFences());
		
		area.setAltitudes(getLandscapeHeights(startX, startY, sizeX, sizeY, startAltitude));

		setTileTypesToArea(startX, startY, sizeX, sizeY, area);

		getItems(startX, startY, sizeX, sizeY, startAltitude, area.getItems());

		return area;
	}


	private short[][] getLandscapeHeights(int startX, int startY, int sizeX, int sizeY, short startAltitude)
	{
		MeshIO mesh = Server.surfaceMesh;
		
		short[][] heights = new short[sizeX + 1][sizeY + 1];	// + 1 because we want one extra corner around the area
		
		for(int x = 0; x < (sizeX + 1); x++) {
			for(int y = 0; y < (sizeY + 1); y++) {
				int tileId = mesh.getTile(startX + x, startY + y);
				short height = Tiles.decodeHeight(tileId);
				
				heights[x][y] = (short)(height - startAltitude);

				//logger.info("Tile " + (startX + x) + ", " + (startY + y) + ": " + height + " -- relative: " + (height - startAltitude));
			}
		}

		return heights;
	}


	private boolean setTileTypesToArea(int startX, int startY, int sizeX, int sizeY, Area area)
	{
		byte[][] types = new byte[sizeX + 1][sizeY + 1];	// + 1 because we want one extra tile around area
		byte[][] data = new byte[sizeX + 1][sizeY + 1];		// + 1 because we want one extra tile around area

		for(int x = 0; x < (sizeX + 1); x++) {
			for(int y = 0; y < (sizeY + 1); y++) {
				int currX = startX + x;
				int currY = startY + y;

				int tile = Server.surfaceMesh.getTile(currX, currY);

				types[x][y] = Tiles.decodeType(tile);
				data[x][y] = Tiles.decodeData(tile);
			}
		}
		
		area.setTileTypes(types);
		area.setTileData(data);
		
		return true;
	}


	HashSet<Long> getStructures(int startX, int startY, int sizeX, int sizeY)
	{
        Connection dbCon = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		HashSet<Long> structureIds = new HashSet<Long>();

		String select = "SELECT DISTINCT structure FROM ";
		String where = " WHERE tilex >= " + startX + " AND tilex <= " + (startX + sizeX) + " AND tiley >= " + startY + " AND tiley <= " + (startY + sizeY);
		String sql = null;

		try {
			dbCon = DbConnector.getZonesDbCon();

			for(int i = 0; i < structurePartTables.length; i++) {
				sql = select + structurePartTables[i] + where;
				
				ps = dbCon.prepareStatement(sql);
				rs = ps.executeQuery();

				while (rs.next()) {
					if(structureIds.contains(rs.getLong("structure")) == false) {
						structureIds.add(rs.getLong("structure"));
					}
				}
	
				rs.close();
				ps.close();
			}
		}
        catch (SQLException sqx) {
            throw new RuntimeException(sqx);
        }
        finally {
            DbUtilities.closeDatabaseObjects(ps, rs);
            DbConnector.returnConnection(dbCon);
        }
		
		return structureIds;
	}


	private void setStructurePartsToTableData(int startX, int startY, int startHeight, HashSet<Long> structureIds, TableData td)
	{
		// This will prevent us from getting partial structures.
		String sql = "SELECT * FROM " + td.getTable() + " WHERE " + td.getStructureIdColumnName() + " IN (" + getCSVfromSet(structureIds) + ") ORDER BY " + td.getStructureIdColumnName();

		setComponentsToTableData(startX, startY, startHeight, td, sql);
	}


	private void setFencesToTableData(int startX, int startY, int sizeX, int sizeY, int startHeight, TableData td)
	{
		String sql = "SELECT * FROM " + td.getTable() + " WHERE tilex >= " + startX + " AND tilex <= " + (startX + sizeX) + " AND tiley >= " + startY + " AND tiley <= " + (startY + sizeY);

		setComponentsToTableData(startX, startY, startHeight, td, sql);
	}


	private void getItems(int startTileX, int startTileY, int sizeTileX, int sizeTileY, int startHeight, TableData td)
	{
		float startX = startTileX * 4;
		float startY = startTileY * 4;
		float sizeX = sizeTileX * 4;
		float sizeY = sizeTileY * 4;

		String templateIds = Arrays.toString(decorationsGroup).replace("[", "").replace("]", "")
			+ ", " + Arrays.toString(furnitureGroup).replace("[", "").replace("]", "")
			+ ", " + Arrays.toString(luxuriesGroup).replace("[", "").replace("]", "");
			
		String sql = "SELECT items.*, itemdata.data1, itemdata.data2 "
				+ "FROM items "
				+ "LEFT JOIN itemdata ON (itemdata.wurmid = items.wurmid) "
				+ "WHERE "
				+ "("
					+ "posx >= " + startX + 
					" AND posx <= " + (startX + sizeX) + 
					" AND posy >= " + startY + 
					" AND posy <= " + (startY + sizeY) + " " +
					(Mod.copyAllItems ? "" : "AND templateid IN (" + templateIds + ")")
				+ ")";
		
		logger.info("New copy...");
		logger.info("Initial sql on area: " + sql);
		ArrayList<Long> itemIds = setComponentsToTableData(startTileX, startTileY, startHeight, td, sql);

		// Handle containers (recursively) -- this could become a lot
		String itemIdsStr = null;
		while(itemIds.size() > 0) {
			itemIdsStr = Arrays.toString(itemIds.toArray(new Long[itemIds.size()])).replace("[", "").replace("]", "");
			
			sql = "SELECT items.*, itemdata.data1, itemdata.data2 "
					+ "FROM items "
					+ "LEFT JOIN itemdata ON (itemdata.wurmid = items.wurmid) "
					+ "WHERE parentid IN (" + itemIdsStr + ")";
			logger.info("Copy items recursively: " + sql);
			
			itemIds = setComponentsToTableData(startTileX, startTileY, startHeight, td, sql);
		}

	}
	
	
	/**
	 * Returns ItemIDs that were included.
	 * 
	 * @param startX
	 * @param startY
	 * @param startHeight
	 * @param td
	 * @param sql
	 * @return
	 */
	private ArrayList<Long> setComponentsToTableData(int startX, int startY, int startHeight, TableData td, String sql)
	{
        Connection dbCon = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int i = 0;
		
		ArrayList<Long> ret = new ArrayList<Long>();

		try {
			if(td.getTable() == "items") {
				dbCon = DbConnector.getItemDbCon();
			} else {
				dbCon = DbConnector.getZonesDbCon();
			}

			ps = dbCon.prepareStatement(sql);

			// We need row and column count to allocate memory for the arrays.
			rs = ps.executeQuery();

			i = 0;
			while(rs.next()) {
				i++;
			}
			int rowCount = i;
			rs.close();

			rs = ps.executeQuery();

			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();

			// Allocate the arrays.
			if(td.getRowCount() == 0) {
				td.setMeta(new String[columnCount]);
				td.setData(new Object[rowCount][columnCount]);

				// Iteration should start at 1.
				for (i = 1; i <= columnCount; i++ ) {
					td.getMeta()[i-1] = rsmd.getColumnName(i).toLowerCase();
				}
			} else {
				// expand number of rows in this set
				td.expandDataRows(rowCount);
			}

			logger.info("Copying " + rowCount + " rows from " + td.getTable());

			// Index to insert into data-array, start after existing entries.
			// NOTE, in case bug: This is where a one-off error would sneak in.
			i = td.getRowCount() - rowCount;
			
			int j = 0;
			
			while (rs.next()) {
				for(j = 0; j < columnCount; j++) {
					final String colName = td.getMeta()[j];
					
					if(rs.getObject(j+1) != null && Stream.of(relativizeExactPosFields).anyMatch(x -> x.equals(colName))) {
						// Translate exact positions (not just tile), largely the same code, but we need to cast to float and multiply by 4
						if(colName.endsWith("x")) {
							//logger.info("Translating: " + colName + " containing " + rs.getObject(j+1) + " to: " + ((int)rs.getObject(j+1) - startX));
							td.getData()[i][j] = ((double)rs.getObject(j+1) - ((float)startX * 4f));

						} else if(colName.endsWith("y")) {
							//logger.info("Translating: " + colName + " containing " + rs.getObject(j+1) + " to: " + ((int)rs.getObject(j+1) - startY));
							td.getData()[i][j] = ((double)rs.getObject(j+1) - ((float)startY * 4f));
						
						} else {
							throw new RuntimeException("Cannot figure out whether this contains x or y: " + colName);
						}

					} else if(rs.getObject(j+1) != null && Stream.of(relativizeTileFields).anyMatch(x -> x.equals(colName))) {
						// Translate tile position (as opposed to exact position)
						if(colName.endsWith("x")) {
							//logger.info("Translating: " + colName + " containing " + rs.getObject(j+1) + " to: " + ((int)rs.getObject(j+1) - startX));
							td.getData()[i][j] = ((int)rs.getObject(j+1) - startX);

						} else if(colName.endsWith("y")) {
							//logger.info("Translating: " + colName + " containing " + rs.getObject(j+1) + " to: " + ((int)rs.getObject(j+1) - startY));
							td.getData()[i][j] = ((int)rs.getObject(j+1) - startY);
						
						} else {
							throw new RuntimeException("Cannot figure out whether this contains x or y: " + colName);
						}
					} else {
						td.getData()[i][j] = rs.getObject(j+1);
					}
				}

				ret.add(Long.parseLong(td.getData()[i][0].toString()));
				
				i++;
			}
			
			rs.close();
			ps.close();
		}
        catch (SQLException sqx) {
            throw new RuntimeException(sqx);
        }
        finally {
            DbUtilities.closeDatabaseObjects(ps, rs);
            DbConnector.returnConnection(dbCon);
        }

		//logger.info("Copied " + td.getRowCount() + " rows from " + td.getTable());
		return ret;
	}


	private String getCSVfromSet(HashSet<Long> hs)
	{
		StringBuilder sb = new StringBuilder();
		
		for(Long s : hs) {
			if(sb.length() > 0) {
				sb.append(",");
			}
			sb.append(s);
		}
		
		return sb.toString();
	}
}
