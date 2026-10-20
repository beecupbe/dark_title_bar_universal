package net.minecraft.launchwrapper;
import java.net.URL;
import java.net.URLClassLoader;

public class LaunchClassLoader extends URLClassLoader {
    public LaunchClassLoader(URL[] sources) {
        super(sources, null);
    }
}
