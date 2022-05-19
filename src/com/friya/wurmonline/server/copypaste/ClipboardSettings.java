package com.friya.wurmonline.server.copypaste;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.NoSuchTemplateException;

class ClipboardSettings
{
	private static Logger logger = Logger.getLogger(ClipboardSettings.class.getName());

	private static final int SETTINGS_XSIZE = 0;
	private static final int SETTINGS_YSIZE = 1;
	private static final int SETTINGS_MARKER_ID = 2;		// This holds the ID of the clipboard marker owned by this player
	private static final int SETTINGS_ROTATION = 3;

	private static final int MIN_CLIPBOARD_SIZE = 1;
	private static final int MAX_CLIPBOARD_SIZE = 150;
	
	private static HashMap<String, long[]> clipboardSettings = new HashMap<String, long[]>();


	/**
	 * /cbs can be used in place of /clipboardsize
	 *
	 * e.g.
	 * /clipboardsize x+5			ctrl+shift+q / east-west
	 * /clipboardsize y+5			ctrl+shift+a / north-south
	 * 
	 * bind ctrl+n "say /clipboardsize x-5 y-5"
	 * bind ctrl+m "say /clipboardsize x+5 y+5"
	 * 
	 * tested (and supported):
	 * 	v	/clipboardsize
	 * 	v	/cbs
	 * 	v	/cbs 5
	 * 	v	/cbs 5 10
	 * 	v	/cbs x+1
	 * 	v	/cbs y+1
	 * 	v	/cbs x+5 y+5
	 * 	v	/cbs x+5 10
	 * 	v	/cbs 10 y+9
	 * 	v	/cbs 10 y+9 y+5
	 * 	v	/cbs x+5 x+5 x+5 y+5
	 *	v	/cbs 10 1 y+10 y+5
	 * 
	 * @author Friya
	 */
	static boolean setClipboardSize(Communicator com, String msg)
	{
		msg = msg.trim();

		String owner = com.getPlayer().getName();
		int sizeX = 0;
		int sizeY = 0;
		
		createClipboardSettings(owner);
		
		sizeX = (int)clipboardSettings.get(owner)[SETTINGS_XSIZE];
		sizeY = (int)clipboardSettings.get(owner)[SETTINGS_YSIZE];
		
		if(msg.equals("/clipboardsize") || msg.equals("/cbs")) {
			// reset the size
			// i.e. /clipboardsize
			setClipboardSettings(com, 1, 1);
			return true;
		}
		
		String[] tokens = msg.split(" ");
		String token = tokens[1];

		// only one number specified
		if(tokens.length == 2 && ChatCommands.isNumeric(token)) {
			// set it to a proper square of specified size
			// i.e. /clipboardsize 5
			sizeX = Integer.valueOf(token).intValue();
			sizeY = Integer.valueOf(token).intValue();

			setClipboardSettings(com, sizeX, sizeY);
			return true;
		}

		// support for:
		// /clipboardsize x+5 y+5
		// /clipboardsize 3 10
		// /clipboardsize x+3 10
		for(int i = 1; i < tokens.length; i++) {
			token = tokens[i];
			//logger.info("Doing token: " + token);

			// first argument is x, if it's an integer, set to fixed size passed in
			if(i == 1 && ChatCommands.isNumeric(token)) {
				sizeX = Integer.valueOf(token).intValue();
				continue;
			}

			// second argument is y, if it's an integer, set to fixed size passed in
			if(i == 2 && ChatCommands.isNumeric(token)) {
				sizeY = Integer.valueOf(token).intValue();
				continue;
			}

			if(token.length() < 3) {
				com.sendNormalServerMessage("Unknown change: " + token + ", expected for example x+5");
				return true;
			}
			
			String operator = token.substring(1, 2);
			
			//logger.info("Operator: '" + operator + "'");

			if(operator.equals("+") == false && operator.equals("-") == false) {
				com.sendNormalServerMessage("Unknown operator: " + operator);
				return true;
			}
			
			String amount = token.split("[" + operator + "]")[1];
			
			if(ChatCommands.isNumeric(amount) == false) {
				com.sendNormalServerMessage("Unknown amount: " + amount + ", expected for example 5");
				return true;
			}

			if(token.startsWith("x")) {
				// i.e. /clipboardsize x+N
				if(operator.equals("+")) {
					sizeX += Integer.valueOf(amount).intValue();
				} else {
					sizeX -= Integer.valueOf(amount).intValue();
				}
				
			} else if(token.startsWith("y")) {
				// i.e. /clipboardsize y+N
				if(operator.equals("+")) {
					sizeY += Integer.valueOf(amount).intValue();
				} else {
					sizeY -= Integer.valueOf(amount).intValue();
				}
				
			} else  {
				com.sendNormalServerMessage("Unknown direction: " + token);
				return true;
			}
		}
		
		setClipboardSettings(com, sizeX, sizeY);

		return true;
	}


