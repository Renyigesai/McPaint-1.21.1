package ichttt.mods.mcpaint.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.util.function.Consumer;

public class HsvColorPicker extends AbstractWidget {
    private float hue = 0f;
    private float saturation = 1f;
    private float brightness = 1f;

    private final int pickerWidth;
    private final int pickerHeight;
    private final int hueBarWidth = 12;

    private boolean draggingPicker = false;
    private boolean draggingHue = false;

    private final Consumer<Integer> colorChangeListener;

    // 纹理缓存
    private DynamicTexture pickerTexture;
    private ResourceLocation pickerTextureLocation;
    private boolean textureDirty = true;

    /**
     * @param x         左上角 X
     * @param y         左上角 Y
     * @param width     主调色板区域宽度（像素）
     * @param height    主调色板区域高度（像素），色相条高度与之相同
     * @param listener  颜色变化回调
     */
    public HsvColorPicker(int x, int y, int width, int height, Consumer<Integer> listener) {
        super(x, y, width + 4 + 12, height, Component.empty());
        this.pickerWidth = width;
        this.pickerHeight = height;
        this.colorChangeListener = listener;
        setColor(0xFF0000);
        initTexture();
    }

    private void initTexture() {
        this.pickerTexture = new DynamicTexture(pickerWidth, pickerHeight, true);
        String uniqueName = "color_picker_" + System.identityHashCode(this);
        this.pickerTextureLocation = ResourceLocation.fromNamespaceAndPath("mcpaint", uniqueName);
        Minecraft.getInstance().getTextureManager().register(this.pickerTextureLocation, this.pickerTexture);
        generatePickerTexture();
    }

    private void generatePickerTexture() {
        NativeImage image = pickerTexture.getPixels();
        for (int x = 0; x < pickerWidth; x++) {
            for (int y = 0; y < pickerHeight; y++) {
                float s = (float) x / pickerWidth;
                float v = 1f - (float) y / pickerHeight;
                int argb = 0xFF000000 | Color.HSBtoRGB(hue, s, v);
                int abgr = ((argb & 0xFF000000) >>> 0) | ((argb & 0x00FF0000) >>> 16) | ((argb & 0x0000FF00) >>> 0)  | ((argb & 0x000000FF) << 16);
                image.setPixelRGBA(x, y, abgr);
            }
        }
        pickerTexture.upload();
        textureDirty = false;
    }

    public void setColor(int rgb) {
        // 从 ARGB 提取分量
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.brightness = hsv[2];
        textureDirty = true;
        notifyListener();
    }

    public int getCurrentColor() {
        return 0xFF000000 | Color.HSBtoRGB(hue, saturation, brightness);
    }

    private void notifyListener() {
        if (colorChangeListener != null) {
            colorChangeListener.accept(getCurrentColor());
        }
    }

    public float getHue() { return hue; }
    public float getSaturation() { return saturation; }
    public float getBrightness() { return brightness; }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();

        if (textureDirty) {
            generatePickerTexture();
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(pickerTextureLocation, x, y, 0, 0, pickerWidth, pickerHeight, pickerWidth, pickerHeight);

        int hueBarX = x + pickerWidth + 4;
        int hueBarY = y;
        for (int j = 0; j < pickerHeight; j++) {
            float h = (float) j / pickerHeight;
            int color = ColorUtils.hsvToRgb(h, 1f, 1f);
            graphics.fill(hueBarX, hueBarY + j, hueBarX + hueBarWidth, hueBarY + j + 1, 0xFF000000 | color);
        }

        int indicatorX = x + (int)(saturation * pickerWidth);
        int indicatorY = y + (int)((1f - brightness) * pickerHeight);
        graphics.fill(indicatorX - 3, indicatorY - 1, indicatorX + 3, indicatorY + 1, 0xFFFFFFFF);
        graphics.fill(indicatorX - 1, indicatorY - 3, indicatorX + 1, indicatorY + 3, 0xFFFFFFFF);
        graphics.fill(indicatorX - 2, indicatorY - 2, indicatorX + 2, indicatorY + 2, 0xFF000000);

        int hueIndicatorY = hueBarY + (int)(hue * pickerHeight);
        graphics.fill(hueBarX - 2, hueIndicatorY - 1, hueBarX + hueBarWidth + 2, hueIndicatorY + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;
        if (button == 0) {
            int x = getX();
            int y = getY();
            if (mouseX >= x && mouseX <= x + pickerWidth && mouseY >= y && mouseY <= y + pickerHeight) {
                draggingPicker = true;
                updatePicker(mouseX, mouseY);
                return true;
            }
            int hueBarX = x + pickerWidth + 4;
            if (mouseX >= hueBarX && mouseX <= hueBarX + hueBarWidth
                    && mouseY >= y && mouseY <= y + pickerHeight) {
                draggingHue = true;
                updateHue(mouseY);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            if (draggingPicker) {
                updatePicker(mouseX, mouseY);
                return true;
            }
            if (draggingHue) {
                updateHue(mouseY);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingPicker = false;
            draggingHue = false;
        }
        return false;
    }

    private void updatePicker(double mouseX, double mouseY) {
        int x = getX();
        int y = getY();
        float s = (float) Math.clamp((mouseX - x) / pickerWidth, 0, 1);
        float v = 1f - (float) Math.clamp((mouseY - y) / pickerHeight, 0, 1);
        this.saturation = s;
        this.brightness = v;
        notifyListener();
    }

    private void updateHue(double mouseY) {
        int y = getY();
        float h = (float) Math.clamp((mouseY - y) / pickerHeight, 0, 1);
        this.hue = h;
        textureDirty = true;
        notifyListener();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}