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


class ActionPaste implements ModAction
{
	private static Logger logger = Logger.getLogger(ActionPaste.class.getName());

	static private short actionId;
	private final ActionEntry actionEntry;
	
	static public short getActionId()
	{
		return actionId;
	}
	

	public ActionPaste()
	{
		logger.log(Level.INFO, "ActionPaste()");

		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Paste", 
			"pasting",
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
				
				// Get the first copy buffer
				Area area = CopyBuffers.getInstance().get(performer.getName());
				
				if(area == null) {
					performer.getCommunicator().sendNormalServerMessage("There is nothing to paste");
					return true;
				}

				performer.getCommunicator().sendNormalServerMessage("Pasting " + area.sizeX + "x" + area.sizeY + " tiles...");

				// Get the area we are about to paste over
				// TODO: Need to flip area according to rotation of the paste we are doing here
				Area original = Copy.getInstance().area(startX, startY, area.sizeX, area.sizeY);
				
				// Add the original area here to Undo buffer
				UndoBuffers.getInstance().add(performer.getName(), original);

				// Now paste in the area which we got from the copy buffer
				Paste.getInstance().area(performer, startX, startY, area);

				performer.getCommunicator().sendNormalServerMessage("Done!");
				return true;
			}

		}; // ActionPerformer
	}
}
