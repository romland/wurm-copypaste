# Copy / Paste for Wurm Unlimited

This server mod implements copy, cut, paste, undo:
- Landscape (elevation, tile-types, etc)
- Structures (houses, bridges, fencing, etc)
- Decorations (tables, chairs, gnomes, etc)
- Access (doors, gates, chests, etc)

I wrote the code in 2016-2017, it's now 2022 and this is my attempt at documenting this.

*Note: I do not know whether this mod works on latest version of Wurm Unlimited. In general, though, pretty small patches are needed for upgrades.*

### Additional features
- anything copied can be rotated before pasted.
- it uses its own serialization (saving) and deserialization (loading) of copied areas so that one can paste things between servers or worlds.
- voluntary allowing/disallowing pasting of items (say, you are only allowed to paste a maxiumum of 3 HOTA statues)

## Examples
[![Example Video](https://img.youtube.com/vi/G461my30wSI/0.jpg)](https://www.youtube.com/watch?v=G461my30wSI "Example Video")
[![Example Video](https://img.youtube.com/vi/7z0LZVvHGz8/0.jpg)](https://www.youtube.com/watch?v=7z0LZVvHGz8?t=85 "Example Video")
[![Example Video](https://img.youtube.com/vi/GeVNs42LwOg/0.jpg)](https://www.youtube.com/watch?v=GeVNs42LwOg "Example Video")


## The basic idea
At the fundamental level this looks like a good tool for gamemasters to quickly slap up decorated houses or deeds. For instance, building a large-scale
city becomes a piece of cake.

It's also a way to archive creations.


## The grand idea: A perpetual Wurm Unlimited server.

The longevity of WU servers was always the biggest flaw in this game:
- _Slow_ skill modifiers makes it hard to attract players (grindy, without guarantees the server will last)
- _Fast_ skill modifiers and the fun is over before it started (people will not invest in their creations)

My idea for a solution involving this mod is two-fold:
1. Use this software (mod) to transfer creations between world-resets (with possible restrictions on items)
2. Another concept (mod) to automatically scale a player's current skills to whatever the skill-modifiers are in a new world

_As an example_:
Let's say you start a server with fast skill/action modifiers (10/10), after a couple of months you reset world and set skill/action modifiers to grindy slow (1/1). All player skills will decrease and the time invested by the player is the only thing that matters.

On the occasional reset one might also want to simply do a raw deduction of skills so that it becomes more attractive to new players.

At the end of the day what would _always_ hold true is: If you were the most skilled weapon-smith on the old server, you will still be the most skilled one on the new server, albeit with a different level of skill (say, 95 vs 55).

A large swat of the code for this (skill transfer) is actually used in my mod 'wurm-vampires'
	
A map-reset is not something to fear, it's something to look forward to. Green fields, plop your deed and continue building where you left off before reset, or start fresh if you want to!

There is still work that needs to be done for this. Primarily the migration of skills, but also a friendly interface to make this not _only_ a mod for GMs.


## The less grand ideas
1. Players can find parchments (think: treasure) containing a deed or a decorated house  
  ... the player will then be able to slap up said find at a location of choice

2. Safe copying of deeds between servers  
  ... you run a server and have a deal with another server owner (shared secrets) which allow import/export of structures

3. A CTF type game  
  ... a small, limited life-time server that would start on demand when two teams want to have a go at eachother, each team bring their own
  deed. The goal is X. Winner wins, loser loses and server is shutdown. Perhaps have some kind of toplist to store the result. Caves are not copied.



## Download and Install
I think it's a matter of installing like any other server mod.

After installation the mod will create a directory at the same level as 'mods' called "Friyas-Clipboard" where all copied areas are automatically saved.

[Download here](https://github.com/romland/wurm-copypaste/releases/tag/v0.5)


## How to use
Firstly, remember that this mod has never been _used_ by anyone but myself. There will _undoubtedly_ be bugs. Feel free to fix them and submit a fix to this repository.

As a gamemaster (GM):
```
    Keybind     Console command      Description
	-------     ---------------      -----------
	ctrl+c		/copy                Copy current selected area
	ctrl+v		/paste               Paste a copied/loaded area
	ctrl+x		/cut                 Copy and delete all structures in a selected area
	ctrl+n		/clipboardsize -5    Decrease the selection size by 5 files in each direction
	ctrl+m		/clipboardsize +5    Increase the selection size by 5 files in each direction
	ctrl+b		/rotate 90 degrees   Rotate whatever you have in the copy buffer
	ctrl+z		/undo                Undo your last paste/cut/delete
	      		/load                Load a saved area into copy-buffer. No key assigned.
	      		/dir                 Show a listing of saved areas. No key assigned.
```

If I remember correctly, there is a menu called "Friya's Clipboard" if you right-click on an item, structure or tile.

_Other helpful notes_
- The /dir command returns a list allowing you to just copy a line and paste it in to load an area/structure (it is prefixed with /load for that reason).
- A menu item will appear for copying a full deed if you right click the deed token
- Oops. Looking at clips I made back then, it seems like I never stashed the copy/paste/undo/cut into a submenu, it's in the root of the main menu :-)


## The code
As you might expect with mods, this is coding-on-the-run, it's very prototypy and rough around the edges. It's not commercial-grade code. :-) But knowing myself, non-obvious bits should have a comment attached.



### Random notes
- Like all of the mods I made, the entry point is in Mod.java
- Tabs are awesome. Shoo with your pesky spaces. Everything is (should be) in 4-space tabs, which is why indentation may look like shit if browsed on GitHub.
- A lot of debug statements are still output as info
- I have no idea why I brought in an HTTP client into the CopyPaste namespace. The author is Matthew Bell and I think it's this one: https://github.com/urgrue/java-async-http (MIT licensed). The idea behind the useage seems to be uploading creations to a central repository.
- The bulk of the tricky bits seems to be in Paste.java
- In Testing.java I seem to create a very very large Bulk Storage Bin with all resources in the game? Not sure why I had that in here.
- In Mod.java there is a way to automatically bring in something into the Copy buffer on server-start; this is for easier testing during development
- There is likely a lot of commented out code in places as I was in the middle of debugging things. Especially around bridge-rotation; if you want to have a go at it, feel free to delete all the commented out code and submit a PR.

## Dependencies
Just one. The JSON library from Google. It is used for (de-)serialization of copied content. The version gson-2.8.0.jar is what I used back then it seems.


## Limitations / bugs
- You can't paste underground structures/caves. It is not an impossible thing to add support for it if you familiarize yourself with the code. You would probably need to modify landscape in arbitrary ways _above_ the copied area, which might not be super trivial to navigate correctly. That's for the pasting, the copying of data should be trivial as it's just a matter of changing a 0 to 1 or so.
- You cannot copy/paste creatures (I deem it out of scope)

## What is not finished / TODO
It's now 2022 and I am going through my notes 5-6 years after the fact.

It seems I got what I needed the mod for, namely: being able to create cities by quickly pasting decorated houses, etc.

_The old TODO list mention the following items (many of which mean nothing to me any more):_

- Primary issues
	- rotation: rotating bridges does not work (2022: I recall spending quite some time on this one!)
	- flatten: Terraforming.flattenImmediately(this.getResponder(), stx, endtx, sty, endty, 1.0f)
	- tweak rotation code for: 180 and 270 degrees on all pasted thingies - start with landscape
	- UNDO must remember which rotation we had when we pasted in (I think?), landscape did not get properly restored after an undo with rotated paste
	- Optionally include but limit at N pieces: hota statues, and other "valuable" items
	- add the paste chat command (important to be able paste from different buffers) (2022: I think done!)
	- add copy/cut/delete chat commands (2022: I think done!)
	- minedoors seems to be copied and pasted (2022: we do not copy underground)
	- the random "plans" we see in buildings, are they the center tile from structures? Do we need to add (or remove) those together with buildtiles?
	- more arrows when resizing clipboard (one every five tiles?) -- put in its own variable, not really a setting anyway
	- on deletion:
		- BUG: need to relog for bridges to disappear sometimes -- do we need to refresh tiles when deleting?
		- parapets, gravestones ... are not destroyed -- same with rope fences?
		- TODO: when deleting, say how big area we are deleting
		- TODO: Cut/Delete should set decent names of the areas too (in clipboard buffers)
		- TEST: are we deleting parapets that are *in* buildings when 'delete'? (The Friyanouce deed had a few)
		- TEST: destroy bridge in 'delete' does not seem to completely clean up (structure.totallyDestroy();)
		  see: com/wurmonline/server/behaviours/BridgePartBehaviour.java for deletion
		- BUG: If we change landscape, make sure we delete all items in the area or they may be flying/in ground
	- have a toggle button that shows the shape/size of a paste in a paste buffer (show corners with arrows or so), this means we don't have to count all the time
	- issue: copying an item should not take entire tile, just take that item
	- if exceptions come in an action, catch them so we can still return action as done or the action will be bugged for that player onwards
	- BUG: in stables of Edgehedge, some planned structures, also: there seems to be a planned building at bottom of Edgehedge entrance bridge -- I did not see it in the original
	- BUG: mine entrances (without minedoors) get copied (but turn into a hole) -- see Friyanouce deed)
	- BUG: Small bridge between the two warehouse buildings in Edgehedge is missing somehow
	- BUG: fences seem to lurk on the edge when deleted (so undoing/pasting too) (probably a 1-off bug)
	- BUGish: we get items from mines and put them on surface -- either we get mines, or we don't
	- BUG: why do I get stirred dirt when pasting the 1x1 test?
	- This seems to already be avoided: Possible ISSUE: Remove restriction for more than one building when pasting (in case there are several unfinished buildings in an area) error: [00:17:37] You cannot design a new house as your mind keeps reverting back to the house "East wall" that you are currently constructing.

	- New functionality; landscaping:
		- make hill/hole (additive)
		- smooth (some (perlin) noise on area, adapting to edges)
		- level area
		- when going below water surface, add sand on edges

	- New functionality and/or lower priority:
		- instant create completed wall, (incl. plan building unless connected to another building) -- be able to set type somehow (keybind!)
		- if bridges are linked to a building, include them when copying a structure (and vice versa)
		- rotation: diagonal pavement, how does that turn out?
		- be able to paste "flat" (landscape gets set to offset 0 height on every tile)
		- paste following landscape, but "according to rules" (roads cannot be more than 20 tiles, buildings must stand on flat, bridges must be above ground)
		- rename mod to Clipboard
		- make submenu: "Friya's Clipboard" then sub-commands under that
		- show number of tiles in buffer in paste menu? I.e: Paste (6 x 7)
		- action to examine a paste's original altitude: before pasting, it could be nice to see what height it was copied at (so you know if landscape 
		will look approximately the same)
		- TEST: can we check the 'decoration' itemtype to see if we should copy or not? what is included in that?
		- can we delay the serializing when copying? it's quite slow? or in another thread?
		- TODO: Guard towers disappear after reboot (I believe) -- maybe this is OK!
		- BUG: If we shut down too fast after modifying landscape, the changes never get to the mesh. Can we force an update of mesh?
		- TODO: stagecount on bridges does not get copied :/
		- can we do partial pasting as it's kind of slow? or in another thread?
		- TEST: can we run the copying and pasting in separate threads? Copying SHOULD be easier
		- need a dialog/preselected choices (or something) to select size of area in: copy/cut/delete (for more advanced copy from disk and into specific buffer etc)
		- make some UI element for selecting size that does NOT require you to start typing (i.e. slider, radio buttons, dropdown menu)
		- BUG: Make sure we don't paste too close to edges of map (remember that it might be rotated too)
		- wth are these items: 680, "Libila stone", 637, "freedom stones"
		- action: redo (undo an undo)
