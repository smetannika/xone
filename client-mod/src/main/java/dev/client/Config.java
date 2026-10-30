package dev.client;

import dev.client.module.BindSetting;
import dev.client.module.BoolSetting;
import dev.client.module.HudModule;
import dev.client.module.Module;
import dev.client.module.ModuleManager;
import dev.client.module.NumSetting;
import dev.client.module.Setting;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public class Config {
    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("clientmod.properties");
    }

    public static void save() {
        Properties p = new Properties();
        for (Module m : ModuleManager.modules) {
            p.setProperty(m.name + ".enabled", String.valueOf(m.enabled));
            p.setProperty(m.name + ".key", String.valueOf(m.key));
            if (m instanceof HudModule) {
                p.setProperty(m.name + ".x", String.valueOf(((HudModule)m).x));
                p.setProperty(m.name + ".y", String.valueOf(((HudModule)m).y));
            }
            for (Setting s : m.settings) {
                String k = m.name + "." + s.name;
                if (s instanceof BoolSetting) {
                    p.setProperty(k, String.valueOf(((BoolSetting)s).value));
                }
                if (s instanceof NumSetting) {
                    p.setProperty(k, String.valueOf(((NumSetting)s).value));
                }
                if (!(s instanceof BindSetting)) continue;
                p.setProperty(k, String.valueOf(((BindSetting)s).key));
            }
        }
        try (OutputStream out = Files.newOutputStream(Config.file(), new OpenOption[0]);){
            p.store(out, "client mod");
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void load() {
        Path f = Config.file();
        if (!Files.exists(f, new LinkOption[0])) {
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(f, new OpenOption[0]);){
            p.load(in);
        }
        catch (Exception e) {
            return;
        }
        for (Module m : ModuleManager.modules) {
            try {
                m.key = Integer.parseInt(p.getProperty(m.name + ".key", "-1"));
                m.enabled = Boolean.parseBoolean(p.getProperty(m.name + ".enabled", String.valueOf(m.enabled)));
                if (m instanceof HudModule) {
                    HudModule hm = (HudModule)m;
                    hm.x = Integer.parseInt(p.getProperty(m.name + ".x", String.valueOf(hm.x)));
                    hm.y = Integer.parseInt(p.getProperty(m.name + ".y", String.valueOf(hm.y)));
                }
                for (Setting s : m.settings) {
                    String v = p.getProperty(m.name + "." + s.name);
                    if (v == null) continue;
                    if (s instanceof BoolSetting) {
                        ((BoolSetting)s).value = Boolean.parseBoolean(v);
                    }
                    if (s instanceof NumSetting) {
                        ((NumSetting)s).set(Double.parseDouble(v));
                    }
                    if (!(s instanceof BindSetting)) continue;
                    ((BindSetting)s).key = Integer.parseInt(v);
                }
            }
            catch (Exception exception) {
            }
        }
    }
}

