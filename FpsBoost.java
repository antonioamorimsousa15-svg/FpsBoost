package com.fpsboost;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Mod(modid = FpsBoost.MODID, name = "FpsBoost", version = "1.0.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class FpsBoost {
    public static final String MODID = "fpsboost";
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static KeyBinding key;
    private static boolean openGui, worldReady;
    private static int tick;

    // animações
    private static Field animField;
    private static List origAnim;
    private static final List EMPTY = new ArrayList();

    // estatísticas de frame
    private static long lastFrame, windowStart, lastBuild;
    private static int frames, fpsAvg, fpsMin;
    private static long maxDt;
    private static double frameMs, worstMs;
    private static boolean windowFresh;
    private static final List<String> lines = new ArrayList<String>();

    // automáticos
    private static int boost, lastRdChange, lastGc;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Settings.load(e.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        key = new KeyBinding("Abrir FpsBoost", Keyboard.KEY_K, "FpsBoost");
        ClientRegistry.registerKeyBinding(key);
        ClientCommandHandler.instance.registerCommand(new Cmd());
        MinecraftForge.EVENT_BUS.register(this);
        try {
            for (Field f : TextureMap.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType()) && !Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    animField = f;
                    break;
                }
            }
        } catch (Throwable t) { animField = null; }
    }

    public static class Cmd extends CommandBase {
        public String getCommandName() { return "fpsboost"; }
        public List<String> getCommandAliases() { return Collections.singletonList("fps"); }
        public String getCommandUsage(ICommandSender s) { return "/fpsboost"; }
        public int getRequiredPermissionLevel() { return 0; }
        public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }
        public void processCommand(ICommandSender s, String[] a) throws CommandException { openGui = true; }
    }

    // ---------------------------------------------------------------- aplicar opções
    public static void applyAll() {
        GameSettings gs = mc.gameSettings;
        boolean reload = false;

        int rd = Settings.renderDistance.value();
        if (Settings.dynamicRD.on()) {
            if (gs.renderDistanceChunks > rd) { gs.renderDistanceChunks = rd; reload = true; }
        } else if (gs.renderDistanceChunks != rd) { gs.renderDistanceChunks = rd; reload = true; }

        boolean fancy = !Settings.fastGraphics.on();
        if (gs.fancyGraphics != fancy) { gs.fancyGraphics = fancy; reload = true; }

        int ao = Settings.smoothLighting.value();
        if (gs.ambientOcclusion != ao) { gs.ambientOcclusion = ao; reload = true; }

        boolean vbo = Settings.vbo.on();
        if (gs.useVbo != vbo) { gs.useVbo = vbo; reload = true; }

        gs.clouds = Settings.clouds.value();
        gs.entityShadows = Settings.shadows.on();
        gs.particleSetting = Settings.particles.value();
        Entity.setRenderDistanceWeight(Settings.entityDistance.value() / 100.0);

        try { // só existe com OptiFine
            Field f = GameSettings.class.getDeclaredField("ofFastMath");
            f.setAccessible(true);
            f.setBoolean(gs, Settings.fastMath.on());
        } catch (Throwable ignored) { }

        if (reload && mc.renderGlobal != null && mc.theWorld != null) mc.renderGlobal.loadRenderers();
    }

    // ---------------------------------------------------------------- ticks
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) { animationStep(true); return; }
        animationStep(false);

        if (openGui || (key.isPressed() && mc.currentScreen == null)) {
            openGui = false;
            mc.displayGuiScreen(new FpsGui());
        }
        if (mc.theWorld == null || mc.thePlayer == null) { worldReady = false; return; }
        if (!worldReady) {
            worldReady = true;
            applyAll();
            TileCuller.install();
        }
        tick++;

        if (!Settings.weather.on()) {
            mc.theWorld.setRainStrength(0f);
            mc.theWorld.setThunderStrength(0f);
        }

        if (Settings.entityCulling.on()) EntityCuller.update(mc.theWorld, mc.thePlayer, tick);
        else EntityCuller.clear();
        if (tick % 40 == 0) TileCuller.clearCache();

        // FPS Stabilizer
        if (windowFresh) {
            windowFresh = false;
            if (Settings.stabilizer.on() && boost == 0 && fpsAvg > 0 && worstMs >= 60 && worstMs > 3.0 * 1000.0 / fpsAvg) {
                boost = 60;
                mc.gameSettings.particleSetting = 2;
                Entity.setRenderDistanceWeight(Math.min(Settings.entityDistance.value() / 100.0, 0.5));
            }
        }
        if (boost > 0 && --boost == 0) applyAll();

        // Dynamic Render Distance (+ Chunk Update Control)
        if (Settings.dynamicRD.on() && tick % 100 == 0 && fpsAvg > 0 && mc.currentScreen == null) {
            int gap = Settings.chunkControl.on() ? 200 : 60;
            if (tick - lastRdChange >= gap) {
                int cur = mc.gameSettings.renderDistanceChunks;
                int cap = Settings.renderDistance.value();
                if (fpsAvg < Settings.targetFps - 5 && cur > 2) {
                    mc.gameSettings.renderDistanceChunks = cur - 1; lastRdChange = tick; mc.renderGlobal.loadRenderers();
                } else if (fpsAvg > Settings.targetFps + 25 && cur < cap) {
                    mc.gameSettings.renderDistanceChunks = cur + 1; lastRdChange = tick; mc.renderGlobal.loadRenderers();
                }
            }
        }

        // Memory Optimization
        if (Settings.memOpt.on() && tick % 600 == 0) {
            Runtime r = Runtime.getRuntime();
            long used = r.totalMemory() - r.freeMemory();
            if (used > 0.85 * r.maxMemory() && tick - lastGc > 1200) { System.gc(); lastGc = tick; }
        }
    }

    // Animation Toggle + Smart Animations
    private void animationStep(boolean start) {
        if (animField == null) return;
        try {
            TextureMap tm = mc.getTextureMapBlocks();
            if (tm == null) return;
            if (start) {
                Object cur = animField.get(tm);
                if (cur != EMPTY) origAnim = (List) cur;
                int interval = Settings.profile.value() == 0 ? 3 : 2;
                boolean animate = Settings.animations.on() && (!Settings.smartAnim.on() || tick % interval == 0);
                if (!animate) animField.set(tm, EMPTY);
            } else if (origAnim != null && animField.get(tm) == EMPTY) {
                animField.set(tm, origAnim);
            }
        } catch (Throwable t) { animField = null; }
    }

    // ---------------------------------------------------------------- frames + HUD
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        long now = System.nanoTime();
        if (lastFrame != 0) {
            long dt = now - lastFrame;
            frameMs = dt / 1.0e6;
            if (dt > maxDt) maxDt = dt;
            frames++;
        }
        lastFrame = now;
        if (now - windowStart >= 1000000000L) {
            if (windowStart != 0 && frames > 0) {
                fpsAvg = (int) Math.round(frames * 1.0e9 / (now - windowStart));
                worstMs = maxDt / 1.0e6;
                fpsMin = (int) (1.0e9 / Math.max(maxDt, 1L));
                windowFresh = true;
            }
            windowStart = now; frames = 0; maxDt = 0;
        }
        boolean any = Settings.fpsCounter.on() || Settings.frameTime.on() || Settings.memUsage.on()
                || Settings.entityCounter.on() || Settings.chunkCounter.on();
        if (!any) { lines.clear(); return; }
        if (!Settings.hudCache.on() || now - lastBuild > 250000000L) { buildLines(); lastBuild = now; }
    }

    private void buildLines() {
        lines.clear();
        if (Settings.fpsCounter.on()) lines.add("FPS: " + Minecraft.getDebugFPS() + " (min " + fpsMin + ")");
        if (Settings.frameTime.on()) lines.add(String.format(Locale.US, "Frame: %.1f ms (pior %.1f)", frameMs, worstMs));
        if (Settings.memUsage.on()) {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) >> 20, max = r.maxMemory() >> 20;
            lines.add("Mem: " + used + "/" + max + " MB (livre " + (max - used) + ")");
        }
        if (mc.renderGlobal != null) {
            if (Settings.entityCounter.on()) lines.add("Entidades: " + mc.renderGlobal.getDebugInfoEntities());
            if (Settings.chunkCounter.on()) lines.add("Chunks: " + mc.renderGlobal.getDebugInfoRenders());
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL || lines.isEmpty() || mc.gameSettings.showDebugInfo) return;
        int y = 4;
        for (String s : lines) { mc.fontRendererObj.drawStringWithShadow(s, 4, y, 0xFFFFFF); y += 10; }
    }

    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public void onRenderLiving(RenderLivingEvent.Pre e) {
        if (Settings.entityCulling.on() && EntityCuller.isHidden(e.entity)) e.setCanceled(true);
    }
}
