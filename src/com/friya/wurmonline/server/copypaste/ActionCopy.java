package com.friya.wurmonline.server.copypaste;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.structures.NoSuchStructureException;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;
import com.wurmonline.server.structures.Wall;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;


class ActionCopy implements ModAction
{
	private static Logger logger = Logger.getLogger(ActionCopy.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	
	static public short getActionId()
	{
		return actionId;
	}
	

	public ActionCopy()
	{
		logger.log(Level.INFO, "ActionCopy()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Copy", 
			"copying",
			new int[] {
				23				// ACTION_TYPE_IGNORERANGE = 23;
			}
		);
		ModActions.registerAction(actionEntry);
	}


	private boolean isAllowed(Creature performer)
	{
		if (performer.isOnSurface() == false || performer.getPower() < 5) {
			return false;
		}
		
		return true;
	}
	
	
	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {
			// on wall without item
			public List<ActionEntry> getBehavioursFor(Creature performer, Wall wall)
			{
				return getBehavioursFor(performer, null, wall);
			}

			// on wall with item
			public List<ActionEntry> getBehavioursFor(Creature performer, Item source, Wall wall)
			{
				if (!isAllowed(performer)) {
					return null;
				}

				return Arrays.asList(actionEntry);
			}
			
			// on item without item
			public List<ActionEntry> getBehavioursFor(Creature performer, Item target)
			{
				return this.getBehavioursFor(performer, null, target);
			}

			// on item with item
			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target)
			{
				if (!isAllowed(performer)) {
					return null;
				}

				if(target.getTemplateId() == ItemList.villageToken) {
					// Deed token
					return Arrays.asList(actionEntry);

				} else if(target.getParentId() == -10) {
					// TODO: Technically we should really only allow this for items in ItemGroups
					return Arrays.asList(actionEntry);
				}

				return null;
			}

			// on tile-(border?) or what is 'dir' here?
			public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir)
			{
				if (!isAllowed(performer)) {
					return null;
				}

				return Arrays.asList(actionEntry);
			}

			// on tile with item
			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, int tilex, int tiley, boolean onSurface, int tile)
			{
				if (!isAllowed(performer)) {
					return null;
				}

				return Arrays.asList(actionEntry);
			}

			// on tile
			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile)
			{
				if (!isAllowed(performer)) {
					return null;
				}

				return Arrays.asList(actionEntry);
			}
		};
	}


	@Override
	public ActionPerformer getActionPerformer()
	{
		return new ActionPerformer() {

			@Override
			public short getActionId() {
				return actionId;
			}

			// on wall without item
			public boolean action(Action act, Creature performer, Wall wall, short action, float counter)
			{
				return action(act, performer, null, wall, action, counter);
			}

			// on wall with item
			public boolean action(Action act, Creature performer, Item source, Wall wall, short action, float counter)
			{
				if (!isAllowed(performer) || wall == null) {
					return true;
				}
				
				Structure s;
				try {
					s = Structures.getStructure(wall.getStructureId());

				} catch (NoSuchStructureException e) {
					logger.log(Level.SEVERE, "Failed to copy building", e);
					performer.getCommunicator().sendNormalServerMessage("Failed to copy building, see server log.");
					return true;
				}

				copy(performer, s.getMinX(), s.getMinY(), s.getMaxX() - s.getMinX() + 1, s.getMaxY() - s.getMinY() + 1, "Building-" + s.getName());

				performer.getCommunicator().sendNormalServerMessage("Copying building \"" + s.getName() + "\"...");
				return true;
			}


			// On item without activated object
			@Override
			public boolean action(Action act, Creature performer, Item target, short action, float counter)
			{
				return action(act, performer, null, target, action, counter);
			}


			// On item with activated object
			@Override
			public boolean action(Action act, Creature performer, Item item, Item target, short action, float counter)
			{
				if (!isAllowed(performer)) {
					return true;
				}

				if(target.getTemplateId() == ItemList.villageToken) {
					// This is a deed, let's get its size then copy all of it
					Village v = Villages.getVillage(target.getTileX(), target.getTileY(), true);

					performer.getCommunicator().sendNormalServerMessage("Copying village \"" + v.getName() + "\"...");
					
					copy(performer, v.startx, v.starty, (v.endx - v.startx + 1), (v.endy - v.starty + 1), "Deed-" + v.getName());
				} else if(true) {
					// TODO: this is only meant for decorations/furniture but allowing all for now
					
					// Only allow items placed on the ground
					logger.info("isGround: " + target.getParentId());
					if(target.getParentId() == -10) {
						copy(performer, target.getTileX(), target.getTileY(), 1, 1, "Item-" + target.getName());

						Area area = null;
						area = Copy.getInstance().area(target.getTileX(), target.getTileY(), 1, 1);
						area.attachNone();
						area.attachItems = true;
						area.isTemporary = true;
						CopyBuffers.getInstance().add(performer.getName(), area, target.getName());
					}

				}

				return true;
			}


			// on tile with item
			public boolean action(Action act, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short action, float counter)
			{
				return action(act, performer, tilex, tiley, onSurface, tile, action, counter);
			}


			// on tile without item
			public boolean action(Action act, Creature performer, int startX, int startY, boolean onSurface, int tile, short action, float counter)
			{
				if (!isAllowed(performer)) {
					return true;
				}
				
				int sizeX = ClipboardSettings.getSizeX(performer.getName());
				int sizeY = ClipboardSettings.getSizeY(performer.getName());
				
				performer.getCommunicator().sendNormalServerMessage("Copying " + sizeX + "x" + sizeY + " tiles...");

				copy(performer, startX, startY, sizeX + 1, sizeY + 1);
				return true;
			}
		}; // ActionPerformer
	}


	private void copy(Creature performer, int startX, int startY, int sizeX, int sizeY)
	{
		copy(performer, startX, startY, sizeX, sizeY, null);
	}


	private void copy(Creature performer, int startX, int startY, int sizeX, int sizeY, String areaName)
	{
		Area area = null;
		
		area = Copy.getInstance().area(startX, startY, sizeX, sizeY);

		if(areaName == null) {
			areaName = "from " + startX + ", " + startY + " with size " + sizeX + "x" + sizeY;
		}

		// Throw into first copy buffer with a name
		CopyBuffers.getInstance().add(performer.getName(), area, areaName);

		performer.getCommunicator().sendNormalServerMessage("Copied to buffer 0: " + areaName);
	}
}
