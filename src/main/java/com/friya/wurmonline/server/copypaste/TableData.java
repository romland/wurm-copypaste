package com.friya.wurmonline.server.copypaste;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import com.wurmonline.server.Point;

class TableData
{
	private static Logger logger = Logger.getLogger(TableData.class.getName());

	private String table;
	private String structureIdColumnName;
	private String[] meta;
	private Object[][] data;
//	private ArrayList[] data;

	
	public TableData(String tableName, String colName)
	{
		setTable(tableName);
		setStructureIdColumnName(colName);
	}

	
	int expandDataRows(int num)
	{
		Object[][] newArray = new Object[data.length + num][meta.length];
		System.arraycopy(data, 0, newArray, 0, data.length);
	    data = (Object[][])newArray;
	    return data.length;
	}
	
	
	String getTable() {
		return table;
	}

	
	void setTable(String table) {
		this.table = table;
	}

	
	String[] getMeta() {
		return meta;
	}

	
	void setMeta(String[] meta) {
		this.meta = meta;
	}

	
	Object[][] getData() {
		return data;
	}
	

	void setData(Object[][] data) {
		this.data = data;
	}
	

	String getStructureIdColumnName() {
		return structureIdColumnName;
	}
	

	void setStructureIdColumnName(String structureIdColumnName) {
		this.structureIdColumnName = structureIdColumnName;
	}
	

	public Object getField(int rowNum, String colName)
	{
		return data[rowNum][getDataIndexByName(colName)];
	}
	
	
	public long getFieldAsLong(int rowNum, String colName)
	{
		return ((Double)data[rowNum][getDataIndexByName(colName)]).longValue();
	}
	
	
	public long getFieldAsInt(int rowNum, String colName)
	{
		return ((Double)data[rowNum][getDataIndexByName(colName)]).intValue();
	}

	
	public int getFieldAsInt(int rowNum, int colIndex)
	{
		return ((Double)data[rowNum][colIndex]).intValue();
	}


	/**
	 * This is the normal method used, without rotation. Flagged as private to enforce use of rotation matrix.
	 * 
	 * @param rowNum
	 * @return
	 */
	private HashMap<String, Object> getRow(int rowNum)
	{
		HashMap<String, Object> ret = new HashMap<String, Object>();
		
		for(int i = 0; i < meta.length; i++) {
			ret.put(meta[i], data[rowNum][i]);
		}

		return ret;
	}


	private int getInt(HashMap<String, Object> d, String name)
	{
		return ((Double)d.get(name)).intValue();
	}
	
	
	private float getFloat(HashMap<String, Object> d, String name)
	{
		return ((Double)d.get(name)).floatValue();
	}

	
	public HashMap<String, Object> getRow(int rowNum, Matrix m)
	{
		HashMap<String, Object> ret = new HashMap<String, Object>();
		
		//logger.info("----------- getRow(rowNum, matrix) -------------");
		
		for(int i = 0; i < meta.length; i++) {
			ret.put(meta[i], data[rowNum][i]);
			
			if(table.equals("bridgeparts")) {
				logger.info(table + "." + meta[i] + " = " + data[rowNum][i]);
			}
		}

		// TODO: Remove the 'true' below when not debugging this. No need to do all these casts if we're not rotating anything!
		if(m.degrees != 0) {
			//Point t = null;

			if(ret.containsKey("tilex")) {
				Point p = m.getPoint(getInt(ret, "tilex"), getInt(ret, "tiley"));
				ret.put("tilex", Integer.valueOf(p.getX()).doubleValue() );
				ret.put("tiley", Integer.valueOf(p.getY()).doubleValue());

				logger.info("\tTRANSLATE tilex/y to " + p.getX() + ", " + p.getY());

				//t = new Point(getInt(ret, "tilex"), getInt(ret, "tiley"));
			}
			
			if(ret.containsKey("startx")) {
				Point p = m.getPoint(getInt(ret, "startx"), getInt(ret, "starty"));
				Point p2 = m.getPoint(getInt(ret, "endx"), getInt(ret, "endy"));

				ret.put("startx", Integer.valueOf(p.getX()).doubleValue());
				ret.put("starty", Integer.valueOf(p.getY()).doubleValue());
				logger.info("\tTRANSLATE startx/y to " + p.getX() + ", " + p.getY());

				ret.put("endx", Integer.valueOf(p2.getX()).doubleValue());
				ret.put("endy", Integer.valueOf(p2.getY()).doubleValue());
				logger.info("\tTRANSLATE endx/y to " + p2.getX() + ", " + p2.getY());
			}
			
			if(ret.containsKey("centerx")) {
				Point p = m.getPoint(getInt(ret, "centerx"), getInt(ret, "centery"));
				ret.put("centerx", Integer.valueOf(p.getX()).doubleValue());
				ret.put("centery", Integer.valueOf(p.getY()).doubleValue());

				logger.info("\tTRANSLATE centerx/y to " + p.getX() + ", " + p.getY());
			}

			// 'fences' has dir, but that's only two different directions (horiz or down)
			if(table.equals("floors")) {
				//int newDir = (((getInt(ret, "dir") * 45) + m.degrees) % 360) / 45;

				int newDir = ((getInt(ret, "dir") + (m.degrees / 45)) % 8);
				
				logger.info("\tTRANSLATE dir " + getInt(ret, "dir") + " to " + newDir);
				
				ret.put("dir", Integer.valueOf(newDir).doubleValue());
			}
			
			if(ret.containsKey("northexit")) {
				logger.info("\tTODO: northexit etc");
			}
			
			// NOTE: all heightoffset might be fixable by changing original area.startAltitude to whatever the new 0,0 is
			
			// tilex, tiley, dir, northexit, eastexit, southexit, westexit			bridgeparts
			// tilex, tiley															buildtiles
			// tilex, tiley, dir													fences
			// tilex, tiley, dir													floors
			// centerx, centery														structures 
			// tilex, tiley, startx, starty, endx, endy								walls
			// posx (t*4), posy (t*4), rotation (actual degrees)					items
		}

		return ret;
	}


	public int getRowCount()
	{
		if(data == null) {
			return 0;
		}
		return data.length;
	}


	int getDataIndexByName(String name)
	{
		for(int i = 0; i < meta.length; i++) {
			if(meta[i].equals(name)) {
				return i;
			}
		}

		throw new RuntimeException("Column " + name + " not found");
	}
	
	
	public void outputRow(int rowNum)
	{
		HashMap<String, Object> row = getRow(rowNum);
		
		StringBuffer sb = new StringBuffer();
		for(String s : row.keySet()) {
			sb.append(s);
			sb.append("=");
			sb.append(row.get(s));
			sb.append("\n");
		}
		
		logger.info(sb.toString());
	}
	
	
	long getMin(String colName)
	{
		int colIndex = getDataIndexByName(colName);

		long ret = 0;

		for(Object[] o : data) {
			if((long)o[colIndex] < ret) {
				ret = (long)o[colIndex];
			}
		}

		return ret;
	}

	
	long getMax(String colName)
	{
		int colIndex = getDataIndexByName(colName);

		long ret = 0;

		for(Object[] o : data) {
			if((long)o[colIndex] > ret) {
				ret = (long)o[colIndex];
			}
		}

		return ret;
	}
}
