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


class ActionDelete implements ModAction
{
	private static Logger logger = Logger.getLogger(ActionDelete.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	
	static public short getActionId()
	{
		return actionId;
	}
	

	public ActionDelete()
	{
		logger.log(Level.INFO, "ActionDelete()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Delete", 
			"deleting",
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

			public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir)
			{
				return Arrays.asList(actionEntry);
			}

			public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, int tilex, int tiley, boolean onSurface, int tile)
			{
				return Arrays.asList(actionEntry);
			}

			@Override
			public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile)
			{
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


			public boolean action(Action act, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short action, float counter)
			{
				action(act, performer, tilex, tiley, onSurface, tile, action, counter);
				return true;
			}
			
			public boolean action(Action act, Creature performer, int startX, int startY, boolean onSurface, int tile, short action, float counter)
			{
				if (!isAllowed(performer)) {
					return true;
				}
				
				int sizeX = ClipboardSettings.getSizeX(performer.getName());
				int sizeY = ClipboardSettings.getSizeY(performer.getName());

				performer.getCommunicator().sendNormalServerMessage("Deleting...");

				// Get the area we are about to delete
				Area org = Copy.getInstance().area(startX, startY, sizeX, sizeY);
				
				// Add the original area here to Undo buffer (possibly the area has fences/structures that are about to get deleted)
				UndoBuffers.getInstance().add(performer.getName(), org);

				// Now delete the contents of the area
				Delete.getInstance().area(startX, startY, sizeX, sizeY);
				
				performer.getCommunicator().sendNormalServerMessage("Deleted!");
				return true;
			}

		}; // ActionPerformer
	}
}
