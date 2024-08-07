# Fabric Offline Players mod - Reworked
## Setup

For setup instructions please see the [fabric wiki page](https://fabricmc.net/wiki/tutorial:setup) that relates to the IDE that you are using.

### Warning

A lot of classes were copied from carpet as I didn't feel like re-inventing the wheel.
The goal of this mod is to be able to spawn an offline-player which will do the AFK-ing for you. 
You as a player will be kicked from the server after spawning a fake player.

When you reconnect, the offline-player will transfer its inventory content back to you and will be kicked.

In case that the offline-player dies, it will not immediately drop it's content. Instead, the server waits for you to rejoin.
It will than transfer the inventory of the offline-player to you, and then kill you. This ensures that you still have a 
5-minute window to retrieve your items.

### Planned features/updates:

- [] Update to 1.21 and rewrite mod :D
- [] Aggro mobs (so that aggrasive mobs target NPC or Player after going offline or online)
- [] ~~Achievements sync~~ <sub>decided to not do this as NPC's are allowed to have their own advancements</sub> 
- [] ~~Stats sync~~ <sub>decided to not do this as NPC's are allowed to have their own stats</sub>
- [] Fix NPC Class clutterness (add helper classes)
- [] Clone sleep time attributes for Phantom spawning mechenics
- [] Auto respawn on server restart

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
