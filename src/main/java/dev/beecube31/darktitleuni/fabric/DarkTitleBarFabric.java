package dev.beecube31.darktitleuni.fabric;

import dev.beecube31.darktitleuni.Bootstrap;
import net.fabricmc.api.ModInitializer;

/** Fabric / Quilt */
public final class DarkTitleBarFabric implements ModInitializer {
    @Override public void onInitialize() { Bootstrap.start("fabric"); }
}
