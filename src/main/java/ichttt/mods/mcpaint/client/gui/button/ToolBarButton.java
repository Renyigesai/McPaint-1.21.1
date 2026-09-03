package ichttt.mods.mcpaint.client.gui.button;

import com.mojang.blaze3d.systems.RenderSystem;
import ichttt.mods.mcpaint.MCPaint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ToolBarButton extends Button {

    public final Type type;
    public final List<Component> tooltips;

    public ToolBarButton(Type type, List<Component> tooltips,Builder builder) {
        super(builder.size(12,12));
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
            case SAVE -> location = "textures/gui/icon/save";
            case ROTATE_RIGHT ->  location = "textures/gui/icon/rotate_right";
            case ROTATE_LEFT ->  location = "textures/gui/icon/rotate_left";
            case UNDO ->  location = "textures/gui/icon/undo";
            case REDO ->  location = "textures/gui/icon/redo";

            default -> location = "textures/gui/icon/save";
        }
        return ResourceLocation.fromNamespaceAndPath(MCPaint.MODID,location + (down ? "_down" : "") + ".png");
    }

    public enum Type{
        SAVE,
        ROTATE_RIGHT,
        ROTATE_LEFT,
        UNDO,
        REDO
    }
}
