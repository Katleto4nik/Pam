package net.gabdab.ikkooptimization;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class ConfigSlider extends SliderWidget {
    private String label;
    private double min, max, step;
    private DoubleConsumer setter;

    public ConfigSlider(int x, int y, int w, int h, String label,
                        double min, double max, double step,
                        DoubleSupplier getter, DoubleConsumer setter) {
        super(x, y, w, h, Text.empty(), (MathHelper.clamp(getter.getAsDouble(), min, max) - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.setter = setter;
        updateMessage();
    }

    private double current() {
        double raw = min + this.value * (max - min);
        double snapped = Math.round((raw - min) / step) * step + min;
        return MathHelper.clamp(snapped, min, max);
    }

    @Override
    protected void updateMessage() {
        if (label == null) return; // викликається з конструктора батька
        double v = current();
        String s = step >= 1 ? String.valueOf((int) Math.round(v)) : String.format(Locale.ROOT, "%.2f", v);
        setMessage(Text.literal(label + ": " + s));
    }

    @Override
    protected void applyValue() {
        setter.accept(current());
    }
}
