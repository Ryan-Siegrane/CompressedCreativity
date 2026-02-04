# Compressed Creativity Changelog

## Version 1.0.0 - NeoForge 1.21.1 Port

### Overview
Compressed Creativity bridges Create and PneumaticCraft: Repressurized, now fully ported to NeoForge 1.21.1 and Java 21.

### Features
- **Rotational Compressor** - Converts Create rotational force into PneumaticCraft compressed air
- **Compressed Air Engine** - Converts PneumaticCraft compressed air into Create rotational force  
- **Air Blower** - Uses compressed air to create Create-style air currents
- **Industrial Air Blower** - Advanced air blower with mesh support for item processing
- **Heater** - Heat exchanger block for thermal integration
- **Bracketed Pressure Tubes** - All three pressure tube tiers with bracket support
- **Colored Plastic Brackets** - 16 color variants
- **Compressed Iron Casing** - Industrial-themed casing block
- **Mesh Items** - Water Soaked, Woven, Dense, and Haunted meshes
- **Mechanical Visor Upgrade** - PneumaticCraft helmet upgrade

### Migration from 1.20.1 Forge
- Migrated from Forge 1.20.1 to NeoForge 1.21.1
- Updated build system to Gradle 8.8 with NeoForge ModDevGradle plugin
- Java 21 required
- All code refactored for NeoForge APIs
- Resource and tag files updated for 1.21.1 format

### Requirements
- Minecraft 1.21.1
- NeoForge 21.1+
- Create 0.6+
- PneumaticCraft: Repressurized
- Java 21

### Known Limitations
- Backtank air handler integration temporarily disabled (awaiting NeoForge capability API refactor)
- Ponder scenes temporarily disabled (integration pending)
- Recipe data generation temporarily disabled

### Credits
- **Lgmrszd** - Original author
- **Create Team** - For the amazing Create mod
- **desht** - PneumaticCraft support
- **MRHminer** - Inspiration from Create Crafts & Additions
- **Sintinium** - Update to Create 6.0.x
- **Lylythii** - Texture help
