package com.friya.wurmonline.server.copypaste;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;

class UndoBuffers
{
//	private static Logger logger = Logger.getLogger(UndoBuffers.class.getName());
	private static UndoBuffers instance = null;

	private int maxBuffersPerOwner = 25;
	private Map<String, LinkedList<Buffer>> buffers = new HashMap<>();
	

	static UndoBuffers getInstance()
	{
		if(instance == null) {
			instance = new UndoBuffers();
		}

		return instance; 
	}


	/**
	 * add an undo buffer last in the list for this player
	 * 
	 * @param owner
	 * @param area
	 * @return
	 */
	boolean add(String owner, Area area)
	{
		if(buffers.containsKey(owner) == false) {
			buffers.put(owner, new LinkedList<Buffer>());
		}
		
		LinkedList<Buffer> list = buffers.get(owner);

		if(buffers.size() > maxBuffersPerOwner) {
			list.removeFirst();
		}
		
		Buffer b = new Buffer(owner, "Area at " + area.startX + ", " + area.startY);
		
		Gson gson = new Gson();
		b.area = gson.toJson(area);

		list.addLast(b);
		return true;
	}


	/**
	 * undo the last action (cut, paste or delete) this player did
	 * 
	 * @param owner
	 * @return
	 */
	Area getLast(String owner)
	{
		if(buffers.containsKey(owner) == false) {
			return null;
		}

		Gson gson = new Gson();
		return gson.fromJson(buffers.get(owner).getLast().area, Area.class);
	}
	

	boolean removeLast(String owner)
	{
		if(buffers.containsKey(owner) == false) {
			return false;
		}
		
		buffers.get(owner).removeLast();
		return true;
	}
	

	boolean isEmpty(String owner)
	{
		return buffers.containsKey(owner) == false || buffers.get(owner).size() == 0;
	}
}
