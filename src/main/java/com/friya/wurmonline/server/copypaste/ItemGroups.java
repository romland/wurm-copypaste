package com.friya.wurmonline.server.copypaste;

interface ItemGroups
{
	static final int[] decorationsGroup = new int[] {
			522,	// carved pumpkin
			37,		// campfire
			692,	// boulder
			696,	// boulder
			487,	// flag
			577,	// banner
			579,	// kingdom flag
			578,	// kingdom banner
			496,	// "lamp"
			658,	// "hanging lamp
			657,	// "torch lamp",
			659,	// "imperial street lamp", 
			660,	// "metal torch",
			208,	// "pointing sign"
			209,	// 4, "sign",
			210,	// 2, "sign"
			677,	// 2, "sign"
			656,	// "shop sign"
			228,	// "candelabra"
			442,	// 4, "delicious julbord",
			323,	// "altar"
			322,	// "altar"
			325,	// "altar",
			324,	// "altar",
			384,	// "guard tower",
			430,	// "guard tower",
			528,	// "guard tower",
			638,	// "guard tower"
			398,	// "statue of nymph"
			399,	// "statue of demon"
			400,	// "statue of dog"
			401,	// "statue of troll"
			518,	// "colossus",
			402,	// "statue of boy"
			403,	// "statue of girl"
			404,	// "bench",
			407,	// "coffin"
			405,	// "decorative fountain"
			408,	// "fountain",
			635,	// "ornate fountain"
			458,	// "archery target"
			511,	// "spirit cottage",
			513,	// "spirit castle"
			510,	// "spirit house"
			512,	// "spirit mansion"
			580,	// "market stall",
			678,	// "Fo obelisk"
			603,	// "monolith portal"
			604,	// "ring portal"
			605,	// "desolate portal"
			606,	// "flame portal"
			607,	// "portal"
			732,	// "epic portal"
			733,	// "huge epic portal"
			608,	// "well"
			655,	// "snowman"
			652,	// "christmas tree",
			670,	// "trash heap"
			722,	// "bell tower"
			713,	// "pylon",
			714,	// "obelisk",
			736,	// "pillar"
			717,	// "foundation pillar"
			716,	// "spirit gate"
			712,	// "shrine"
			715,	// "temple"
			811,	// "statue of horse",
			821,	// "gravestone"
			822,	// "gravestone"
			835,	// 3, "village recruitment board"
			841,	// 3, "small brazier"
			842,	// 3, "marble brazier pillar"
			855,	// 3, "steel portal"
			865,	// "pavilion"
			869,	// "Colossus of Vynora",
			870,	// "Colossus of Magranon"
			907,	// "Colossus of Fo"
			916,	// "Colossus of Libila",
			919,	// "ivy trellis"
			920,	// "grape trellis"
			938,	// "spike barrier"
			931,	// "siege shield"
			939,	// "archery tower"
			996,	// "neutral guard tower"
			999,	// "tall kingdom banner",
			1016,	// "Stone of Soulfall",
			1018,	// "rose trellis"
			1112,	// "waystone",
			1113,	// "blind catseye"
			1114,	// "catseye", 
			1116,	// "highway pointer",
			1271,	// 3, "village message board",
			1274,	// "hops trellis"
			1275,	// "hops seedling",
			1279	// "food shelf",
	};
	
	static final int[] furnitureGroup = new int[] {
			261,	// "stool"
			263,	// "chair"
			265,	// "armchair"
			484,	// "bed"
			725,	// "polearms rack"
			724,	// "weapons rack"
			758,	// "bow rack"
			759,	// "armour stand"
			815,	// "blue flowerpot"
			814,	// "yellow flowerpot"
			819,	// "greenish-yellow flowerpot"
			818,	// "orange-red flowerpot"
			816,	// "purple flowerpot"
			817,	// "white flowerpot"
			820,	// "white-dotted flowerpot"
			847,	// "brown bear rug"
			846,	// "black bear rug"
			848,	// "mountain lion rug"
			849,	// "black wolf rug",
			885,	// 2, "bedside table",
			889,	// 3, "open fireplace"
			890,	// "canopy bed"
			891,	// "bench"
			892,	// 3, "wardrobe"
			893,	// 3, "coffer"
			894,	// "royal throne"
			895,	// 3, "washing bowl"
			896,	// 2, "tripod table"
			908,	// 2, "colourful carpet",
			910,	// 4, "colourful carpet"
			909,	// 3, "colourful carpet"
			911,	// 3, "high bookshelf",
			912,	// 3, "low bookshelf"
			913,	// 3, "fine high chair"
			914,	// 3, "high chair",
			915,	// 3, "paupers high chair"
			927,	// 3, "cupboard",
			1120,	// "storage shelf",
			928,	// 3, "round marble table"
			929,	// 3, "rectangular marble table",
			987,	// "tapestry stand",
			988,	// "green tapestry"
			989,	// "beige tapestry"
			990,	// "orange tapestry",
			991,	// "cavalry motif tapestry",
			992,	// "festivities motif tapestry"
			993,	// "battle of Kyara tapestry",
			994,	// "tapestry of Faeldray",
			997,	// "valentines"
			1001,	// "marble planter",
			1003,	// "blue planter"
			1002,	// "yellow planter"
			1007,	// "greenish-yellow planter"
			1006,	// "orange-red planter"
			1004,	// "purple planter",
			1005,	// "white planter",
			1008,	// "white-dotted planter"
			1030,	// "sword display"
			1031	// "axe display"
	};

	static final int[] luxuriesGroup = new int[] {
			742,	// hota statue
			
			738, 	// "garden gnome"
			967, 	// "garden gnome"
			972, 	// "yule goat",
			1032, 	// "yule reindeer"
			
			713, 	// "pylon", (mission)
			714, 	// "obelisk", (mission)
			736, 	// "pillar", (mission)
			717, 	// "foundation pillar", (mission)
			716, 	// "spirit gate", (mission)
			712, 	// "shrine", (mission)
			715, 	// "temple", (mission)
			
			511, 	// "spirit cottage",
			513, 	// "spirit castle"
			510, 	// "spirit house",
			512 	// "spirit mansion"
	};
}
