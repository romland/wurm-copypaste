# Copy / Paste for Wurm Unlimited

This server mod implements copy, cut, paste, undo:
- Landscape (elevation, tile-types, etc)
- Structures (houses, bridges, fencing, etc)
- Decorations (tables, chairs, gnomes, etc)
- Access (doors, gates, chests, etc)

I wrote the code in 2016-2017, it's now 2022 and this is my attempt at documenting this.

### Additional features
- anything copied can be rotated before pasted.
- it uses its own for serialization (saving) and deserialization (loading) of copied areas so that one can paste things between servers or worlds.
- voluntary allowing/disallowing pasting of items (say, you are only allowed to paste a maxiumum of 3 HOTA statues)


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


## Examples

[![Example Video](https://img.youtube.com/vi/7z0LZVvHGz8/0.jpg)](https://www.youtube.com/watch?v=7z0LZVvHGz8 "Example Video")



## How to install
TODO

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



## What is not finished
It's now 2022 and I am going through my notes 5-6 years after the fact.

It seems I got what I needed the mod for, namely: being able to create cities by quickly pasting decorated houses, etc.


## The code
As you might expect with mods, this is coding-on-the-run, it's very prototypy and rough around the edges. It's not commercial-grade code. :-) But knowing myself, non-obvious bits should have a comment attached.

### Random notes
- Like all of my mods, the entry point is in Mod.java
- Tabs are awesome. Shoo with your pesky spaces. Everything is in 4-space tabs, which is why indentation may look like shit if browsed on GitHub.
- A lot of debug statements are still output as info
- I have no idea why I brought in an HTTP client into the CopyPaste namespace. The author is Matthew Bell and I think it's this one: https://github.com/urgrue/java-async-http (MIT licensed). The idea behind the useage seems to be uploading creations to a central repository.
- The bulk of the tricky bits seems to be in Paste.java
- In Testing.java I seem to create a very very large Bulk Storage Bin with all resources in the game? Not sure why I had that in here.
- In Mod.java there is a way to automatically bring in something into the Copy buffer on server-start; this is for easier testing during development


## Dependencies
Just one. The JSON library from Google. It is used for (de-)serialization of copied content. The version gson-2.8.0.jar is what I used back then it seems.


## Limitations / bugs
- You can't paste underground structures/caves. It is not an impossible thing to add support for it if you familiarize yourself with the code. You would probably need to modify landscape in arbitrary ways _above_ the copied area, which might not be super trivial to navigate correctly. That's for the pasting, the copying of data should be trivial as it's just a matter of changing a 0 to 1 or so.
- You cannot copy/paste creatures (I deem it out of scope)

## TODO
Again, writing this 5-6 years after the fact
