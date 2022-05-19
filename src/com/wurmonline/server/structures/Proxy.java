package com.wurmonline.server.structures;

public class Proxy
{
	static public void setFinalFinished(Structure struct, boolean finfinish)
	{
		((DbStructure)struct).setFinalFinished(finfinish);
	}
}
