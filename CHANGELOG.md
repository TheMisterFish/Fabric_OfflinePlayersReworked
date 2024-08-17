# OfflinePlayers-Reworked 1.0.6 (MC 1.21.1)

**Updates:**
- Upgraded to 1.21.1 (still works with 1.21)
- Added command suggestions for Action:Interval:Offset
- Added version command to verify current version
- Added ActionType mapper & validation so no incorrect ActionType can be added
- Library updates:
  * fabric_version -> 0.102.1+1.21.1
  * midnightlib_version -> 1.5.8
  * guava_version -> 33.3.0-jre
# OfflinePlayers-Reworked 1.0.5 (MC 1.21)

**Updates:**
- Added check to see if item is consumable which includes bottles & milk. 
- Added a check that makes sure that no ominous effect is active. Without this check it can reset the raid effect causing no raid to start.

# OfflinePlayers-Reworked 1.0.4 (MC 1.21)

**Updates:**
- Fix OfflinePlayer not spawning because of no chat session, replaced with a warning log.
- Updated mod icon

# OfflinePlayers-Reworked 1.0.3 (MC 1.21)

**Updates:**
- Fix startup bug because of missing libraries.

# OfflinePlayers-Reworked 1.0.2 (MC 1.21)

**Updates:**
- Added prefix option in config
- Added time parser to parse times such as 100ms, 1s, 1m, 1.5h, 0.5d
- Added disconnect action to allow offline player to disconnect after set time
- Better exception handling of invalid/lost playerdata of offlinePlayer
- Minor bug fixes


# OfflinePlayers-Reworked 1.0.1 (MC 1.21)

**Updates:**
- Carpet is now compatible
- /kick now kick's the offline player (+ new option `informAboutKickedPlayer` in config to inform the player his offline player had been kicked)
- Offline players now able to be created in the nether and the end
- On server restart, offline players will be recreated (+ new option `respawnKickedPlayers` in config to also recreate kicked offline players)
- Configurable actions using `availableOptions` config
- Minor bug fixes

# OfflinePlayers-Reworked 1.0.0 (MC 1.21)

Full rework of the mod. Updated to minecraft version 1.21.

**Updates:**  
- Chaining different actions
- Configuration using the config.json
- No DB usage for storing items, switched to using player data.
- Better death message for both server & clients (using packages)
- Readme update
