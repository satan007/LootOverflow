package com.lootoverflow;

import com.lootoverflow.menu.LootOverflowMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(LootOverflowMod.MOD_ID)
public class LootOverflowMod {

    public static final String MOD_ID = "lootoverflow";
    public static final Logger LOGGER = LoggerFactory.getLogger("LootOverflow");

    public LootOverflowMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        LootOverflowMenus.MENUS.register(modEventBus);
        // LootFillHandler, ContainerOpenHandler and OverflowAttachHandler register themselves
        // on the Forge event bus via @Mod.EventBusSubscriber.
    }
}
