package dev.beecube31.darktitleuni.legacy;

import dev.beecube31.darktitleuni.Bootstrap;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.List;

public final class DarkTitleBarTweaker implements ITweaker {
    public DarkTitleBarTweaker() { Bootstrap.start("launchwrapper"); }

    @Override public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) { }

    @Override public void injectIntoClassLoader(LaunchClassLoader classLoader) { }

    @Override public String getLaunchTarget() { return "net.minecraft.client.main.Main"; }

    @Override public String[] getLaunchArguments() { return new String[0]; }
}
