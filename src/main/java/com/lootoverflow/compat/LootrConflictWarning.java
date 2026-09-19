package com.lootoverflow.compat;

import com.lootoverflow.LootOverflowMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

/**
 * LootOverflow deliberately never touches Lootr's own chests (see OverflowAttachHandler /
 * LootFillHandler / ContainerOpenHandler) - Lootr already gives each player independent loot,
 * and this mod's overflow protection only makes sense for a single, shared loot roll. Running
 * both together isn't harmful, but the overflow protection simply won't apply to Lootr chests,
 * which is easy to mistake for a bug if nobody tells you. This just makes that visible.
 */
@Mod.EventBusSubscriber(modid = LootOverflowMod.MOD_ID)
public final class LootrConflictWarning {

    private static final String MESSAGE =
            "LootOverflow и Lootr вместе не работают: "
            + "LootOverflow намеренно не трогает сундуки Lootr "
            + "(у них своя логика лута на игрока). "
            + "Защита от переполнения для Lootr-сундуков не работает. "
            + "Если она вам не нужна именно для Lootr - "
            + "можно смело удалить один из двух модов.";

    private LootrConflictWarning() {
    }

    private static boolean lootrPresent() {
        return ModList.get().isLoaded("lootr");
    }

    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event) {
        if (lootrPresent()) {
            LootOverflowMod.LOGGER.warn("========================================================");
            LootOverflowMod.LOGGER.warn("LootOverflow: обнаружен установленный мод Lootr.");
            LootOverflowMod.LOGGER.warn(MESSAGE);
            LootOverflowMod.LOGGER.warn("========================================================");
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!lootrPresent()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(2)) {
            serverPlayer.sendSystemMessage(
                    Component.literal("[LootOverflow] " + MESSAGE).withStyle(ChatFormatting.YELLOW));
        }
    }
}
