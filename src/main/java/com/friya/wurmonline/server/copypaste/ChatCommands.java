package com.friya.wurmonline.server.copypaste;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.wurmonline.server.creatures.Communicator;

/*
 * 
 * add chat commands:
 *
 * "disk" here means "remote site"
 * 
 * - /copy 10 10 <to buffer 1> <as some name>		-- give the buffer a name, helpful if juggling several buffers
 * - /copy 10 10 to disk <as some name> 
 * - /copy "deedname" to disk <as some name>
 * 
 * - /cut 10 10 <to buffer 1> -- copy to buffer then delete all structures/items in the area
 * 
 * bind Ctrl+N "say /cbs x-5 y-5"
 * bind Ctrl+M "say /cbs x+5 y+5"
 * bind Ctrl+B "say /rot"
 * bind Ctrl+Z "say /undo"
 * bind Ctrl+C "say /copy"
 * bind Ctrl+V "say /paste"
 * 
 */
class ChatCommands
{
	private static Logger logger = Logger.getLogger(ChatCommands.class.getName());

	static boolean onPlayerMessage(Communicator com, String msg)
	{
		if(msg.startsWith("/clipboardsize") || msg.startsWith("/cbs")) {
			return ClipboardSettings.setClipboardSize(com, msg);
			
		} else if(msg.startsWith("/dir") || msg.startsWith("/ls")) {
			return cmdDir(com, msg);
			
		} else if(msg.startsWith("/load")|| msg.startsWith("/ld")) {
			return cmdLoad(com, msg);
		
		} else if(msg.startsWith("/undo")) {
			return cmdUndo(com, msg);

		} else if(msg.startsWith("/paste") || msg.startsWith("/pt")) {
			return cmdPaste(com, msg);
			
		} else if(msg.startsWith("/rotate") || msg.startsWith("/rot")) {
			return cmdRotate(com, msg);
			
		} else if(msg.startsWith("/copy") || msg.startsWith("/cp")) {
			return cmdCopy(com, msg);
		}

		return false;
	}


	/*
	 * - /paste 1 (shortcut for: /paste all from buffer 1)
	 * - /paste buffer 1 (same as paste 1)
	 * - /paste all from buffer 1
	 * - /paste all from <some name on disk>
	 * - 'all' can also be any or multiple of: fences, bridges, buildings, landscape, trees, tiletypes, decorations, furniture, items
	 */
	private static boolean cmdPaste(Communicator com, String msg)
	{
		String[] tokens = translateCommandline(msg);
		
		if(tokens.length == 2) {
			
		}

		// TODO
		com.sendNormalServerMessage("/paste TODO");
		return true;
	}
	

	private static boolean cmdDir(Communicator com, String msg)
	{
		// TODO
		String[] pasteFiles = IO.dir();
		
		for(String pasteFile : pasteFiles) {
			com.sendNormalServerMessage("/load " + pasteFile);
		}
		
		/*
		com.sendNormalServerMessage(
			String.join("\r\n", pasteFiles)
		);
		*/
		//com.sendNormalServerMessage("/load " + pasteFile);
		
		return true;
	}
	
	
	private static boolean cmdCopy(Communicator com, String msg)
	{
		// TODO
		com.sendNormalServerMessage("/copy TODO");
		return true;
	}


	private static boolean cmdUndo(Communicator com, String msg)
	{
		ActionUndo.undo(com.getPlayer());
		return true;
	}


	private static boolean cmdRotate(Communicator com, String msg)
	{
		ClipboardSettings.addRotation(com.getPlayer().getName(), 90);
		com.sendNormalServerMessage("Rotation set to " + ClipboardSettings.getRotation(com.getPlayer().getName()) + " degrees.");
		return true;
	}


	/*
	 * - /load saved-name into buffer 1
	 * - /load "saved-name" 1
	 */
	private static boolean cmdLoad(Communicator com, String msg)
	{
		int copyBufferId = 0;
		String fileName = null;
		
		String[] tokens = translateCommandline(msg);

		logger.info("" + Arrays.toString(tokens));
		
		// /load filename
		if(tokens.length == 2) {
			fileName = tokens[1];
			copyBufferId = 0;
			
		} else if(tokens.length == 3 && isNumeric(tokens[3])) {
			// load filename bufferid
			fileName = tokens[1];
			copyBufferId = Integer.valueOf(tokens[3]).intValue();
			
		} else if(tokens.length > 3 && tokens[3].equals("into")) {
			// load filename into bufferid
			// load filename into buffer bufferid
			fileName = tokens[1];
			copyBufferId = Integer.valueOf(tokens[tokens.length-1]).intValue();
			
		} else {
			com.sendNormalServerMessage("Me not understand. Usage examples: \"/load <areaname>\" or \"/load <areaname> into buffer <0>\" or /load <areaname> <buffer>\"");
			return true;
		}
		
		if(IO.exists(fileName) == false) {
			com.sendNormalServerMessage("Area \"" + fileName + "\" does not exist.");
			return true;
		}
		
		boolean loadResult = IO.load(com.getPlayer().getName(), fileName, copyBufferId);
		if(loadResult == false) {
			com.sendNormalServerMessage("Loading failed.");
			return true;
		}
		
		com.sendNormalServerMessage("Loaded " + fileName + " into buffer " + copyBufferId + ".");
		return true;
	}


    /**
     * Crack a command line.
     * 
     * https://commons.apache.org/proper/commons-exec/apidocs/src-html/org/apache/commons/exec/CommandLine.html
     * 
     * @param toProcess the command line to process.
     * @return the command line broken into strings.
     * 
     * An empty or null toProcess parameter results in a zero sized array.
     */
    static String[] translateCommandline(String toProcess)
    {
        if (toProcess == null || toProcess.length() == 0) {
            //no command? no string
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        final ArrayList<String> result = new ArrayList<String>();
        final StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();

            switch (state) {
                case inQuote:
                    if ("\'".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                    
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        lastTokenHasBeenQuoted = true;
                        state = normal;
                    } else {
                        current.append(nextTok);
                    }
                    break;
                    
                default:
                    if ("\'".equals(nextTok)) {
                        state = inQuote;
                    } else if ("\"".equals(nextTok)) {
                        state = inDoubleQuote;
                    } else if (" ".equals(nextTok)) {
                        if (lastTokenHasBeenQuoted || current.length() != 0) {
                            result.add(current.toString());
                            current.setLength(0);
                        }
                    } else {
                        current.append(nextTok);
                    }
                    lastTokenHasBeenQuoted = false;
                    break;
            }
        }

        if (lastTokenHasBeenQuoted || current.length() != 0) {
            result.add(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new RuntimeException("unbalanced quotes in " + toProcess);
        }

        return result.toArray(new String[result.size()]);
    }


	/**
	 * Despite this using an exception for functionality, I am okay with it. It's executed rarely enough and definitely rare enough to trigger the exception.
	 * 
	 * @param str
	 * @return
	 */
	static boolean isNumeric(String str)  
	{  
		try {
			@SuppressWarnings("unused")
			double d = Double.parseDouble(str);  
		}  
		catch(NumberFormatException nfe) {  
			return false;  
		}
	
		return true;  
	}	
}
