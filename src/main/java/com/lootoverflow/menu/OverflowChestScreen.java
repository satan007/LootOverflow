package com.lootoverflow.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Renders {@link OverflowChestMenu} by reusing vanilla's own generic container texture for the
 * chest/inventory area (so no custom art asset is needed), then appending a small extra panel
 * below everything for the overflow indicator slot when one is present.
 */
public class OverflowChestScreen extends AbstractContainerScreen<OverflowChestMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

    private final int bottomBlockY;
    private final int indicatorY;

    public OverflowChestScreen(OverflowChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        int rows = menu.getRows();
        int offset = (rows - 4) * 18;
        this.bottomBlockY = rows * 18 + 17;
        this.indicatorY = 161 + offset + 18 + 16;

        this.imageWidth = 176;
        this.imageHeight = menu.hasIndicator() ? this.indicatorY + 18 + 6 : this.bottomBlockY + 96;
        this.inventoryLabelY = this.bottomBlockY + 96 - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int rows = this.menu.getRows();

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, rows * 18 + 17);
        guiGraphics.blit(TEXTURE, x, y + this.bottomBlockY, 0, 126, this.imageWidth, 96);

        if (this.menu.hasIndicator()) {
            guiGraphics.blit(TEXTURE, x + 7, y + this.indicatorY - 1, 7, 17, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        if (this.menu.hasIndicator()) {
            guiGraphics.drawString(this.font, Component.literal("Скрытый лут"), 30, this.indicatorY + 4, 0x404040, false);
        }
    }
}
