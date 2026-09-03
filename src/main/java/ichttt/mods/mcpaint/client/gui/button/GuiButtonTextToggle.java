package ichttt.mods.mcpaint.client.gui.button;

import com.mojang.blaze3d.systems.RenderSystem;
import ichttt.mods.mcpaint.MCPaint;
import ichttt.mods.mcpaint.client.gui.drawutil.EnumDrawType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.util.List;
import java.util.Locale;

public class GuiButtonTextToggle extends Button {
    public final EnumDrawType type;
    public boolean toggled = true;
    public final List<Component> tooltips;

    public GuiButtonTextToggle(int x, int y, int widthIn, int heightIn, EnumDrawType type, OnPress pressable, List<Component> tooltips) {
        super(x, y, widthIn, heightIn, Component.translatable(MCPaint.MODID + ".gui." + type.toString().toLowerCase(Locale.ENGLISH)), pressable, DEFAULT_NARRATION);
        this.type = type;
        this.tooltips = tooltips;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int x, int y, float p) {
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        boolean down = x >= this.getX() && x <= this.getX() + 12 && y >= this.getY() && y <= this.getY() + 12;
        guiGraphics.blit(getIcon(down), this.getX(), this.getY(), 0, 0, 12, 12, 12, 12);

        if (down) {
            Minecraft mc = Minecraft.getInstance();
            guiGraphics.renderComponentTooltip(mc.font, tooltips,x,y);
        }
    }

    public ResourceLocation getIcon(boolean down) {
        String location;
        switch (this.type) {
            case PENCIL -> location = "textures/gui/icon/pencil";
            case ERASER ->  location = "textures/gui/icon/eraser";
            case FILL ->  location = "textures/gui/icon/fill";
            case PICK_COLOR -> location = "textures/gui/icon/pick_color";
            default -> location = "textures/gui/icon/save";
        }
        return ResourceLocation.fromNamespaceAndPath(MCPaint.MODID,location + (down ? "_down" : "") + ".png");
    }
}
