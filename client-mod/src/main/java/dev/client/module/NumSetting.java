package dev.client.module;

import dev.client.module.Setting;

public class NumSetting
extends Setting {
    public final double min;
    public final double max;
    public final double step;
    public double value;

    public NumSetting(String name, double value, double min, double max, double step) {
        super(name);
        this.value = value;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public void set(double v) {
        v = (double)Math.round(v / this.step) * this.step;
        this.value = Math.max(this.min, Math.min(this.max, v));
    }

    public float f() {
        return (float)this.value;
    }
}

