
# Bird Addon

A general-purpose M4 mod aimed at helping players get faster runs!

[![Discord](https://img.shields.io/discord/1206694833931157514?style=flat-square&label=Discord&color=%235865F2)](https://discord.gg/QyXwYvdch6)

This mod is an addon for **Odin**:

👉 https://github.com/odtheking/Odin

- Odin must be installed to use Bird Addon
- Latest version requires **Odin 0.2.2** and **Minecraft 26.1.2**
- Use `/od` or `/birdaddon` to open the config

# Features
- Mob highlight
- Mob Counter Display
- Overkill Display
- Decoy helper to show you where the best spot to place decoys is
- Customizable Spirit Bear Timer
- Thorn Stun Timer
- Trajectories - Shows where mobs will lands (sheeps/cows/chickens)
- Sounds feature to quickly disable annoying sounds in m4 boss
- Rabbit Countdown to when rabbits spawn
- Render Optimizer - Reduces unnecessary entity and visual rendering in M4 boss fight for improved FPS
- Custom Titles for: Wish, Missed bows, Bow pickups, Bear spawn start, Bear spawned, and Bear killed
- Custom boss waypoints that dynamically update depending on what stage of boss you're on
- Boss Timer
- Logging & Replay (In Beta)

# Commands

## All features
All features can be found under the M4 category in:
- `/od`
- `/odin`
- `/birdaddon`

## Waypoints
Waypoints can be edited during or after a boss fight, or on the private islands of `catgirlm4` or `M4Miku`.

Command:
`/m4wp add <class> <start> <end>`
Adds a waypoint at the block you are looking at, visible only for the specified class and time window.

`<class>` The class to add the waypoint for.
- Supports  `bers`, `arch`, `mage`, `tank`, and `heal`
- Also supports aliases `b`, `a`, `m`, `t`, and `h`

`<start>` and `<end>` The time window of when the waypoint should be displayed. Useful for creating pointers or locations to stand.
- `b<number>spawn` When specified bear spawns. (example: `b2spawn`)
- `b<number>kill` When specified bear dies. (example: `b3kill`)
- `b<number>spawnstart` When the spawn timer for specified bear begins. (example: `b1spawnstart`)
- `<number>s` Number of seconds into the boss fight. (example: `30s`)
- `bossstart` The start of the boss
- `bossend` The end of the boss

`/m4wp remove` Removes the waypoint you are currently looking at.

`/m4wp export` Exports your waypoint configuration for sharing.
`/m4wp import` Imports a waypoint configuration.

### Replay
- `/m4rp load` – Open run selection menu
- `/m4rp play` – Start replay playback
- `/m4rp pause` – Pause playback
- `/m4rp step` – Advance 1 tick
- `/m4rp goto <time>` – Jump to a specific timestamp
    - Default unit: ticks
    - Example: `/m4rp goto 15s` & `/m4rp goto 300` both go to 15s

---

## Bug Reports & Feedback

For bugs, feature requests, or discussion, join the M4 Newgens Discord:  
[M4 Newgens Discord](discord.gg/QyXwYvdch6)