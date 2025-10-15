# GoldRush Plugin

A Minecraft minigame plugin themed around the Gold Rush era. Players can mine, pan, smelt, and trade gold through a dynamic economic system.

## Overview

Players mine dirt from deposits, extract gold through panning, increase purity through smelting, and sell at various trading posts. Features a dynamic price fluctuation system and multiple trading post types with different rates.

## Requirements

- **Minecraft**: 1.21.10
- **Server**: Spigot/Paper
- **Java**: 1.8+
- **Dependencies**: None (Spigot API and standard library only)

## Features

### 🪨 Mining System
- Custom blocks containing gold ore
- Dirt bundle system for collecting materials
- Progressive mining with durability system

### 💎 Panning System
- Use bowls in water to pan for gold
- Click rapidly during 3-second window (up to 50 clicks)
- Impurity rate: 3-30% (varies based on click count)

### 🔥 Smelting System
- Interactive fire power control (click rapidly)
- Optimal fire power (67.5%) removes up to 50% impurities
- Excessive fire power causes gold loss (up to 5%)
- Automatic upgrade based on gold weight

### 💰 Economic System

#### Gold Types & Price Multipliers
| Type | Multiplier |
|------|------------|
| Dust | 1.0x |
| Small Nugget | 1.05x |
| Nugget | 1.10x |
| Sheet | 1.15x |
| Ingot | 1.20x |
| Cube | 1.25x |
| Block | 1.30x |
| Jewelry | 1.50x |

#### Trading Post System
| Trading Post | Buy Rate | Sell Rate | Purity Requirement |
|--------------|----------|-----------|-------------------|
| Official Exchange | 1.00x | 1.15x | 95%+ |
| Merchant Guild | 1.03x | 1.18x | 95%+ |
| Royal Bank | 0.98x | 1.13x | 95%+ |
| Black Market | 1.15x | 1.05x | Any |
| Suspicious Dealer | 1.10x | 0.90x | Any |

#### Dynamic Price Fluctuation
- **Base Price**: $1.0/g
- **Price Range**: $0.8 ~ $1.2/g
- **Update Interval**: Every 5 minutes
- **Factors**:
  - Time passage (±3-8%)
  - Supply & demand (based on total sales)
  - Random events (20% chance every hour)

#### Price Events
| Event | Duration | Effect |
|-------|----------|--------|
| Vein Discovery | 30 min | -30% |
| Kingdom Purchase | 20 min | +40% |
| Merchant Visit | 15 min | +25% |
| Bandit Raid | 25 min | -20% |

### 🔍 Purity System
- Tracks pure gold weight and impurities separately
- 95%+ purity classified as "pure gold"
- Verification available at assay office for $2000
- Sub-95% purity cannot be traded at official exchanges

## Commands

### Player Commands
```
/customblock - Get custom block items (debug)
```

### Admin Commands
```
/goldrush money <amount> <player> - Add money to player
/goldrush menu smelt <player> - Open smelting menu
/goldrush menu sell <player> - Open selling menu
/goldrush menu shop <player> - Open shop menu
/goldrush menu check <player> - Open assay menu
/goldrush item - Get debug items
```

## Permissions
```yaml
goldrush.op: Admin permissions (default: op)
customblock.op: Custom block permissions (default: op)
```

## Gameplay Flow

1. **Mining**: Break custom blocks (BEDROCK/INFESTED_COBBLESTONE/DEAD_FIRE_CORAL_BLOCK)
2. **Collection**: Right-click clay blocks with dirt bundle
3. **Panning**: Right-click with bowl in water (click rapidly for higher purity)
4. **Smelting**: Use BLAST_FURNACE (control fire power by clicking)
5. **Assaying**: Optional purity verification at assay office
6. **Trading**: Sell gold at trading posts

## Data Storage

### Player Data
- Location: `plugins/GoldRush/players/<UUID>.yml`
- Content: Money balance

### Market Data
- Location: `plugins/GoldRush/goldmarket.yml`
- Content: Current price rate, event info, supply/demand stats

## Custom Items

All custom items are managed via NBT data with the following properties:
- Gold type (dust, small_nugget, nugget, etc.)
- Pure gold weight (g)
- Impurity weight (g)
- Pure gold verification flag
- Contraband flag

## Technical Specifications

- **Language**: Java 8
- **API Version**: 1.21
- **Package Structure**:
  - `net.raiid.goldrush` - Main logic, economic system
  - `net.raiid.customblock` - Custom block system
  - `net.raiid.util` - Utility classes

## Configuration

Currently, `config.yml` is empty. All settings are managed in code.

## Building
```bash
# Using Maven
mvn clean package

# Manual build
javac -cp spigot-1.21.jar src/net/raiid/**/*.java
jar cvf GoldRush.jar plugin.yml net/
```

## Installation

1. Place built `GoldRush.jar` in server's `plugins` folder
2. Start the server
3. Place BLAST_FURNACEs and custom blocks in world as needed

## Important Notes

- Crafting is completely disabled
- Doors, trapdoors, fence gates, signs, etc. are restricted (except in Creative mode)
- Custom block breaking requires Survival/Adventure mode
- Inventory operations are restricted during panning

## Author

**raiid_dev**

## License

All rights reserved.

## Support

For issues or suggestions, please contact the developer.

---

**Warning**: This plugin is designed for minigame use and significantly modifies vanilla gameplay. Not recommended for standard survival servers.