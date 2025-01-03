# Fabric Offline Players Mod - Reworked

## Installing

Download/Build the .jar file and place it in the `/mods/` folder of your Minecraft Fabric server. If the server is already running, restart it.

> Note: This is a **Server-side mod** only.

## Dependencies and Compatibility

This mod is built for Minecraft version 1.21 & 1.21.1 and is build using the following dependencies:

- Fabric Loader 0.15.11 or higher
- Fabric API 0.102.1+1.21.1 or higher
- Loom-quiltflower version 1.7.3

## Versions

- **Current Version (1.21 & 1.21.1)**: [Download v1.1.2-mc.1.21.1](https://github.com/lilfish/Fabric_OfflinePlayersReworked/releases/tag/v1.1.2-mc.1.21.1)
    - Available on modrinth: [OfflinePlayers - Modrinth](https://modrinth.com/mod/offlineplayers)
- **Legacy Version (1.18.2)**: [Download v0.1.6-mc.1.18.2](https://github.com/lilfish/Fabric_OfflinePlayersReworked/releases/tag/v0.1.6-mc.1.18.2)
    - This version is deprecated and will not be available on Modrinth.

## Goal

This mod allows players to go AFK on a server without leaving their computers on. Simply use the `/offline` command to spawn an exact copy of your player, called an offline player. Depending on the configuration, you'll be kicked after the offline player has spawned.

If the offline player is killed, it won't drop loot but will be kicked from the server. When you rejoin, the inventory and XP levels from the offline player are copied to you, and you'll be killed. This gives you time to retrieve your items.

If the offline player survives until you rejoin, its inventory and gained experience will be transferred to you.

## How to Use

Type `/offline` to spawn an offline player.

### Actions

You can add actions to your offline player. Available actions include:

| Name          | Description                                                       |
|---------------|-------------------------------------------------------------------|
| attack        | Player left click                                                 |
| break         | Player left click, but wait for block to fully break.             |
| place / use   | Player right click                                                |
| crouch        | Player crouching                                                  |
| jump          | Player jumping                                                    |
| eat           | Player eating (only if hungry & having food in one hand)          |
| drop_item     | Drops an item from the active slot                                |
| drop_stack    | Drops all items from the active slot                              |
| move_forward  | Moves the player forward                                          |
| move_backward | Moves the player backwards                                        |
 | disconnect    | Automatically disconnects the offline player after a set interval |

To use an action, type `/offline [action]`, e.g., `/offline attack`.

> Tip: use `/offline actions` to see a list of available actions

### Interval/Offset

You can execute actions with set intervals or offsets:

- Interval: `/offline break:20` (20 tick or 1 second interval between breaking blocks)
- Offset: `/offline attack:20:10` (10 tick offset added to the action)

#### Time Parsing

Besides using ticks (the default), it is also possible to set the interval and offset to milliseconds, seconds, minutes, hours, and even days.

Example: `/offline use:100ms`, `/offline attack:1s`, `/offline attack:1.5m`, `/offline eat:0.25h`, `/offline disconnect:1d`

### Chaining

You can chain actions, for example:

- `/offline attack move_forward`
- `/offline attack:100 jump eat:20:10`
- `/offline jump:20 attack eat move_backward`

Actions are executed in the order specified.

## Config

After initialization, the configuration file is located at `config/OfflinePlayersReworked.properties` in your Minecraft server's root folder.

The configuration looks as follows:
```properties
opRequired=false #               - If true, only OPs can use `/offline`. | default: false
autoOp=true #                    - If true, offline players of OPs are automatically made OPs. | default: true
autoWhitelist=false #            - If true and whitelist is enabled, offline players are auto-whitelisted. | default: false
autoDisconnect=true #            - If true, players automatically disconnect after using `/offline`. (Use false at your own risk) | default: true
killOnDeath=true #               - If true, players automatically die upon reconnecting if their offline player died. | default: true
respawnKickedPlayers=true #      - If true, offline players automatically respawn on server restart when kicked | default: true
informAboutKickedPlayer=true #   - If true, if offline player was kicked and player rejoins, player will be informed about offline player being kicked | default: true
copySkin=true #                  - If true, offline players copy the original player's skin. | default: true
ignoreSleepingPercentage=true #  - If true, offline players will not be counted towards the sleeping percentage rule. | default: true
offlinePlayerPrefix=[OFF] #       - Sets the prefix for the offline player. | default: OFF_

availableOptions=attack,break,place,use,crouch,jump,eat,drop_item,drop_stack,move_forward,move_backward,disconnect #  | default: attack,break,place,use,crouch,jump,eat,drop_item,drop_stack,move_forward,move_backward,disconnect
# ^ A comma-separated list of the available action options that can be used.
```

## Reporting Issues

If you encounter any bugs or have suggestions for improvements, please create an issue on our GitHub repository. To create an issue:

1. Go to the [Issues](https://github.com/lilfish/Fabric_OfflinePlayersReworked/issues) tab of this repository.
2. Click on "New Issue".
3. Choose between "Bug report" or "Feature request" template.
4. Fill out the template with as much detail as possible.
5. Submit the issue.

## Screenshots

### Offline Player
Able to spawn a offline player with the prefix [OFF]
![OfflinePlayersScreenshot](https://github.com/user-attachments/assets/827503c6-101b-40ff-bfc9-f511f7a2160e)

### Updated death message
When the offline player dies because of, for example, a creeper; the original player will have a updated death message
![OfflinePlayersScreenshot_Dead](https://github.com/user-attachments/assets/4a244c99-8714-4092-8bf0-16f9ec261ca2)

### Player list
Player list will show 0/20 online when only offline players are online but will still show them in the small preview list.
![OfflinePlayersScreenshot_PlayerList](https://github.com/user-attachments/assets/baf13a2a-9f8b-49fb-bb5e-c77c1a16ffa5)


## Setup

For setup instructions, please see the [Fabric wiki page](https://fabricmc.net/wiki/tutorial:setup) relevant to your IDE.

## Credits

This mod incorporates classes from the Carpet mod, which served as a valuable reference for implementation. We extend our gratitude to the Carpet mod developers for their work.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it into your own projects.
