# Bird Addon

Mod made by BarefootBird

**This is an odin addon. It requires odin 0.1.6 for 1.21.10 or odin 0.1.9 for 1.21.11**

[Odin](https://github.com/odtheking/Odin)

Use /od to open the config

## Features
- Mob highlight
- Logging & Replay (Logs your runs so you can play them back on catgirlm4's island)
- Mob counters - Shows on the HUD how many of each mob there are, also has an option to show how many mobs are under thorn
- Overkill Display
- Customizable Spirit Bear Timer
- Thorn Stun Timer - Shows how long thorn is stunned for
- Boss Timer (Just a timer that shows how long boss has gone on for)
- Custom Titles that trigger on: bow miss, bow pickup, bear kill, bear spawn, bear spawn start
- Custom boss waypoints that dynamically update depending on where in the boss you are

## Commands

### Replay
- **/m4rp load** opens a menu to select a run to load
- **/m4rp play** starts playing the run
- **/m4rp pause** pauses the run
- **/m4rp step** steps 1 tick forward in the run
- **/m4rp goto <time>** goes to a specified point in the run, by default is in ticks, but you can do **/m4rp goto 15s** for example to go to 15s in

### Waypoints (can be edited in boss, or on catgirlm4's island)
- **/m4wp add <class> <start> <end>**  adds a waypoint where you're looking at, it will show up for the specified class and only show between the start and the end times. Start and end have the options: b<number>spawn, b<number>kill, b<number>spawnstart, <number of seconds>s, bossstart, bossend
- **/m4wp remove** removes the waypoint that you're looking at
- waypoint file is stored at .minecraft/config/odin/addons/m4waypoints.json if you want to share waypoints with other people, just put their waypoint file there and reload your game

---
If you have bugs to report, feature requests or anything else, feel free to send me a dm on discord, my disc is fredward_