	private static boolean setClipboardSettings(Communicator com, int x, int y)
	{
		if(x < MIN_CLIPBOARD_SIZE) x = MIN_CLIPBOARD_SIZE;
		if(y < MIN_CLIPBOARD_SIZE) y = MIN_CLIPBOARD_SIZE;
		
		if(x > MAX_CLIPBOARD_SIZE) x = MAX_CLIPBOARD_SIZE;
		if(y > MAX_CLIPBOARD_SIZE) y = MAX_CLIPBOARD_SIZE;
		
		String owner = com.getPlayer().getName();
		
		createClipboardSettings(owner);

		long[] settings = clipboardSettings.get(owner);
		
		int oldX = (int)settings[SETTINGS_XSIZE];
		int oldY = (int)settings[SETTINGS_YSIZE];
		
		if(x == oldX && y == oldY) {
			com.sendNormalServerMessage("Clipboard size remains at " + x + ", " + y + ".");
			return true;
		}
		
		settings[SETTINGS_XSIZE] = x;
		settings[SETTINGS_YSIZE] = y;

		if(settings[SETTINGS_MARKER_ID] > 0) {
			deleteMarker(owner, settings[SETTINGS_MARKER_ID]);
		}
		
		try {
			Item item = ItemFactory.createItem(
				FloatingMarker.getId(),
				100.0f,
			    (float)((com.getPlayer().getTileX() + x) << 2) + 2f,
			    (float)((com.getPlayer().getTileY() + y) << 2) + 2f,
			    180f,
			    true,	// surface 
			    (byte) 0,
			    -10,	// not on bridge
			    null
			);

			settings[SETTINGS_MARKER_ID] = item.getWurmId();

			ShortEventDispatcher.add(new RemoveMarkerEvent(9000, owner, item.getWurmId()));

		} catch (NoSuchTemplateException | FailedException e) {
			logger.log(Level.SEVERE, "Failed to create clipboard marker item", e);
		}
		
		com.sendNormalServerMessage("Clipboard size is now " + x + " tile" + (x == 1 ? "" : "s") + " east, " + y + " tile" + (y == 1 ? "" : "s") + " south (it was: " + oldX + ", " + oldY + ")");

		// TODO: do we need the next line? or are we working by reference on plain int[] -- test
		clipboardSettings.put(owner, settings);

		return true;
	}
	
	static void deleteMarker(String owner, long itemId)
	{
		long[] settings = clipboardSettings.get(owner);

		if(settings[SETTINGS_MARKER_ID] != itemId || settings[SETTINGS_MARKER_ID] < 0) {
			return;
		}

		Items.destroyItem(settings[SETTINGS_MARKER_ID]);
		settings[SETTINGS_MARKER_ID] = -1;

		// TODO: do we need the next line? or are we working by reference on plain int[] -- test
		clipboardSettings.put(owner, settings);
	}
	
	private static void createClipboardSettings(String owner)
	{
		if(clipboardSettings.containsKey(owner) == false) {
			clipboardSettings.put(owner, new long[]{1, 1, -1, 0});
		}
	}
	
	static int getSizeX(String owner)
	{
		createClipboardSettings(owner);
		return (int)clipboardSettings.get(owner)[SETTINGS_XSIZE];
	}

	static int getSizeY(String owner)
	{
		createClipboardSettings(owner);
		return (int)clipboardSettings.get(owner)[SETTINGS_YSIZE];
	}
	
	static void setRotation(String owner, int degrees)
	{
		logger.info("Setting rotation: " + degrees);
		createClipboardSettings(owner);
		clipboardSettings.get(owner)[SETTINGS_ROTATION] = degrees;
	}
	
	static void addRotation(String owner, int degrees)
	{
		clipboardSettings.get(owner)[SETTINGS_ROTATION] = (degrees + clipboardSettings.get(owner)[SETTINGS_ROTATION]) % 360;
	}
	
	static int getRotation(String owner)
	{
		createClipboardSettings(owner);
		return (int)clipboardSettings.get(owner)[SETTINGS_ROTATION];
	}
}
