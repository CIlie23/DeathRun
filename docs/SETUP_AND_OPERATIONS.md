# DeathRun Setup And Operations Guide

This document contains full installation, setup, administration, and testing notes.

## Requirements

- Java 21
- Paper 1.21.10 (recommended)
- WorldEdit 7.2.9+

Spigot may work, but current development and testing are focused on Paper.

## Build

```bash
./gradlew :core:shadowJar -x test
```

Built jar:

```text
core/build/libs/deathrun-core-1.3.3-PATCHED-3.jar
```

## Install

1. Place the jar in your server plugins folder.
2. Install WorldEdit.
3. Start server once to generate config files.
4. Configure maps with setup commands.

## Core Player Commands

- /deathrun
- /deathrun help
- /deathrun maps
- /deathrun leave

## Admin Setup Commands

All setup commands require permission `mrstudios.command.deathrun.setup`.

### Map Management

- /deathrun setup maps list
- /deathrun setup maps use <id>
- /deathrun setup maps create <id> <world>
- /deathrun setup maps delete <id>
- /deathrun setup maps enable <id>
- /deathrun setup maps disable <id>

### Health And Status

- /deathrun setup maps check
- /deathrun setup maps check <id>
- /deathrun setup maps status
- /deathrun setup maps status <id>

### Maintenance And Recovery

- /deathrun setup maps fixbarrier <id>
- /deathrun setup maps backup <id>
- /deathrun setup maps autofix <id>
- /deathrun setup maps restore <id>

### Map Content Editing

- /deathrun setup setname <name>
- /deathrun setup setwaitinglobby
- /deathrun setup setstartbarrier (material)
- /deathrun setup addspawn <death|runner>
- /deathrun setup addtrap <type> (...args)
- /deathrun setup addcheckpoint
- /deathrun setup addteleport
- /deathrun setup save

## Multi-Map Runtime Model

- One active match runtime per map world.
- Players choose a map through /deathrun maps selector.
- Runtime tracks state independently per map: WAITING, STARTING, PLAYING, ENDING.
- End of match returns map runtime to WAITING.

## Safety Features

### Preflight Gates

Map finalize operations are blocked when critical requirements are missing.

/deathrun setup save and /deathrun setup maps disable <id> validate:

- world configured and loaded
- waiting lobby configured
- runner spawn exists
- death spawn exists
- checkpoints exist
- start barrier exists
- barrier restore snapshot consistency
- backup existence (for disable path)

### Recovery Tools

- fixbarrier rebuilds barrier restore snapshot from live blocks
- backup refreshes backup zip for target world
- autofix applies safe fixes in one pass
- restore reloads world from backup zip, rebinds map references, and reloads runtime

## Recommended Admin Workflow

1. Create/select map.
2. Configure map content.
3. Run /deathrun setup maps check <id>.
4. Run /deathrun setup maps autofix <id> if needed.
5. Run /deathrun setup save.
6. Verify with /deathrun setup maps status <id>.

## Quick Test Plan

1. Start server with plugin + WorldEdit.
2. Confirm no startup errors.
3. Run /deathrun setup maps status and /deathrun setup maps check.
4. Open selector with /deathrun maps and join map.
5. Start a round and verify:
   - match flow runs
   - end transitions back to WAITING
   - barrier restore behaves correctly
6. Test maintenance:
   - /deathrun setup maps backup <id>
   - /deathrun setup maps restore <id> when map has no players

## Configuration Files

Generated under plugin data folder:

- config.yml: gameplay timings, sounds, boosters, effects
- language.yml: all message and UI text
- map.yml: map definitions, setup data, checkpoints, traps, barriers, backups

## Known Notes

- WorldEdit is required and checked on plugin enable.
- Map restore is blocked while players are active in that map runtime.
- If map IDs are missing in old config format, they are normalized automatically.
