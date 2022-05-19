package com.friya.wurmonline.server.copypaste;

import java.util.Properties;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ItemTemplatesCreatedListener;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerLoginListener;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.google.gson.Gson;
import com.wurmonline.server.Point;
import com.wurmonline.server.Servers;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.items.ItemTemplateCreator;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.NoSuchStructureException;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;

import java.util.logging.Level;
import java.util.logging.Logger;



//
// TODO: Why are there walls lingering in TableData that (SHOULD) only contain an object or three?
// TODO: When copying items, don't copy landscape, walls, etc (actually, just don't paste the shit -- need config options in-game to decide what to paste)
// TODO: When copying, need to export all custom template ids as well (and then do a conversion of templateids)




// WORKING ON:
//	- Bridges! How to fucking rotate them
//	- Also 'undo' when having pasted in a rotated item will not work properly (this is because I have not written code for it)
//	- constructor for bridgepart changed in 1.4ish (May2018: I've now fixed this for 1.6)

// BOTTOM FLOOR WALLS NOT STICKING? This is the temporary solution:
// this problem can be spotted by doubleclicking a bottom-floor just after pasting -- if its state is INITIALIZED -- it will fail later on (duplicate walls are in DB)
//		delete from walls where type = 127 and originalql = 10.0 and currentql = 10.0 and outerwall = 0 and state = 1 and material = 0;
//
// It seems to be somehting between creating structure and adding walls -- what is standing if we don't add walls is the exact thing we don't want...
// should the walls of the plan be deleted before adding actual walls?


/*
delete from bridgeparts;
delete from buildtiles;
delete from doors;
delete from fences;
delete from floors;
delete from gates;
delete from structures;
delete from walls;
*/
public class Mod implements WurmServerMod, Initable, Configurable, ServerStartedListener, PreInitable, ItemTemplatesCreatedListener, PlayerMessageListener, PlayerLoginListener
{
	private static Logger logger = Logger.getLogger(Mod.class.getName());
	
	static boolean copyAllItems = true;
	static boolean copyContainerContents = true;		// needed for Bdew's 'place things on things'
	

	// This is still a TODO:
	// This will be hashed together with a savefile. The resulting hash is then stored in said savefile.
	// This will ensure that the file cannot be tampered with unless you have the password. 
	// This way you can store or your deed on your own harddisk, and it can then be imported on any server that has this password.
	// It is up to the server if they want to enforce password check or not.
	// Likewise, to protect your savefile from other users, hash it with your steam-id, and server can make sure that you are the true owner of the file
	//
	// For the big picture: Bake in 'taxing of high skills' to keep things perpetual, you choose which skills you want to go high in.
	
	static String saveFilePassword = "oia8wekjasdfnasdiouysrhjewrh82122";


	@Override
	public void configure(Properties properties)
	{
	
		if(false) {
			//io.load("Friya", "Friyanouce-Building_Two_Towers_Church-1491876363869", 0);
			//io.load("Friya", "Friyanouce-Deed_Friyanouce-1491874936787", 0);
			IO.load("Friya", "Friya-Building_1x1shed-1492344821013", 0);
			//IO.load("Friya", "1-4bridges.friyaclip", 0);
			Area area = CopyBuffers.getInstance().get("Friya");
			Matrix m = Paste.getInstance().getSquareMatrix(area);
			
			logger.info("Before:\r\n" + m);
			
			m.rotate90();
	
			logger.info("After:\r\n" + m);
			
			Point pTest = m.getPoint(-1,-1);
			logger.info("-1,-1: " + pTest.getX() + "," + pTest.getY());
			
			int foo = 1/0; // yep error out
		}
	}


	@Override
	public void preInit()
	{
	}


	@Override
	public void init()
	{
		WurmServerMod.super.init();
		ModActions.init();
	}


	@Override
	public void onItemTemplatesCreated()
	{
		FloatingMarker.onItemTemplatesCreated();
	}


	@Override
	public void onServerStarted()
	{
		ModActions.registerAction(new ActionExamine());
		ModActions.registerAction(new ActionCopy());
		ModActions.registerAction(new ActionCut());
		ModActions.registerAction(new ActionPaste());
		ModActions.registerAction(new ActionDelete());
		ModActions.registerAction(new ActionUndo());

		//doEarlyTesting();
		Testing.createMyBsbs();
	}

	

	// Each onChatMessage should return 'true'if it was intercepted.
	public boolean onPlayerMessage(Communicator c, String msg)
	{
		boolean intercepted = false;
		
		if(ChatCommands.onPlayerMessage(c, msg)) {
			intercepted = true;
		}
		
		return intercepted;
	}

	
	static boolean isTestEnv()
	{
		return Servers.localServer.getName().equals("Friya");
	}


