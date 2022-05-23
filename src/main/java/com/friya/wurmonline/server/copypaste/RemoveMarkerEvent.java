package com.friya.wurmonline.server.copypaste;


class RemoveMarkerEvent extends EventOnce
{
	//private static Logger logger = Logger.getLogger(CreateMazeHedgeEvent.class.getName());
	
	String owner = null;
	long id = -1;
	
	public RemoveMarkerEvent(int fromNow, String owner, long id)
	{
        super(fromNow, Unit.MILLISECONDS);
        
        this.owner = owner;
        this.id = id;
	}

	@Override
	public boolean invoke()
	{
		ClipboardSettings.deleteMarker(this.owner, this.id);
		return true;
	}
}
