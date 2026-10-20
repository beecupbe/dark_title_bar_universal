package dev.beecube31.darktitleuni.core;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DarkTitleBar {
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19; // Win10 1809–1909
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;     // Win10 20H1+, Win11
    private static final int E_INVALIDARG = 0x80070057;

    private static final int GWL_STYLE = -16;
    private static final int WS_CAPTION = 0x00C00000;
    private static final int GW_OWNER = 4;
    private static final int WM_NCACTIVATE = 0x0086;

    // SWP_NOSIZE | SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED | SWP_ASYNCWINDOWPOS
    private static final int SWP_REFRESH_FRAME = 0x0001 | 0x0002 | 0x0004 | 0x0010 | 0x0020 | 0x4000;

    private static final int CALL_CONV = detectCallingConvention();

    private static boolean started;

    private static WndEnumProc enumProc;
    private static final List<Long> found = new ArrayList<Long>();
    private static int ownPid;

    public interface WndEnumProc extends StdCallLibrary.StdCallCallback {
        boolean callback(Pointer hwnd, Pointer lParam);
    }

    private DarkTitleBar() {}

    /** Called from any entry point of any loader. Subsequent calls are ignored. */
    public static synchronized void start(final String loader) {
        if (started) return;
        started = true;
        if (!System.getProperty("os.name", "").startsWith("Windows")) return;

        Thread t = new Thread(() -> loop(loader), "DarkTitleBar");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private static void loop(String loader) {
        log("started via " + loader);
        Set<Long> applied = new HashSet<Long>();
        long startedAt = System.currentTimeMillis();
        try {
            ownPid = fn("kernel32", "GetCurrentProcessId").invokeInt(new Object[0]);
            enumProc = new WndEnumProc() {
                @Override public boolean callback(Pointer hwnd, Pointer lParam) {
                    if (isGameWindow(hwnd)) found.add(Pointer.nativeValue(hwnd));
                    return true;
                }
            };

            while (true) {
                // Ignore windows that have closed (LWJGL 2 recreates the window when F11 is pressed)
                applied.removeIf(aLong -> user32("IsWindow", ptr(aLong)) == 0);

                found.clear();
                fn("user32", "EnumWindows").invokeInt(new Object[]{enumProc, null});

                for (Long hwnd : found) {
                    if (applied.contains(hwnd)) continue;
                    int hr = apply(hwnd);
                    if (hr >= 0) {
                        applied.add(hwnd);
                        log("dark title bar applied to 0x" + Long.toHexString(hwnd));
                    } else if (hr == E_INVALIDARG) {
                        log("Windows does not support immersive dark mode (needs Win10 1809+), stopping");
                        return;
                    }
                }

                boolean early = System.currentTimeMillis() - startedAt < 5 * 60_000L;
                Thread.sleep(applied.isEmpty() ? (early ? 200 : 2000) : 750);
            }
        } catch (InterruptedException ignored) {
        } catch (Throwable t) {
            log("disabled: " + t);
        }
    }

    private static boolean isGameWindow(Pointer hwnd) {
        int[] pid = new int[1];
        fn("user32", "GetWindowThreadProcessId").invokeInt(new Object[]{hwnd, pid});
        if (pid[0] != ownPid) return false;
        if (user32("IsWindowVisible", hwnd) == 0) return false;
        int style = user32("GetWindowLongW", hwnd, GWL_STYLE);
        if ((style & WS_CAPTION) != WS_CAPTION) return false;

        // Only top-level windows without an owner (dialogs and tooltips are excluded)
        Pointer owner = (Pointer) fn("user32", "GetWindow").invoke(Pointer.class, new Object[]{hwnd, GW_OWNER});
        return owner == null;
    }

    /** @return HRESULT DwmSetWindowAttribute */
    private static int apply(long hwndValue) {
        Pointer hwnd = ptr(hwndValue);
        int[] on = {1};
        Function dwm = fn("dwmapi", "DwmSetWindowAttribute");
        int hr = dwm.invokeInt(new Object[]{hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, on, 4});
        if (hr < 0) hr = dwm.invokeInt(new Object[]{hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, on, 4});
        if (hr < 0) return hr;

        fn("user32", "SetWindowPos").invokeInt(new Object[]{hwnd, null, 0, 0, 0, 0, SWP_REFRESH_FRAME});
        Pointer fg = (Pointer) fn("user32", "GetForegroundWindow").invoke(Pointer.class, new Object[0]);
        if (fg != null && Pointer.nativeValue(fg) == hwndValue) {
            fn("user32", "PostMessageW").invokeInt(new Object[]{hwnd, WM_NCACTIVATE, null, null});
            fn("user32", "PostMessageW").invokeInt(new Object[]{hwnd, WM_NCACTIVATE, new Pointer(1), null});
        }
        return hr;
    }

    private static int user32(String name, Object... args) {
        return fn("user32", name).invokeInt(args);
    }

    static Function fn(String lib, String name) {
        return Function.getFunction(lib, name, CALL_CONV);
    }

    static int detectCallingConvention() {
        String model = System.getProperty("sun.arch.data.model", "");
        String arch = System.getProperty("os.arch", "");
        if ("64".equals(model) || arch.contains("64")) return 0;
        try {
            return StdCallLibrary.class.getField("STDCALL_CONVENTION").getInt(null);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static Pointer ptr(long v) {
        return new Pointer(v);
    }

    private static void log(String msg) {
        System.out.println("[DarkTitleBar] " + msg);
    }
}
