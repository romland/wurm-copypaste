package com.friya.wurmonline.server.copypaste;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.ItemMaterials;


class FloatingMarker implements ItemTypes, MiscConstants, ItemMaterials
{
	private static Logger logger = Logger.getLogger(FloatingMarker.class.getName());
	private static int itemId;

	static int getId()
	{
		return itemId;
	}

	static void onItemTemplatesCreated()
	{
		try {
			ItemTemplateBuilder itemTemplateBuilder = new ItemTemplateBuilder("friya.floatingmarker");
			itemTemplateBuilder.name("Clipboard Marker", "clipboard markers", "A clipboard marker.");
			itemTemplateBuilder.descriptions("excellent", "good", "ok", "poor");
			itemTemplateBuilder.itemTypes(
				new short[] {	// com/wurmonline/server/items/ItemTypes.java
					52, 92, 60	// 52 decoration, 45 temporary, 59 alwayspoll, 60 = floating, 92 colorable
				}
			);

			itemTemplateBuilder.imageNumber((short) 561);		// cheese
			itemTemplateBuilder.behaviourType((short) 1);
			itemTemplateBuilder.combatDamage(0);
			itemTemplateBuilder.decayTime(3024000L);
			itemTemplateBuilder.dimensions(10000, 10000, 10000);
			itemTemplateBuilder.primarySkill((int) NOID);
			itemTemplateBuilder.bodySpaces(new byte[]{});
			itemTemplateBuilder.modelName("model.marker.temporary.");
			itemTemplateBuilder.difficulty(99.0f);
			itemTemplateBuilder.weightGrams(500000);
			itemTemplateBuilder.material(MATERIAL_CLAY);
			itemTemplateBuilder.value(1);
			itemTemplateBuilder.isTraded(false);
			
			ItemTemplate tpl = itemTemplateBuilder.build();
			itemId = tpl.getTemplateId();
			logger.log(Level.INFO, "Using template id " + itemId);

			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		logger.log(Level.INFO, "Setup completed");
	}

}