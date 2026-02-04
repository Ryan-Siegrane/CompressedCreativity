# Comprehensive Guide: Porting CompressedCreativity from Forge 1.20.1 to NeoForge 1.21.1

## Table of Contents
1. [Overview](#overview)
2. [Project Structure Changes](#project-structure-changes)
3. [Build System Migration](#build-system-migration)
4. [Package and Import Changes](#package-and-import-changes)
5. [Main Mod Class Changes](#main-mod-class-changes)
6. [Capability System Migration](#capability-system-migration)
7. [Registration System Changes](#registration-system-changes)
8. [Block Entity Changes](#block-entity-changes)
9. [Networking System Migration](#networking-system-migration)
10. [Event System Changes](#event-system-changes)
11. [Configuration System Changes](#configuration-system-changes)
12. [Create Mod API Changes](#create-mod-api-changes)
13. [PneumaticCraft API Changes](#pneumaticcraft-api-changes)
14. [Ponder System Migration](#ponder-system-migration)
15. [Mixin Updates](#mixin-updates)
16. [Resource File Changes](#resource-file-changes)
17. [Common Issues and Solutions](#common-issues-and-solutions)
18. [Testing Checklist](#testing-checklist)

---

## Overview

This guide documents the complete process of porting CompressedCreativity from Forge 1.20.1 to NeoForge 1.21.1. The port involves significant changes due to:

- **NeoForge split from Forge** - Different packages and APIs
- **Java 17 → Java 21** - Language feature updates
- **Minecraft 1.20.1 → 1.21.1** - Vanilla API changes
- **Create 0.5.x → 6.0.x** - Major Create API overhaul
- **PneumaticCraft API updates** - New capability patterns
- **New capability system** - LazyOptional removed entirely

### Key Version Information

| Component | Old Version (1.20.1) | New Version (1.21.1) |
|-----------|---------------------|---------------------|
| Minecraft | 1.20.1 | 1.21.1 |
| Mod Loader | Forge 47.x | NeoForge 21.1.216+ |
| Java | 17 | 21 |
| Create | 0.5.x | 6.0.9 |
| PneumaticCraft | 6.x | 8.2.16+ |
| Registrate | MC1.20.1-1.3.x | MC1.21-1.3.0+67 |
| Flywheel | 0.6.x | 1.0.6 |
| Ponder | (bundled in Create) | 1.0.81 (separate) |

---

## Project Structure Changes

### Old Structure (Forge 1.20.1)
```
CompressedCreativity/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── src/
    └── main/
        ├── java/
        │   └── com/lgmrszd/compressedcreativity/
        └── resources/
            └── META-INF/mods.toml
```

### New Structure (NeoForge 1.21.1)
```
CompressedCreativity/
├── build.gradle                    # Root build file
├── gradle.properties
├── settings.gradle
├── gradle/
│   └── libs.versions.toml          # NEW: Version catalog
├── common/                         # Shared code (optional)
│   └── src/main/java/
└── neoforge/                       # NeoForge-specific
    ├── build.gradle
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── com/lgmrszd/compressedcreativity/
        │   └── resources/
        │       └── META-INF/neoforge.mods.toml  # Renamed!
        └── generated/
            └── resources/          # Data generation output
```

### Why Multi-Module?
The multi-module structure allows for potential multi-loader support (Fabric) in the future. Even if not using Fabric, this structure keeps the codebase organized.

---

## Build System Migration

### 1. Root `settings.gradle`

**Old (Forge 1.20.1):**
```gradle
pluginManagement {
    repositories {
        maven { url = 'https://maven.minecraftforge.net/' }
        gradlePluginPortal()
    }
}
```

**New (NeoForge 1.21.1):**
```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "NeoForge"
            url = "https://maven.neoforged.net/releases"
            content {
                includeGroupAndSubgroups "net.neoforged"
            }
        }
        maven {
            name = "Parchment"
            url = "https://maven.parchmentmc.org"
            content {
                includeGroupAndSubgroups "org.parchmentmc"
            }
        }
        maven {
            name = "Sponge"
            url = "https://repo.spongepowered.org/repository/maven-public/"
            content {
                includeGroupAndSubgroups "org.spongepowered"
            }
        }
    }
}

rootProject.name = 'CompressedCreativity'
include('neoforge')
```

### 2. Version Catalog (`gradle/libs.versions.toml`)

Create this new file for centralized version management:

```toml
[versions]
minecraft = "1.21.1"
neoforge = "21.1.216"
create = "0.5.1.i-66+mc1.21.1"
pneumaticcraft = "8.2.16+mc1.21.1"

[plugins]
moddevgradle = { id = "net.neoforged.moddev", version = "2.0.92" }

[libraries]
neoforge = { group = "net.neoforged", name = "neoforge", version.ref = "neoforge" }
create = { group = "com.simibubi.create", name = "create-neoforge-1.21.1", version.ref = "create" }
pneumaticcraft = { group = "me.desht.pneumaticcraft", name = "pneumaticcraft-repressurized", version.ref = "pneumaticcraft" }
```

### 3. Root `build.gradle`

```gradle
plugins {
    id 'java'
}

subprojects {
    apply plugin: 'java'

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)  // Updated from 17!
        }
    }

    repositories {
        mavenCentral()
        maven {
            name = "NeoForge"
            url = "https://maven.neoforged.net/releases"
        }
        maven {
            name = "Create Maven"
            url = "https://maven.createmod.net"
        }
        maven {
            name = "Parchment"
            url = "https://maven.parchmentmc.org"
        }
        maven {
            name = "Registrate"
            url = "https://maven.tterrag.com"
        }
    }

    version = "1.21.1-0.2.0"
    group = "com.lgmrszd.compressedcreativity"
}
```

### 4. NeoForge Module `neoforge/build.gradle`

```gradle
plugins {
    alias(libs.plugins.moddevgradle)  // NeoForge ModDevGradle plugin
    id 'maven-publish'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

version = project.mod_version
group = 'com.lgmrszd.compressedcreativity'
base {
    archivesName = 'CompressedCreativity'
}

dependencies {
    // Create mod via Curse Maven
    implementation "curse.maven:create-328085:7408951"
    
    // PneumaticCraft via Curse Maven
    implementation "curse.maven:pneumaticcraft-repressurized-281849:7535029"
    
    // Registrate - extracted from Create's JarJar bundle
    implementation files("${rootProject.projectDir}/libs/Registrate-MC1.21-1.3.0+67.jar")
    
    // Ponder - now a separate library
    implementation files("${rootProject.projectDir}/libs/ponder-neoforge-1.0.81+mc1.21.1.jar")
    
    // Flywheel - Create's rendering library
    implementation files("${rootProject.projectDir}/libs/flywheel-neoforge-1.21.1-1.0.6.jar")
}

neoForge {
    version = libs.versions.neoforge.get()
    
    runs {
        client {
            client()
        }
        
        server {
            server()
        }
        
        data {
            data()
            programArguments.addAll '--mod', 'compressedcreativity',
                '--all',
                '--output', file('src/generated/resources/').getAbsolutePath(),
                '--existing', file('src/main/resources/').getAbsolutePath()
        }
    }
    
    mods {
        compressedcreativity {
            sourceSet sourceSets.main
        }
    }
}

sourceSets.main.resources.srcDir "src/generated/resources"

tasks.withType(ProcessResources) {
    duplicatesStrategy = 'exclude'
}

repositories {
    maven {
        name = "tterrag Maven"
        url = "https://maven.tterrag.com/"
    }
    maven {
        name = "Curse Maven"
        url = "https://cursemaven.com"
        content {
            includeGroup "curse.maven"
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        filter {
            includeGroup "maven.modrinth"
        }
    }
}
```

### 5. Extracting JarJar Dependencies

Create 6.0 bundles several dependencies using JarJar. You need to extract these:

1. Download Create's jar file
2. Open with archive tool (7-Zip, WinRAR)
3. Navigate to `META-INF/jarjar/` folder
4. Extract these files to your `libs/` folder:
   - `Registrate-MC1.21-1.3.0+67.jar`
   - `ponder-neoforge-1.0.81+mc1.21.1.jar`
   - `flywheel-neoforge-1.21.1-1.0.6.jar`

---

## Package and Import Changes

### Global Package Renames

| Old Package (Forge) | New Package (NeoForge) |
|---------------------|------------------------|
| `net.minecraftforge.fml` | `net.neoforged.fml` |
| `net.minecraftforge.event` | `net.neoforged.neoforge.event` |
| `net.minecraftforge.common` | `net.neoforged.neoforge.common` |
| `net.minecraftforge.registries` | `net.neoforged.neoforge.registries` |
| `net.minecraftforge.network` | `net.neoforged.neoforge.network` |
| `net.minecraftforge.client` | `net.neoforged.neoforge.client` |
| `net.minecraftforge.eventbus.api` | `net.neoforged.bus.api` |
| `net.minecraftforge.api.distmarker` | `net.neoforged.api.distmarker` |
| `net.minecraftforge.capabilities` | `net.neoforged.neoforge.capabilities` |

### Specific Import Changes

```java
// OLD
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;

// NEW
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.api.distmarker.Dist;
```

---

## Main Mod Class Changes

### Old Pattern (Forge 1.20.1)

```java
@Mod(CompressedCreativity.MOD_ID)
public class CompressedCreativity {
    public static final String MOD_ID = "compressedcreativity";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);
    
    public CompressedCreativity() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        REGISTRATE.registerEventListeners(modEventBus);
        
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::doClientStuff);
        
        MinecraftForge.EVENT_BUS.register(this);
        
        CCItems.register();
        CCBlocks.register();
        CCBlockEntities.register();
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.COMMON_SPEC);
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        // Setup code
    }
    
    private void doClientStuff(final FMLClientSetupEvent event) {
        // Client setup
    }
}
```

### New Pattern (NeoForge 1.21.1)

```java
@Mod(CompressedCreativity.MOD_ID)
public class CompressedCreativity {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "compressedcreativity";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    // Constructor now receives IEventBus and ModContainer directly!
    public CompressedCreativity(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modEventBus);

        // Config registration changed - use ModContainer
        CCConfigHelper.init();  // Uses ModContainer internally
        CCConfigHelper.registerConfigListener(modEventBus);
        
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::doClientStuff);
        modEventBus.addListener(this::postInit);
        modEventBus.addListener(this::registerCapabilities);  // NEW: Capability registration
        modEventBus.addListener(CCNetwork::register);
        modEventBus.addListener(EventPriority.LOWEST, CompressedCreativity::gatherData);
        
        // Client-side initialization check
        if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            CCBlockPartials.init();
        }

        // Use NeoForge instead of MinecraftForge
        NeoForge.EVENT_BUS.addListener(this::serverStart);

        CCCreativeTabs.register(modEventBus);  // Creative tabs need modEventBus
        CCItems.register(modEventBus);
        CCBlocks.register();
        CCBlockEntities.register();
    }

    private void setup(final FMLCommonSetupEvent event) {
        CCCommonSetup.init(event);
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        CCClientSetup.init(event);
    }

    private void serverStart(final ServerAboutToStartEvent event) {
        // Server initialization
    }

    private void postInit(final FMLLoadCompleteEvent event) {
        // Post-initialization
    }

    // NEW: Capability registration method
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        LOGGER.info("Registering PneumaticCraft air handler capabilities");
        
        event.registerBlockEntity(
            PNCCapabilities.AIR_HANDLER_MACHINE, 
            CCBlockEntities.ROTATIONAL_COMPRESSOR.get(), 
            (be, side) -> {
                if (be instanceof IPneumaticTileEntity pneumatic) {
                    return pneumatic.getAirHandler(side);
                }
                return null;
            }
        );
        
        // Repeat for each block entity type...
    }
}
```

### Key Differences

1. **Constructor signature changed**: Now receives `IEventBus` and `ModContainer` as parameters
2. **No more `FMLJavaModLoadingContext.get().getModEventBus()`**: Event bus is passed directly
3. **`MinecraftForge` → `NeoForge`**: Global event bus renamed
4. **Config registration**: Now uses `ModContainer` instead of `ModLoadingContext`
5. **Capability registration**: New event-based system (see Capability section)

---

## Capability System Migration

This is one of the **most significant changes** between Forge and NeoForge. The `LazyOptional` pattern is completely removed.

### Old Capability Pattern (Forge 1.20.1)

```java
public class RotationalCompressorBlockEntity extends KineticBlockEntity {
    protected final IAirHandlerMachine airHandler;
    private LazyOptional<IAirHandlerMachine> airHandlerCap = LazyOptional.empty();
    
    public RotationalCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.airHandler = PneumaticRegistry.getInstance().getAirHandlerMachineFactory()
                .createAirHandler(PressureTier.TIER_ONE, 5000);
        this.airHandlerCap = LazyOptional.of(() -> airHandler);
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        airHandlerCap.invalidate();
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY) {
            if (canConnectPneumatic(side)) {
                return airHandlerCap.cast();
            }
        }
        return super.getCapability(cap, side);
    }
}
```

### New Capability Pattern (NeoForge 1.21.1)

**Step 1: Create an interface for capability providers**

```java
// IPneumaticTileEntity.java
package com.lgmrszd.compressedcreativity.blocks.common;

import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import net.minecraft.core.Direction;
import javax.annotation.Nullable;

public interface IPneumaticTileEntity {
    float getDangerPressure();
    
    @Nullable
    IAirHandlerMachine getAirHandler(@Nullable Direction side);
}
```

**Step 2: Implement in block entity (simplified)**

```java
public class RotationalCompressorBlockEntity extends KineticBlockEntity 
        implements IPneumaticTileEntity {
    
    protected final IAirHandlerMachine airHandler;
    
    public RotationalCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.airHandler = PneumaticRegistry.getInstance().getAirHandlerMachineFactory()
                .createAirHandler(
                    PressureTierConfig.CustomTier.ROTATIONAL_COMPRESSOR_TIER,
                    CommonConfig.ROTATIONAL_COMPRESSOR_VOLUME.get()
                );
    }
    
    @Override
    public void invalidate() {
        super.invalidate();
        // No LazyOptional to invalidate!
    }
    
    // Direct method instead of capability
    @Override
    public IAirHandlerMachine getAirHandler(Direction side) {
        if (canConnectPneumatic(side)) {
            return airHandler;
        }
        return null;
    }
    
    public boolean canConnectPneumatic(Direction dir) {
        Direction orientation = getBlockState().getValue(RotationalCompressorBlock.HORIZONTAL_FACING);
        return dir != Direction.UP && dir != Direction.DOWN && 
               dir != orientation && dir != orientation.getOpposite();
    }
    
    @Override
    public float getDangerPressure() {
        return airHandler.getDangerPressure();
    }
}
```

**Step 3: Register capabilities in main mod class**

```java
private void registerCapabilities(RegisterCapabilitiesEvent event) {
    // Register for each block entity type
    event.registerBlockEntity(
        PNCCapabilities.AIR_HANDLER_MACHINE,  // The capability type
        CCBlockEntities.ROTATIONAL_COMPRESSOR.get(),  // Block entity type
        (be, side) -> {  // Provider function
            if (be instanceof IPneumaticTileEntity pneumatic) {
                return pneumatic.getAirHandler(side);
            }
            return null;
        }
    );
    
    event.registerBlockEntity(
        PNCCapabilities.AIR_HANDLER_MACHINE, 
        CCBlockEntities.COMPRESSED_AIR_ENGINE.get(), 
        (be, side) -> {
            if (be instanceof IPneumaticTileEntity pneumatic) {
                return pneumatic.getAirHandler(side);
            }
            return null;
        }
    );
    
    // ... repeat for all block entities with capabilities
}
```

**Step 4: Querying capabilities**

```java
// OLD (Forge 1.20.1)
BlockEntity be = world.getBlockEntity(pos);
if (be != null) {
    be.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, side)
        .ifPresent(handler -> {
            // use handler
        });
}

// NEW (NeoForge 1.21.1) - Query from Level
IAirHandlerMachine handler = world.getCapability(
    PNCCapabilities.AIR_HANDLER_MACHINE, 
    pos, 
    side
);
if (handler != null) {
    // use handler directly
}
```

### Item Capabilities

For item capabilities (like air handlers on items), PneumaticCraft provides helper methods:

```java
// Getting air handler from ItemStack
PNCCapabilities.getAirHandler(stack).ifPresent(airHandler -> {
    float pressure = airHandler.getPressure();
    // ...
});
```

---

## Registration System Changes

### Creative Tabs Registration

**Old (Forge 1.20.1):**
```java
public class CCCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTER = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CompressedCreativity.MOD_ID);
    
    public static final RegistryObject<CreativeModeTab> BASE_CREATIVE_TAB = REGISTER.register("base",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.compressedcreativity.main"))
            .icon(() -> new ItemStack(CCBlocks.ROTATIONAL_COMPRESSOR.get(), 1))
            .build()
    );
    
    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
```

**New (NeoForge 1.21.1):**
```java
public class CCCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CompressedCreativity.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_CREATIVE_TAB = 
        REGISTER.register("base",
            () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup."+CompressedCreativity.MOD_ID+".main"))
                .icon(() -> new ItemStack(CCBlocks.ROTATIONAL_COMPRESSOR.get(), 1))
                .build()
        );

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
```

**Key Changes:**
- `RegistryObject<T>` → `DeferredHolder<T, T>` 

### Block Registration with Registrate

Block registration using Registrate remains largely similar, but some methods have changed:

```java
public class CCBlocks {
    static {
        REGISTRATE.setCreativeTab(CCCreativeTabs.BASE_CREATIVE_TAB);
    }

    public static final BlockEntry<RotationalCompressorBlock> ROTATIONAL_COMPRESSOR = 
        REGISTRATE.block("rotational_compressor", RotationalCompressorBlock::new)
            .initialProperties(SharedProperties::stone)
            .transform(TagGen.axeOrPickaxe())
            .blockstate(BlockStateGen.horizontalBlockProvider(true))
            .addLayer(() -> RenderType::cutoutMipped)  // Render layer method
            .item()
            .transform(customItemModel())
            .register();

    public static void register() {
        // Static init is enough
    }
}
```

### Block Entity Registration

**Old (Forge 1.20.1):**
```java
public static final BlockEntityEntry<RotationalCompressorBlockEntity> ROTATIONAL_COMPRESSOR = REGISTRATE
    .blockEntity("rotational_compressor", RotationalCompressorBlockEntity::new)
    .instance(() -> RotationalCompressorInstance::new, false)  // OLD: instance()
    .validBlock(CCBlocks.ROTATIONAL_COMPRESSOR)
    .renderer(() -> RotationalCompressorRenderer::new)
    .register();
```

**New (NeoForge 1.21.1):**
```java
public static final BlockEntityEntry<RotationalCompressorBlockEntity> ROTATIONAL_COMPRESSOR = REGISTRATE
    .blockEntity("rotational_compressor", RotationalCompressorBlockEntity::new)
    .visual(() -> RotationalCompressorVisual::new, false)  // NEW: visual() instead of instance()
    .validBlock(CCBlocks.ROTATIONAL_COMPRESSOR)
    .renderer(() -> RotationalCompressorRenderer::new)
    .register();
```

**Key Change:** `.instance()` → `.visual()` (Flywheel API rename)

---

## Block Entity Changes

### NBT Read/Write Methods

**Old (Forge 1.20.1):**
```java
@Override
public void write(CompoundTag compound, boolean clientPacket) {
    super.write(compound, clientPacket);
    compound.put("AirHandler", airHandler.serializeNBT());
}

@Override
protected void read(CompoundTag compound, boolean clientPacket) {
    super.read(compound, clientPacket);
    airHandler.deserializeNBT(compound.getCompound("AirHandler"));
}
```

**New (NeoForge 1.21.1):**
```java
@Override
public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
    super.write(compound, registries, clientPacket);
    compound.put("AirHandler", airHandler.serializeNBT());
    if (clientPacket) {
        compound.putDouble("airGeneratedPerTick", airGeneratedPerTick);
        compound.putBoolean("isWrongDirection", isWrongDirection);
    }
}

@Override
protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
    super.read(compound, registries, clientPacket);
    airHandler.deserializeNBT(compound.getCompound("AirHandler"));
    if (clientPacket) {
        airGeneratedPerTick = compound.getDouble("airGeneratedPerTick");
        isWrongDirection = compound.getBoolean("isWrongDirection");
    }
}
```

**Key Change:** Methods now require `HolderLookup.Provider registries` parameter for codec support.

### Block Entity Ticker

The ticker pattern remains the same:

```java
@Override
public void tick() {
    super.tick();
    airHandler.tick(this);
    
    if (updateGeneratedAir) {
        // Update logic
        updateGeneratedAir = false;
        notifyUpdate();
    }
    
    if (getLevel() != null && !getLevel().isClientSide) {
        // Server-side logic
    }
}
```

---

## Networking System Migration

The networking system has been completely rewritten. No more SimpleChannel!

### Old Pattern (Forge 1.20.1)

```java
public class CCNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(CompressedCreativity.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, ObservePacket.class, 
            ObservePacket::encode, ObservePacket::decode, ObservePacket::handle);
        INSTANCE.registerMessage(id++, ForceUpdatePacket.class,
            ForceUpdatePacket::encode, ForceUpdatePacket::decode, ForceUpdatePacket::handle);
    }
    
    public static void sendToServer(Object msg) {
        INSTANCE.sendToServer(msg);
    }
}

public class ObservePacket {
    private final BlockPos pos;
    private final int node;
    
    public ObservePacket(BlockPos pos, int node) {
        this.pos = pos;
        this.node = node;
    }
    
    public static void encode(ObservePacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.node);
    }
    
    public static ObservePacket decode(FriendlyByteBuf buf) {
        return new ObservePacket(buf.readBlockPos(), buf.readVarInt());
    }
    
    public static void handle(ObservePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // handle packet
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
```

### New Pattern (NeoForge 1.21.1)

**Network Registration:**
```java
public class CCNetwork {

    @SubscribeEvent  // Use event subscription
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");  // Version
        
        registrar.playToServer(
            ObservePacket.TYPE,
            ObservePacket.STREAM_CODEC,
            ObservePacket::handle
        );
        
        registrar.playToClient(
            ForceUpdatePacket.TYPE,
            ForceUpdatePacket.STREAM_CODEC,
            ForceUpdatePacket::handle
        );
    }
}
```

**Packet as Record with CustomPacketPayload:**
```java
public record ObservePacket(BlockPos pos, int node) implements CustomPacketPayload {
    
    // Type identifier
    public static final CustomPacketPayload.Type<ObservePacket> TYPE = 
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("compressedcreativity", "observe")
        );
    
    // Stream codec for serialization
    public static final StreamCodec<ByteBuf, ObservePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ObservePacket::pos,
        ByteBufCodecs.VAR_INT,
        ObservePacket::node,
        ObservePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ObservePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                ServerPlayer player = (ServerPlayer) ctx.player();
                if (player != null) {
                    sendUpdate(pkt, player);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void sendUpdate(ObservePacket pkt, ServerPlayer player) {
        BlockEntity te = player.level().getBlockEntity(pkt.pos());
        if (te instanceof IObserveTileEntity) {
            ((IObserveTileEntity)te).onObserved(player, pkt);
            Packet<ClientGamePacketListener> updatePacket = te.getUpdatePacket();
            if (updatePacket != null) {
                player.connection.send(updatePacket);
            }
        }
    }

    // Sending to server
    public static void send(BlockPos pos, int node) {
        if (cooldown <= 0) {
            cooldown = 10;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new ObservePacket(pos, node)
            );
        }
    }
}
```

**Client-bound Packet:**
```java
public record ForceUpdatePacket(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ForceUpdatePacket> TYPE =
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("compressedcreativity", "force_update")
        );

    public static final StreamCodec<ByteBuf, ForceUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ForceUpdatePacket::pos,
        ForceUpdatePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ForceUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            handlePacket(packet, context);
        });
    }

    private static void handlePacket(ForceUpdatePacket packet, IPayloadContext context) {
        BlockPos pos = packet.pos();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.isLoaded(pos)) return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IUpdateBlockEntity ube)) return;
        ube.forceUpdate();
    }

    // Sending to tracking clients
    public static void send(Level world, BlockPos pos) {
        PacketDistributor.sendToPlayersTrackingChunk(
            (net.minecraft.server.level.ServerLevel) world, 
            new net.minecraft.world.level.ChunkPos(pos), 
            new ForceUpdatePacket(pos)
        );
    }
}
```

**Key Changes:**
1. Use `record` for immutable packet data
2. Implement `CustomPacketPayload` interface
3. Define `Type<>` static field with ResourceLocation
4. Use `StreamCodec` instead of manual encode/decode
5. Handler receives `IPayloadContext` instead of `Supplier<NetworkEvent.Context>`
6. Use `PacketDistributor` static methods for sending

---

## Event System Changes

### Event Bus Subscriber

**Old (Forge 1.20.1):**
```java
@Mod.EventBusSubscriber(modid = CompressedCreativity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCColorHandlers {
    @SubscribeEvent
    public static void registerBlockColorHandlers(ColorHandlerEvent.Block event) {
        event.getBlockColors().register(/* ... */);
    }
}
```

**New (NeoForge 1.21.1):**
```java
@EventBusSubscriber(modid = CompressedCreativity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CCColorHandlers {
    @SubscribeEvent
    public static void registerBlockColorHandlers(RegisterColorHandlersEvent.Block event) {
        event.register(CCColorHandlers::getTintColor, CCBlocks.INDUSTRIAL_AIR_BLOWER.get());
        event.register(CCColorHandlers::getTintColor, CCBlocks.HEATER.get());
    }

    public static int getTintColor(BlockState state, @Nullable BlockAndTintGetter world, 
                                   @Nullable BlockPos pos, int tintIndex) {
        if (world != null && pos != null) {
            BlockEntity te = world.getBlockEntity(pos);
            return te instanceof ITintedBlockEntity tinted 
                ? tinted.getTintColor(tintIndex) 
                : TintColor.WHITE.getARGB();
        }
        return -1;
    }
}
```

**Key Changes:**
- `Mod.EventBusSubscriber` → `EventBusSubscriber`
- `ColorHandlerEvent.Block` → `RegisterColorHandlersEvent.Block`
- Add `value = Dist.CLIENT` for client-only events

### Client Tick Event

**Old:**
```java
@SubscribeEvent
public static void clientTickEvent(TickEvent.ClientTickEvent evt) {
    if (evt.phase == TickEvent.Phase.START) {
        // tick logic
    }
}
```

**New:**
```java
@SubscribeEvent
public static void clientTickEvent(ClientTickEvent.Pre evt) {
    // Pre = START, Post = END
    ObservePacket.tick();
}
```

**Key Change:** `TickEvent.ClientTickEvent` with phase → `ClientTickEvent.Pre` or `ClientTickEvent.Post`

---

## Configuration System Changes

### Config Registration

**Old (Forge 1.20.1):**
```java
public CompressedCreativity() {
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.COMMON_SPEC);
    ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC);
}
```

**New (NeoForge 1.21.1):**
```java
public class CCConfigHelper {
    public static void init() {
        ModContainer container = ModList.get()
            .getModContainerById(CompressedCreativity.MOD_ID)
            .orElseThrow();
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC);
    }

    public static void registerConfigListener(IEventBus modEventBus) {
        modEventBus.addListener(CCConfigHelper::onConfigChanged);
    }

    private static void onConfigChanged(final ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (config.getSpec() == ClientConfig.CLIENT_SPEC) {
            refreshClient();
        }
    }
}
```

### Config Spec Definition

The config definition syntax remains the same:

```java
public class CommonConfig {
    public static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec COMMON_SPEC;

    public static final ModConfigSpec.IntValue ROTATIONAL_COMPRESSOR_VOLUME;
    public static final ModConfigSpec.DoubleValue ROTATIONAL_COMPRESSOR_BASE_PRODUCTION;

    static {
        COMMON_BUILDER.comment("Rotational Compressor Settings").push("rotational_compressor");
        
        ROTATIONAL_COMPRESSOR_VOLUME = COMMON_BUILDER
            .comment("Air Volume of the machine")
            .defineInRange("volume", 5000, 0, Integer.MAX_VALUE);
            
        ROTATIONAL_COMPRESSOR_BASE_PRODUCTION = COMMON_BUILDER
            .comment("Base air production rate")
            .defineInRange("base_production", 10.0, 0.0, 100.0);
            
        COMMON_BUILDER.pop();
        
        COMMON_SPEC = COMMON_BUILDER.build();
    }
}
```

---

## Create Mod API Changes

### Lang System

**Old (Create 0.5.x):**
```java
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;

Lang.translate("tooltip.pressure", value)
    .forGoggles(tooltip);
```

**New (Create 6.0.x):**
```java
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import com.simibubi.create.foundation.utility.CreateLang;

public class CCLang {
    public static LangBuilder builder() {
        return new LangBuilder(CompressedCreativity.MOD_ID);
    }

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }

    public static LangBuilder number(double d) {
        return builder().text(LangNumberFormat.format(d));
    }
}

// Usage:
CCLang.translate("tooltip.pressure")
    .style(ChatFormatting.GRAY)
    .forGoggles(tooltip);
    
CCLang.number(airHandler.getPressure())
    .translate("unit.bar")
    .style(ChatFormatting.AQUA)
    .forGoggles(tooltip, 1);
```

**Key Changes:**
- `Lang` class moved to `net.createmod.catnip.lang`
- Create a wrapper class (`CCLang`) for your mod's translations
- Use `CreateLang` for Create-specific translations

### Goggle Interfaces

**Old:**
```java
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.goggles.IHaveHoveringInformation;
```

**New:**
```java
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
```

### Rendering and Animation

**Old (Flywheel 0.6.x):**
```java
import com.jozufozu.flywheel.api.InstanceData;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;
import com.jozufozu.flywheel.core.PartialModel;

public class RotationalCompressorInstance extends KineticBlockEntityInstance<RotationalCompressorBlockEntity> {
    // ...
}
```

**New (Flywheel 1.0.x):**
```java
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

public class RotationalCompressorVisual extends KineticBlockEntityVisual<RotationalCompressorBlockEntity> {
    protected final RotatingInstance shaft;
    protected final RotatingInstance fan;
    final Direction direction;

    public RotationalCompressorVisual(VisualizationContext context, 
                                      RotationalCompressorBlockEntity tile, 
                                      float partialTick) {
        super(context, tile, partialTick);

        direction = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction opposite = direction.getOpposite();
        
        shaft = instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
            .createInstance();
            
        fan = instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.ENCASED_FAN_INNER))
            .createInstance();

        shaft.setup(tile)
            .setPosition(getVisualPosition())
            .rotateToFace(Direction.SOUTH, opposite)
            .setChanged();

        fan.setup(tile, getFanSpeed())
            .setPosition(getVisualPosition())
            .rotateToFace(Direction.SOUTH, opposite)
            .setChanged();
    }

    @Override
    public void update(float partialTick) {
        shaft.setup(blockEntity).setChanged();
        fan.setup(blockEntity, getFanSpeed()).setChanged();
    }

    @Override
    protected void _delete() {
        shaft.delete();
        fan.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(shaft);
        consumer.accept(fan);
    }
}
```

**Key Changes:**
- `Instance` suffix → `Visual` suffix
- Package moved to `dev.engine_room.flywheel`
- `Materializer` → `instancerProvider().instancer()`
- Constructor takes `VisualizationContext` instead of `MaterialManager`
- `remove()` → `_delete()`
- Must implement `collectCrumblingInstances()`

### PartialModel Registration

**Old:**
```java
import com.jozufozu.flywheel.core.PartialModel;

public static final PartialModel AIR_ENGINE_ROTOR = new PartialModel(
    new ResourceLocation(CompressedCreativity.MOD_ID, "block/compressed_air_engine/rotor")
);
```

**New:**
```java
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

public static final PartialModel AIR_ENGINE_ROTOR = PartialModel.of(
    ResourceLocation.fromNamespaceAndPath(
        CompressedCreativity.MOD_ID, 
        "block/compressed_air_engine/rotor"
    )
);
```

### Block Renderer

**Old:**
```java
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
```

**New:**
```java
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.animation.AnimationTickHolder;
```

### Schematic/ItemRequirement

**Old:**
```java
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;
```

**New:**
```java
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
```

### Stress Values API

**Old:**
```java
import com.simibubi.create.content.kinetics.BlockStressValues;
```

**New:**
```java
import com.simibubi.create.api.stress.BlockStressValues;
```

---

## PneumaticCraft API Changes

### Heat Exchanger

The heat exchanger API has some method signature changes:

```java
// Creating heat exchanger
IHeatExchangerLogic heatExchanger = PneumaticRegistry.getInstance()
    .getHeatRegistry()
    .makeHeatExchangerLogic();
    
heatExchanger.setThermalCapacity(5);

// Connecting exchangers
airExchanger.addConnectedExchanger(heatExchanger);
airExchanger.setThermalResistance(25.0);

// Initialize as hull (for blocks with exposed sides)
heatExchanger.initializeAsHull(
    getLevel(), 
    getBlockPos(), 
    (levelAccessor, blockPos) -> true,  // Side validity predicate 
    sidesArray
);
```

### Pressure Tubes

```java
// Getting PNC blocks
Block tube = BuiltInRegistries.BLOCK.get(
    ResourceLocation.fromNamespaceAndPath(PneumaticRegistry.MOD_ID, "pressure_tube")
);

// Force shape recalculation (after placing/removing tubes)
IMiscHelpers miscHelpers = PneumaticRegistry.getInstance().getMiscHelpers();
miscHelpers.forceClientShapeRecalculation(world, pos);
```

### Air Particle

```java
public static ParticleOptions getAirParticle() {
    IMiscHelpers miscHelpers = PneumaticRegistry.getInstance().getMiscHelpers();
    return miscHelpers.airParticle();
}
```

### Upgrade Registration

```java
public static final PNCUpgrade MECHANICAL_VISOR = PneumaticRegistry.getInstance()
    .getUpgradeRegistry()
    .registerUpgrade(CCMisc.CCRL("mechanical_visor"));
```

---

## Ponder System Migration

Ponder has been split into a separate library in Create 6.0.

### Plugin Registration

Create a `PonderPlugin` implementation:

```java
package com.lgmrszd.compressedcreativity.index;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.registration.*;
import net.minecraft.resources.ResourceLocation;

public class CCPonderPlugin implements PonderPlugin {
    
    @Override
    public String getModId() {
        return CompressedCreativity.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        CCPonder.registerScenes(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        CCPonder.registerTags(helper);
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        // Register shared text resources
    }

    @Override
    public void onPonderLevelRestore(PonderLevel ponderLevel) {
        // Called when ponder level resets
    }

    @Override
    public void indexExclusions(IndexExclusionHelper helper) {
        // Exclude items from ponder index
    }
}
```

### Scene Registration

```java
public class CCPonder {
    public static final ResourceLocation PRESSURE = 
        ResourceLocation.fromNamespaceAndPath(CompressedCreativity.MOD_ID, "pressure");

    public static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<RegistryEntry<?, ?>> HELPER = 
            helper.withKeyFunction(RegistryEntry::getId);

        HELPER.registerTag(PRESSURE)
            .addToIndex()
            .item(CCBlocks.ROTATIONAL_COMPRESSOR.get())
            .title("Pressure")
            .description("Components which use pressurized air")
            .register();

        HELPER.addToTag(PRESSURE)
            .add(CCBlocks.ROTATIONAL_COMPRESSOR)
            .add(CCBlocks.COMPRESSED_AIR_ENGINE)
            .add(CCBlocks.AIR_BLOWER);
    }

    public static void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER = 
            helper.withKeyFunction(RegistryEntry::getId);

        HELPER.addStoryBoard(
            CCBlocks.ROTATIONAL_COMPRESSOR, 
            "rotational_compressor", 
            PonderScenes::rotationalCompressor, 
            PRESSURE
        );
    }
}
```

### Scene Building

```java
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;

public class PonderScenes {
    public static void rotationalCompressor(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);  // Wrap for Create helpers
        
        scene.title("rotational_compressor", "Generating Pressure using a Rotational Compressor");
        scene.configureBasePlate(0, 1, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);

        BlockPos compressor = util.grid().at(2, 1, 3);

        scene.idle(5);
        scene.world().showSection(util.select().fromTo(3, 1, 0, 2, 2, 2), Direction.DOWN);
        
        scene.overlay().showText(50)
            .text("The Rotational Compressor generates pressure from rotation")
            .placeNearTarget()
            .pointAt(util.vector().topOf(compressor));
            
        scene.idle(60);
        // ...
    }
}
```

### Service Registration

Register your plugin in `META-INF/services/net.createmod.ponder.api.registration.PonderPlugin`:

```
com.lgmrszd.compressedcreativity.index.CCPonderPlugin
```

---

## Mixin Updates

### Mixin Configuration

**mixins.compressedcreativity.json:**
```json
{
  "required": true,
  "package": "com.lgmrszd.compressedcreativity.mixin",
  "compatibilityLevel": "JAVA_21",  // Updated from JAVA_17
  "refmap": "mixins.compressedcreativity.refmap.json",
  "mixins": [
    "create.AirCurrentMixin",
    "create.BackTankUtilMixin",
    "create.BracketBlockMixin",
    "create.ChuteBlockEntityMixin"
  ],
  "client": [
    "create.BackTankUtilMixinClient"
  ],
  "injectors": {
    "defaultRequire": 1
  },
  "minVersion": "0.8"
}
```

### Register in neoforge.mods.toml

```toml
[[mixins]]
    config = "mixins.compressedcreativity.json"
```

### Example Mixin Update

The mixin syntax largely remains the same, but target method signatures may change:

```java
@Mixin(AirCurrent.class)
public class AirCurrentMixin {
    @ModifyVariable(
        method = "rebuild",
        at = @At(value = "STORE", ordinal = 0),
        name = "type",
        remap = false  // Create's methods aren't remapped
    )
    public FanProcessingType AirBlowerMeshProcessingTypeInRebuild(
            FanProcessingType type, 
            @Local(name = "world") Level world,  // Use @Local from MixinExtras
            @Local(name = "start") BlockPos start
    ) {
        BlockEntity be = world.getBlockEntity(start);
        if (be instanceof AdvancedAirBlowerBlockEntity abbe) {
            return abbe.getProcessingType().orElse(type);
        }
        return type;
    }
}
```

**Key Update:** Use MixinExtras' `@Local` annotation for capturing local variables cleanly.

---

## Resource File Changes

### Mod Descriptor

**Old: `META-INF/mods.toml`**

**New: `META-INF/neoforge.mods.toml`**

```toml
modLoader = "javafml"
loaderVersion = "[4,)"
license = "MIT"

[[mods]]
    modId = "compressedcreativity"
    version = "${file.jarVersion}"
    displayName = "Compressed Creativity"
    displayURL = "https://github.com/Lgmrszd/CompressedCreativity"
    logoFile = "compcreat_logo.png"
    authors = "Lgmrszd"
    description = '''
Bridging Create and PneumaticCraft: Repressurized together
'''

[[mixins]]
    config = "mixins.compressedcreativity.json"

[[dependencies.compressedcreativity]]
    modId = "neoforge"
    type = "required"
    versionRange = "[21.1,)"
    ordering = "NONE"
    side = "BOTH"

[[dependencies.compressedcreativity]]
    modId = "minecraft"
    type = "required"
    versionRange = "[1.21.1,1.22)"
    ordering = "NONE"
    side = "BOTH"

[[dependencies.compressedcreativity]]
    modId = "create"
    type = "required"
    versionRange = "[0.6,)"
    ordering = "NONE"
    side = "BOTH"
```

### Pack Format

**pack.mcmeta:**
```json
{
  "pack": {
    "pack_format": 34,
    "description": "Compressed Creativity Mod Resources"
  }
}
```

| MC Version | Pack Format |
|------------|-------------|
| 1.20.1 | 15 |
| 1.21.1 | 34 |

### ResourceLocation Changes

**Old:**
```java
new ResourceLocation(MOD_ID, "path")
new ResourceLocation("minecraft", "stone")
```

**New:**
```java
ResourceLocation.fromNamespaceAndPath(MOD_ID, "path")
ResourceLocation.withDefaultNamespace("stone")  // For minecraft namespace
```

---

## Common Issues and Solutions

### Issue 1: `NoClassDefFoundError` for Forge classes

**Symptom:** Runtime crash looking for `net.minecraftforge.*` classes

**Solution:** Search and replace all Forge imports with NeoForge equivalents. Use IDE's global search:
- `net.minecraftforge.fml` → `net.neoforged.fml`
- `net.minecraftforge.event` → `net.neoforged.neoforge.event`
- etc.

### Issue 2: `LazyOptional` not found

**Symptom:** Compile error - `LazyOptional` class doesn't exist

**Solution:** The capability system was completely rewritten. Remove all `LazyOptional` usage and implement the new capability pattern. See [Capability System Migration](#capability-system-migration).

### Issue 3: Create's Lang class not found

**Symptom:** `Lang` class from Create doesn't exist at expected location

**Solution:** Create 6.0 moved utility classes to `net.createmod.catnip`. Create your own `CCLang` wrapper class.

### Issue 4: Instance rendering doesn't work

**Symptom:** Blocks render statically, no animation

**Solution:** 
1. Rename `*Instance` classes to `*Visual`
2. Update to Flywheel 1.0 API
3. Change `.instance()` to `.visual()` in registration

### Issue 5: Recipes don't generate

**Symptom:** Data generation produces no recipe files

**Solution:** Create 6.0 made `CreateRecipeProvider` final. You need to use a different approach for recipe generation, or use JSON-based recipes in resources.

### Issue 6: NBT methods have wrong signature

**Symptom:** `read()` and `write()` methods don't compile

**Solution:** Add `HolderLookup.Provider registries` parameter to both methods.

### Issue 7: Networking packets don't arrive

**Symptom:** Packets are sent but never handled

**Solution:** Ensure you:
1. Use `record` types implementing `CustomPacketPayload`
2. Define `TYPE` and `STREAM_CODEC` static fields
3. Register using `RegisterPayloadHandlersEvent`
4. Use correct distributor (`playToServer` vs `playToClient`)

### Issue 8: Capabilities return null

**Symptom:** `world.getCapability()` always returns null

**Solution:** Ensure capability registration happens in `RegisterCapabilitiesEvent`, and that you're using the correct capability constant from the library (e.g., `PNCCapabilities.AIR_HANDLER_MACHINE`).

### Issue 9: Config values are null

**Symptom:** `ConfigValue.get()` returns null or crashes

**Solution:** Config must be registered through `ModContainer`, not `ModLoadingContext`. See [Configuration System Changes](#configuration-system-changes).

### Issue 10: Ponder scenes don't appear

**Symptom:** Ponder button exists but no scenes show

**Solution:**
1. Create `PonderPlugin` implementation
2. Register in `META-INF/services/net.createmod.ponder.api.registration.PonderPlugin`
3. Ensure NBT files exist in `assets/<modid>/ponder/`

---

## Testing Checklist

Before releasing your port, verify each of these works:

### Basic Functionality
- [ ] Mod loads without crashes
- [ ] All blocks appear in creative tab
- [ ] All items appear in creative tab
- [ ] Blocks can be placed and broken
- [ ] Block entities are created correctly

### Create Integration
- [ ] Kinetic blocks connect to shafts
- [ ] Speed/stress values are correct
- [ ] Goggle tooltips display
- [ ] Hovering information displays
- [ ] Block rendering/animation works
- [ ] Wrench interactions work

### PneumaticCraft Integration
- [ ] Air handlers connect to tubes
- [ ] Pressure transfers correctly
- [ ] Heat exchangers work (if applicable)
- [ ] Upgrades can be installed
- [ ] Helmet HUD integration works

### Networking
- [ ] Client-server sync works
- [ ] Block entity updates replicate
- [ ] Observer packets function

### Configuration
- [ ] Common config loads
- [ ] Client config loads
- [ ] Config changes apply correctly
- [ ] Config UI displays (if applicable)

### Ponder (if applicable)
- [ ] Ponder tag appears
- [ ] Scenes play correctly
- [ ] Animations work
- [ ] Text displays correctly

### Multiplayer
- [ ] Server starts with mod
- [ ] Client can connect
- [ ] All features work in MP

---

## Conclusion

Porting from Forge 1.20.1 to NeoForge 1.21.1 is a significant undertaking due to:

1. **NeoForge API changes** - New capability system, event patterns
2. **Java 21 requirement** - Record patterns, new features
3. **Create 6.0 overhaul** - Flywheel 1.0, Ponder extraction, API refactors
4. **Minecraft 1.21 changes** - Method signatures, resource formats

Take your time, work through each system methodically, and test extensively. The new APIs are generally cleaner and more consistent once you understand the patterns.

### Resources

- [NeoForge Documentation](https://docs.neoforged.net/)
- [Create GitHub](https://github.com/Creators-of-Create/Create)
- [PneumaticCraft API](https://github.com/TeamPneumatic/pnc-repressurized)
- [Flywheel Wiki](https://github.com/Engine-Room/Flywheel/wiki)
- [Ponder Library](https://github.com/Create-Mod/Ponder)

---

*Guide written during the CompressedCreativity port, February 2026*
