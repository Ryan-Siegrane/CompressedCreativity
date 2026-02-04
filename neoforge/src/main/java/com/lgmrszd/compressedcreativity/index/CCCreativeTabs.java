package com.lgmrszd.compressedcreativity.index;

import com.lgmrszd.compressedcreativity.CompressedCreativity;
import com.simibubi.create.AllCreativeModeTabs;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CCCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CompressedCreativity.MOD_ID);

    // PneumaticCraft's creative tab ResourceKey
    private static final ResourceKey<CreativeModeTab> PNC_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(PneumaticRegistry.MOD_ID, "default"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_CREATIVE_TAB = REGISTER.register("base",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup."+CompressedCreativity.MOD_ID+".main"))
                    .icon(() -> new ItemStack(CCBlocks.ROTATIONAL_COMPRESSOR.get(), 1))
                    // Items are added by Registrate via REGISTRATE.setCreativeTab()
                    .build());

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
