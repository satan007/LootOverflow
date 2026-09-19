package com.lootoverflow.menu;

import com.lootoverflow.LootOverflowMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class LootOverflowMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, LootOverflowMod.MOD_ID);

    public static final RegistryObject<MenuType<OverflowChestMenu>> OVERFLOW_CHEST = MENUS.register(
            "overflow_chest", () -> IForgeMenuType.create(OverflowChestMenu::fromNetwork));

    private LootOverflowMenus() {
    }
}