	@SuppressWarnings("unused")
	private void doEarlyTesting()
	{
		if(true) {
			/*
			 * Test deed information:
			 * 		1350 433	northwest corner (excluding bridge)
			 * 		1362 443	southeast corner of flat area
			 * 		= 12, 10
			 * 
			 * 		1329 426	northwest corner of entire island
			 * 		1377 462	southeast corner of entire island
			 * 
			 * 		1354 436	northwest corner of building
			 * 		1360 442	southeast corner of building
			 * 
			 * 		1349 386	northwest corner of bridge
			 * 		1351 433	southeast corner of bridge
			 * ----
			 * Paste at (smallish areas only -- near water):
			 * 		1399 470	northwest corner
			 */
			Area a = Copy.getInstance().area(1350, 433, 12, 10);

			// Serialize
			Gson gson = new Gson();
			logger.info("========================= SERIALIZE =========================");
			String jsonArea = gson.toJson(a);
			logger.info(jsonArea);

			// Deseralize
			gson = new Gson();
			logger.info("========================= DESERIALIZE =========================");
			Area testArea = gson.fromJson(jsonArea, Area.class);
			String jsonTestArea = gson.toJson(testArea);
			logger.info(jsonTestArea);

		}

		if(false) {
			// inspect bridges to see what info we need to get -- we want the args to pass into "planBridge"
			Structure s;
			try {
				s = Structures.getStructure(6996156646148L);
				
				BridgePart[] bps = s.getBridgeParts();
				for(int i = 0; i < bps.length; i++) {
					logger.info("----------");
					logger.info("    ID: " + bps[i].getId());
					logger.info("struct: " + bps[i].getStructureId());
	
					logger.info("E exit: " + bps[i].getEastExit());
					logger.info("N exit: " + bps[i].getNorthExit());
					logger.info("S exit: " + bps[i].getSouthExit());
					logger.info("W exit: " + bps[i].getWestExit());
	
					logger.info(" stage: " + bps[i].getFloorStageAsString());
					logger.info("   dir: " + bps[i].getDir());
					logger.info("  endY: " + bps[i].getEndX());
					logger.info("  endX: " + bps[i].getEndY());
					logger.info("fl.lvl: " + bps[i].getFloorLevel());
					logger.info("fl.stg: " + bps[i].getFloorStageAsString());
					logger.info("  fl.z: " + bps[i].getFloorZ());
					logger.info("height: " + bps[i].getHeight());
					logger.info("hgtoff: " + bps[i].getHeightOffset());
					logger.info(" max Z: " + bps[i].getMaxZ());
					logger.info(" min X: " + bps[i].getMinX());
					logger.info(" min Y: " + bps[i].getMinY());
					logger.info(" min Z: " + bps[i].getMinZ());
					logger.info("MB mat: " + bps[i].getModByMaterial());
					logger.info("extens: " + bps[i].getNumberOfExtensions());
					logger.info(" pos X: " + bps[i].getPositionX());
					logger.info(" pos Y: " + bps[i].getPositionY());
					logger.info("rl hgt: " + bps[i].getRealHeight());
					logger.info(" slope: " + bps[i].getSlope());
					logger.info("startX: " + bps[i].getStartX());
					logger.info("startY: " + bps[i].getStartY());
					logger.info(" state: " + bps[i].getState());
					logger.info("tile X: " + bps[i].getTileX());
					logger.info("tile Y: " + bps[i].getTileY());
					logger.info("bstate: " + bps[i].getBridgePartState());
					logger.info("ctr pt: " + bps[i].getCenterPoint());
					logger.info("   mat: " + bps[i].getMaterial());
					logger.info("  type: " + bps[i].getType());
					logger.info("  code: " + bps[i].getType().getCode() );
				}
			} catch (NoSuchStructureException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	@Override
	public void onPlayerLogin(Player p)
	{
		if(isTestEnv() && p.getName().equals("Friya")) {
			p.getCommunicator().sendNormalServerMessage("AUTO-LOADING FILE INTO COPY BUFFER 0 -- AND MIGHT ROTATE!");
			//io.load(p.getName(), "1x1-2stories.friyaclip", 0);
			//io.load(p.getName(), "Friya-Building_1x1shed-1492344821013", 0);
			//io.load(p.getName(), "Friyanouce-Deed-Stomped-1491705424524.friyaclip", 0);
			//io.load(p.getName(), "Friya-Deed-Edgehedge-1491701433098.friyaclip", 0);
			//io.load(p.getName(), "Friyanouce-Deed-Milliways-1491705464464.friyaclip", 0);
			//io.load(p.getName(), "Friyanouce-Deed_Friyanouce-1491874936787.friyaclip", 0);
			//io.load(p.getName(), "1x1-with-signs-on-different-floors", 0);
			//io.load(p.getName(), "Friya-Building_Edgehedge_Chapel_of_the_Threestrongholdstronghold-1492042607723", 0);
			//io.load(p.getName(), "1wall-shed", 0);
			//io.load(p.getName(), "1-test-all-2", 0);
			//IO.load(p.getName(), "1-bridge2", 0);
			//IO.load(p.getName(), "1-bridge2", 0);
			IO.load(p.getName(), "Friya-Item_round_marble_table-1528259503231", 0);		// marble table with stuff in/on it
			//ClipboardSettings.setRotation(p.getName(), 90);
		}
		
	}
}
