package com.friya.wurmonline.server.copypaste;

class Buffer
{
	int id = 0;
	String owner = null;		// Actual owner of the buffer

	String name = null;			// Arbitrary name of buffer
	String area = null;			// The serialized data

	
	public Buffer()
	{
	}

	
	public Buffer(String owner, String name)
	{
		this.owner = owner;
		this.name = name;
	}

	
	public Buffer(String owner, int id)
	{
		this.owner = owner;
		this.id = id;
	}
	
	
	public Buffer(String owner, int id, String name)
	{
		this.owner = owner;
		this.id = id;
		this.name = name;
	}
	
	
	public Buffer(String owner, int id, String name, String area)
	{
		this.owner = owner;
		this.id = id;
		this.name = name;
		this.area = area;
	}
	
	
	public void reset()
	{
		name = null;
		area = null;
	}
	

	public boolean isEmpty()
	{
		return area == null || area.length() == 0;
	}
}
