package com.friya.wurmonline.server.copypaste;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.NoSuchStructureException;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;
import com.wurmonline.server.structures.Wall;

public class ActionExamine implements ModAction
{
	static private short actionId;
	private final ActionEntry actionEntry;
	
	static public short getActionId()
	{
		return actionId;
	}

	private boolean isAllowed(Creature performer)
	{
		if (performer.getPower() < 5) {
			return false;
		}
		
		return true;
	}

	public ActionExamine()
	{
		actionId = (short) ModActions.getNextActionId();
		actionEntry = ActionEntry.createEntry(
			actionId, 
			"Clipboard Examine", 
			"studying",
			new int[] {
				23				// ACTION_TYPE_IGNORERANGE = 23;
			}
		);

		ModActions.registerAction(actionEntry);
	}

	@Override
	public BehaviourProvider getBehaviourProvider()
	{
		return new BehaviourProvider() {
			// on bridgepart without item
			public List<ActionEntry> getBehavioursFor(Creature performer, boolean onSurface, BridgePart bp)
			{
				return getBehavioursFor(performer, null, onSurface, bp);
			}

			// on bridgepart with item
			public List<ActionEntry> getBehavioursFor(Creature performer, Item source, boolean onSurface, BridgePart bp)
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

			// on bridgepart without item
			public boolean action(Action act, Creature performer, boolean onSurface, BridgePart aBridgePart, int encodedTile, short action, float counter) { return action( act,  performer, null,  onSurface,  aBridgePart,  encodedTile,  action,  counter); }
			// on bridgepart with item
			public boolean action(Action act, Creature performer, Item item, boolean onSurface, BridgePart bp, int encodedTile, short action, float counter)
			{
				if (!isAllowed(performer) || bp == null) {
					return true;
				}
				
				performer.getCommunicator().sendNormalServerMessage("Examine BrigePart...");
				performer.getCommunicator().sendNormalServerMessage("        type: " + bp.getType().getCode() + " -- " + bp.getType());
				performer.getCommunicator().sendNormalServerMessage("       slope: " + bp.getSlope());
				performer.getCommunicator().sendNormalServerMessage("heightoffset: " + bp.getHeightOffset());
				performer.getCommunicator().sendNormalServerMessage("         dir: " + bp.getDir());
				
				return true;
			}

		};
	}
}
