package com.lgmrszd.compressedcreativity.client;

import com.lgmrszd.compressedcreativity.CompressedCreativity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = CompressedCreativity.MOD_ID, value = Dist.CLIENT)
public class CCClientEvents {
    
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var item = event.getItemStack().getItem();
        var itemId = event.getItemStack().getItem().builtInRegistryHolder().key().location();
        
        // Check if item is from our mod
        if (itemId.getNamespace().equals(CompressedCreativity.MOD_ID)) {
            // Add mod name tooltip (like PneumaticCraft does)
            event.getToolTip().add(Component.literal("Compressed Creativity").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        }
    }
}
