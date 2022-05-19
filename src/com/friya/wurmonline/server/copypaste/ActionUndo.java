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


class ActionUndo implements ModAction
{
	private static Logger logger = Logger.getLogger(ActionUndo.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	
	static public short getActionId()
	{
		return actionId;
	}
	

	public ActionUndo()
	{
		logger.log(Level.INFO, "ActionDelete()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Undo", 
			"undoing",
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
				
				undo(performer);

				return true;
			}

		}; // ActionPerformer
	}


	static boolean undo(Creature performer)
	{
		if(UndoBuffers.getInstance().isEmpty(performer.getName())) {
			performer.getCommunicator().sendNormalServerMessage("There is nothing left to undo.");
			return true;
		}
		
		performer.getCommunicator().sendNormalServerMessage("Undoing...");

		Area area = UndoBuffers.getInstance().getLast(performer.getName());
		
		if(area == null) {
			performer.getCommunicator().sendNormalServerMessage("There is nothing to undo...");
			return true;
		}

		Delete.getInstance().area(area.startX, area.startY, area.sizeX, area.sizeY);

		Paste.getInstance().area(performer, area.startX, area.startY, area);

		// We should not be able to undo the same thing twice, so remove this from my buffers
		UndoBuffers.getInstance().removeLast(performer.getName());
		return true;
	}
}
