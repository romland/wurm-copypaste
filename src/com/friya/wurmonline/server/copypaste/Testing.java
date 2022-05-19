package com.friya.wurmonline.server.copypaste;

import java.util.logging.Logger;

import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;

public class Testing 
{
	private static Logger logger = Logger.getLogger(Testing.class.getName());

	static void createMyBsbs()
	{
//		"[" + i + ",669,\"bulk item\",\"1000x\",null,99.0,99.0,null,29398299098882,11296604,11296604,0,-10,1409356185344,null,null,null,null,1.0,-10,0.0,1,1,1,3000,14,-10,0,0,0,0,0,0,22,-1,0,0,0,0,\"\",0,0,-10,0,-1,null,null],"
		StringBuilder sb = new StringBuilder();
		
		String preString = 
			"{\"areaVersion\":1,\"contentHash\":\"BSB\",\"timestamp\":1528294072114,\"isTemporary\":false,\"serverIP\":\"192.168.178.100\",\"serverName\":\"Friya\",\"owner\":\"\",\"steamId\":\"\",\"mayor\":\"\",\"deedName\":\"\",\"ownerIsGM\":false,\"isPublicServer\":false,\"isTamperProtected\":false,\"attachBridges\":true,\"attachBuildings\":true,\"attachFences\":true,\"attachLandscape\":true,\"attachTrees\":true,\"attachItems\":true,\"startAltitude\":32,\"startX\":1210,\"startY\":382,\"sizeX\":1,\"sizeY\":1,\"bridgeParts\":{\"table\":\"bridgeparts\",\"structureIdColumnName\":\"structure\",\"meta\":[\"id\",\"structure\",\"type\",\"lastmaintained\",\"originalql\",\"currentql\",\"damage\",\"tilex\",\"tiley\",\"state\",\"material\",\"heightoffset\",\"dir\",\"slope\",\"stagecount\",\"northexit\",\"eastexit\",\"southexit\",\"westexit\",\"settings\",\"roadtype\",\"layer\"],\"data\":[]},\"buildTiles\":{\"table\":\"buildtiles\",\"structureIdColumnName\":\"structureid\",\"meta\":[\"id\",\"structureid\",\"tilex\",\"tiley\",\"layer\"],\"data\":[]},\"doors\":{\"table\":\"doors\",\"structureIdColumnName\":\"structure\",\"meta\":[\"id\",\"structure\",\"innerwall\",\"outerwall\",\"lockid\",\"name\",\"settings\"],\"data\":[]},\"floors\":{\"table\":\"floors\",\"structureIdColumnName\":\"structure\",\"meta\":[\"id\",\"structure\",\"type\",\"lastmaintained\",\"originalql\",\"currentql\",\"damage\",\"tilex\",\"tiley\",\"state\",\"color\",\"material\",\"heightoffset\",\"layer\",\"dir\",\"slope\",\"stagecount\",\"settings\"],\"data\":[]},\"structures\":{\"table\":\"structures\",\"structureIdColumnName\":\"wurmid\",\"meta\":[\"wurmid\",\"centerx\",\"centery\",\"roof\",\"finished\",\"finfinished\",\"surfaced\",\"name\",\"writid\",\"allowsallies\",\"allowsvillagers\",\"allowskingdom\",\"structuretype\",\"planner\",\"ownerid\",\"settings\",\"village\"],\"data\":[]},\"walls\":{\"table\":\"walls\",\"structureIdColumnName\":\"structure\",\"meta\":[\"id\",\"structure\",\"type\",\"lastmaintained\",\"originalql\",\"currentql\",\"damage\",\"tilex\",\"tiley\",\"startx\",\"starty\",\"endx\",\"endy\",\"outerwall\",\"state\",\"color\",\"material\",\"isindoor\",\"heightoffset\",\"layer\",\"wallorientation\",\"settings\"],\"data\":[]},\"fences\":{\"table\":\"fences\",\"meta\":[\"id\",\"type\",\"lastmaintained\",\"originalql\",\"currentql\",\"damage\",\"tilex\",\"tiley\",\"zoneid\",\"dir\",\"state\",\"color\",\"heightoffset\",\"layer\",\"settings\"],\"data\":[]},\"items\":{\"table\":\"items\",\"meta\":[\"wurmid\",\"templateid\",\"name\",\"description\",\"place\",\"qualitylevel\",\"originalqualitylevel\",\"capacity\",\"parentid\",\"lastmaintained\",\"creationdate\",\"creationstate\",\"ownerid\",\"lastownerid\",\"temperature\",\"posx\",\"posy\",\"posz\",\"rotation\",\"zoneid\",\"damage\",\"sizex\",\"sizey\",\"sizez\",\"weight\",\"material\",\"lockid\",\"price\",\"bless\",\"enchant\",\"banked\",\"auxdata\",\"wornarmour\",\"realtemplate\",\"color\",\"female\",\"mailed\",\"mailtimes\",\"transferred\",\"creator\",\"hidden\",\"rarity\",\"onbridge\",\"settings\",\"color2\",\"data1\",\"data2\"],\"data\":["
			+ "[29398299098882,662,\"bulk storage bin\",\"\",null,99.0,99.0,null,-10,11296371,11296371,0,-10,1409356185344,null,0.48486328125,2.1126708984375,3.573178291320801,95.64544677734375,581,0.0,200,200,400,20000,14,-10,0,0,0,0,0,0,-10,16742520,0,0,0,0,\"Friya\",0,0,-10,0,-1,null,null]"
		;
		
		int added = 0;
		ItemTemplate[] tpls = ItemTemplateFactory.getInstance().getTemplates();
		for(ItemTemplate tpl : tpls) {

			if(tpl.getWeightGrams() < 1 || tpl.getWeightGrams() > 800000 || tpl.isFood() || tpl.isLiquid()) {
				continue;
			}
			
			sb.append(",");

			sb.append(
				"[" 
					+ (tpl.getTemplateId() + 8600000)
					+ ",669,\"bulk item\",\"1000x\",null,99.0,99.0,null,29398299098882,11296604,11296604,0,-10,1409356185344,null,null,null,null,1.0,-10,0.0,1,1,1,"
					+ Integer.MAX_VALUE //+ (tpl.getWeightGrams() * 100000)
					+ "," 
					+ tpl.getMaterial()
					+",-10,0,0,0,0,0,0,"
					+ tpl.getTemplateId()
					+ ",-1,0,0,0,0,\"\",0,0,-10,0,-1,null,null"
				+ "]"
			);
			
			sb.append("\n");
			added++;
			
			logger.info("Added to bsb: " + tpl.getName());
		}
		
		String postString = "]},\"altitudes\":[[0,5],[8,14]],\"tileTypes\":[[2,2],[2,2]],\"tileData\":[[-64,-64],[-64,-64]]}";
		IO.saveRaw("mybsb", preString + sb.toString() + postString);


		sb = new StringBuilder();
		added = 0;
		for(ItemTemplate tpl : tpls) {

			if(tpl.getWeightGrams() < 800000 && !tpl.isFood() && !tpl.isLiquid()) {
				continue;
			}
			
			sb.append(",");

			sb.append(
				"[" 
					+ (tpl.getTemplateId() + 8600000)
					+ ",669,\"bulk item\",\"1000x\",null,99.0,99.0,null,29398299098882,11296604,11296604,0,-10,1409356185344,null,null,null,null,1.0,-10,0.0,1,1,1,"
					+ (tpl.getWeightGrams() * 100000)
					+ "," 
					+ tpl.getMaterial()
					+",-10,0,0,0,0,0,0,"
					+ tpl.getTemplateId()
					+ ",-1,0,0,0,0,\"\",0,0,-10,0,-1,null,null"
				+ "]"
			);
			
			sb.append("\n");
			added++;

			logger.info("Added to bsb: " + tpl.getName());
		}
		IO.saveRaw("myfsb", preString + sb.toString() + postString);
		
		logger.info("SILLINESS: Created my super BSBs as: _mybsb.friyaclip and _myfsb.friyaclip");
	}
	

}
