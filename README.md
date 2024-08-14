# Fabric Offline Players mod - Reworked

## Installing

Download/Build the .jar file and place it in the `/mods/` folder of your minecraft fabric server. If already running, restart the minecraft server.

> Note: This is a **Server sided mod** only.
## Goal

The goal of this mod is to allow all players to go afk on a server without the need to leave their computer on.
You can simply write /offline and an exact copy of your player will be spawned in the server, called an offline player.
Depending on configuration you'll be kicked after the offline player has spawned.

If the offline player gets killed for some reason, the offline player will not drop its loot, but instead it will just be kicked from the server.
After the offline player has died and you as a player re-join the server, the inventory and xp levels are copied over from the offline player to you, and you'll get killed.
This way you have ample time to still retrieve your items.

If the offline player survives all the way until you re-join, 
the inventory as well as the experience the offline player gained will be transferred to you.

## How to use

Write `/offline` to spawn in an offline player.

### Actions

You can also add actions to your offline player. Down below you see a table of actions available in the offline players mod:

| Name          | Description                                              |
|---------------|----------------------------------------------------------|
| attack        | Player left click                                        |
| place / use   | Player right click                                       
| crouch        | Player crouching                                         |
| jump          | Player jumping                                           |
| eat           | Player eating (only if hungry & having food in one hand) |
| drop_item     | Drops an item from the active slot                       |
| drop_stack    | Drops all items from the active slot                     |
| move_foward   | Moves the player forward                                 |
| move_backward | Moves the player backwards                               |

To use an action you can do `/offline attack`.

### Interval/offset

It is also possible to execute an action with a set interval or offset.

To create an interval do the following: 

`/offline attack:20`

This will create a 20 tick, or 1 second interval between each attack.

To add an offset to the action, you can use the following example:

`/offline attack:20:10`

This will add an interval of 10 ticks to the action.

### Chaining

It is also possible to chain actions. You can for example run the following examples:

`/offline attack move_forward`

`/offline attack:100 jump eat:20:10`

`/offline jump:20 attack eat move_backward`

But keep in mind that the first action will be run first, then the second, third, etc.

## Config

If the mod has initialized, the configuration can be found under `config/OfflinePlayersReworked.json` in the root of your minecraft server folder.

The configuration looks as follow:
```json
{
  "opRequired": false,
  "autoOp": true,
  "autoWhitelist": true,
  "autoDisconnect": true,
  "killOnDeath": true,
  "copySkin": true,
  "databaseLocation": "./offlineplayersreworked/"
}
```

- `opRequired`: If op required  to use `/offline`.
- `autoOp`: If the player is OP, automatically upgrade the offline player to OP.
- `autoWhitelist`: If the player is whitelisted, and whitelist is enabled, auto whitelist the offline player.
- `autoDisconnect`: If `true`, the player will automatically disconnect after using `/offline`. (Set to false on your own risk)
- `killOnDeath`: If `true`, the player will automatically die upon reconnecting **if** the offline player died.
- `copySkin`: If `true`, the offline player copies the original player's skin.
- `databaseLocation`: Folder location for the database. Default is `./offlinePlayersReworked/` which can be found after the mod initialized for the first time.

## Setup

For setup instructions please see the [fabric wiki page](https://fabricmc.net/wiki/tutorial:setup) that relates to the IDE that you are using.

### Fair Warning

A lot of classes were copied from carpet as I didn't want to re-inventing the wheel.
For this reason the mod is also not compatible with carpet.


## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.

