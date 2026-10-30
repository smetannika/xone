package dev.client;

import dev.client.Config;
import dev.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class ClientMod
implements ClientModInitializer {
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Config.save());
    }
}

