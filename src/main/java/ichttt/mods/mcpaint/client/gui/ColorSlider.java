package ichttt.mods.mcpaint.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ColorSlider extends AbstractSliderButton {
    private final Component prefix;
    private final double min;
    private final double max;

    public ColorSlider(int x, int y, int width, int height, Component prefix, Component suffix,
                       double min, double max, double current, boolean showTooltip) {
        super(x, y, width, height, prefix, current);
        this.prefix = prefix;
        this.min = min;
        this.max = max;
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(prefix.getString() + ": " + (int) this.value));
    }

    @Override
    protected void applyValue() {
        // 由外部回调处理，这里留空
    }

    public void setValue(double value) {
        this.value = Mth.clamp(value, min, max);
        this.updateMessage();
    }

    public int getValueInt() {
        return (int) Math.round(this.value);
    }
}