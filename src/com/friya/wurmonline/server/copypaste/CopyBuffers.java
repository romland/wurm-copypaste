package com.friya.wurmonline.server.copypaste;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;


class CopyBuffers
{
	private static Logger logger = Logger.getLogger(CopyBuffers.class.getName());
	private static CopyBuffers instance = null;

	private int maxBuffersPerOwner = 10;
	private Map<String, LinkedList<Buffer>> buffers = new HashMap<>();
	


	static CopyBuffers getInstance()
	{
		if(instance == null) {
			instance = new CopyBuffers();
		}

		return instance; 
	}

	
	CopyBuffers()
	{
	}

	
	private void createBuffers(String owner)
	{
		if(buffers.containsKey(owner) == false) {
			buffers.put(owner, new LinkedList<Buffer>());
		}
		
		LinkedList<Buffer> list = buffers.get(owner);
		
		if(list.size() >= maxBuffersPerOwner) {
			return;
		}

		for(int i = 0; i < maxBuffersPerOwner; i++) {
			Buffer b = new Buffer(owner, i);
			list.add(b);
		}
	}
	
	
	// This will also createBuffers if they do not exist
	private boolean canAdd(String owner, int toBuffer)
	{
		if(toBuffer < 0 || toBuffer >= maxBuffersPerOwner) {
			return false;
		}
		
		if(buffers.containsKey(owner) == false) {
			createBuffers(owner);
		}

		return true;
	}

	
	private Area getAreaOrNullByBufferId(String owner, int id)
	{
		if(buffers.containsKey(owner) == false || buffers.get(owner).get(id).isEmpty()) {
			return null;
		}
		
		Gson gson = new Gson();
		Area a = gson.fromJson(buffers.get(owner).get(id).area, Area.class);
		
		// NOTE: This is a dumb fix. I had a lot of saved areas and introduced file incompatiblity.
		a.attachFix();
		return a;
	}
	

	private Area getAreaOrNullByBufferName(String owner, String name)
	{
		if(buffers.containsKey(owner) == false) {
			return null;
		}

		LinkedList<Buffer> bs = buffers.get(owner);

		for(int i = 0; i < bs.size(); i++) {
			if(bs.get(i).name.equals(name)) {
				return getAreaOrNullByBufferId(owner, i);
			}
		}

		return null;
	}


	boolean setJsonToBuffer(String owner, String fileName, StringBuffer sb, int toBuffer)
	{
		if(canAdd(owner, toBuffer) == false) {
			return false;
		}
		
		Buffer b = buffers.get(owner).get(toBuffer);
		b.reset();

		b.name = fileName;
		b.owner = owner;
		b.area = sb.toString();
		
		return true;
	}

	// Set to buffer id (also set its name)
	boolean add(String owner, Area area, int toBuffer, String asName)
	{
		if(canAdd(owner, toBuffer) == false) {
			throw new RuntimeException("Maximum buffers is " + maxBuffersPerOwner);
		}
		
		Buffer b = buffers.get(owner).get(toBuffer);
		b.reset();

		Gson gson = new Gson();
		b.area = gson.toJson(area);
		b.name = asName;

		if(area.isTemporary == false) {
			IO.save(b);
		}
		
		return true;
	}
	
	// Set to buffer with id
	boolean add(String owner, Area area, int toBuffer)
	{
		return add(owner, area, toBuffer, "");
	}

	// Set to buffer 0 as name
	boolean add(String owner, Area area, String asName)
	{
		return add(owner, area, 0, asName);
	}

	// Set to buffer 0
	boolean add(String owner, Area area)
	{
		return add(owner, area, 0, "");
	}


	// Get from buffer with id
	Area get(String owner, int bufferId)
	{
		return getAreaOrNullByBufferId(owner, bufferId);
	}
	
	// Get from buffer with name
	Area get(String owner, String bufferName)
	{
		return getAreaOrNullByBufferName(owner, bufferName);
	}

	// Get buffer 0
	Area get(String owner)
	{
		return getAreaOrNullByBufferId(owner, 0);
	}
}
