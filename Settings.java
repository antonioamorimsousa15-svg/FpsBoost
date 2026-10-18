package com.fpsboost;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Settings {
    public enum Cat {
        RENDER("Renderização"), GRAPHICS("Gráficos"), OPT("Otimizações"), MONITOR("Monitoramento");
        public final String label;
        Cat(String l) { label = l; }
    }

    public static final List<Opt> ALL = new ArrayList<Opt>();

    public static class Opt {
        public final Cat cat;
        public final String key, name, desc;
        public final int[] values;
        public final String[] labels;
        public final int def;
        public int index;

        Opt(Cat cat, String key, String name, String desc, int[] values, String[] labels, int def) {
            this.cat = cat; this.key = key; this.name = name; this.desc = desc;
            this.values = values; this.labels = labels; this.def = def; this.index = def;
            ALL.add(this);
        }
        public int value() { return values[index]; }
        public boolean on() { return values[index] == 1; }
        public void step(int d) { index = ((index + d) % values.length + values.length) % values.length; }
        public String text() { return name + ": " + labels[index]; }
    }

    private static Opt bool(Cat c, String key, String name, String desc, boolean def) {
        return new Opt(c, key, name, desc, new int[]{0, 1}, new String[]{"OFF", "ON"}, def ? 1 : 0);
    }

    // ---- Renderização ----
    public static final Opt renderDistance = new Opt(Cat.RENDER, "renderDistance", "Render Distance", "Distância de chunks (2-16)",
            new int[]{2, 4, 6, 8, 10, 12, 14, 16},
            new String[]{"2 chunks", "4 chunks", "6 chunks", "8 chunks", "10 chunks", "12 chunks", "14 chunks", "16 chunks"}, 3);
    public static final Opt entityDistance = new Opt(Cat.RENDER, "entityDistance", "Entity Distance", "Distância de renderização das entidades",
            new int[]{25, 50, 75, 100}, new String[]{"25%", "50%", "75%", "100%"}, 2);
    public static final Opt entityCulling = bool(Cat.RENDER, "entityCulling", "Entity Culling", "Não desenha entidades escondidas atrás de blocos", true);
    public static final Opt tileCulling = bool(Cat.RENDER, "tileCulling", "Tile Entity Culling", "Não desenha baús/placas etc. distantes ou escondidos", true);
    public static final Opt chunkControl = bool(Cat.RENDER, "chunkControl", "Chunk Update Control", "Muda a render distance aos poucos (evita travada)", true);
    public static final Opt vbo = bool(Cat.RENDER, "vbo", "VBO Optimization", "Usa VBOs do jogo (pode bugar em alguns celulares)", false);

    // ---- Gráficos ----
    public static final Opt fastGraphics = bool(Cat.GRAPHICS, "fastGraphics", "Fast Graphics", "ON = gráficos rápidos (menos efeitos)", true);
    public static final Opt smoothLighting = new Opt(Cat.GRAPHICS, "smoothLighting", "Smooth Lighting", "Iluminação suave",
            new int[]{0, 1, 2}, new String[]{"OFF", "Minimum", "ON"}, 0);
    public static final Opt clouds = new Opt(Cat.GRAPHICS, "clouds", "Clouds", "Nuvens",
            new int[]{0, 1, 2}, new String[]{"OFF", "Fast", "Fancy"}, 0);
    public static final Opt weather = bool(Cat.GRAPHICS, "weather", "Weather", "ON = mostra chuva/neve", false);
    public static final Opt shadows = bool(Cat.GRAPHICS, "entityShadows", "Entity Shadows", "Sombras das entidades", false);
    public static final Opt particles = new Opt(Cat.GRAPHICS, "particles", "Particles", "Quantidade de partículas",
            new int[]{0, 1, 2}, new String[]{"All", "Decreased", "Minimal"}, 2);
    public static final Opt animations = bool(Cat.GRAPHICS, "animations", "Animation Toggle", "Liga/desliga texturas animadas (água, lava, fogo)", true);

    // ---- Otimizações ----
    public static final Opt smartAnim = bool(Cat.OPT, "smartAnim", "Smart Animations", "Atualiza as animações com menos frequência", true);
    public static final Opt fastMath = bool(Cat.OPT, "fastMath", "Fast Math", "Matemática rápida (só com OptiFine)", true);
    public static final Opt hudCache = bool(Cat.OPT, "hudCache", "HUD Caching", "Reaproveita o texto do HUD do mod (atualiza 4x/s)", true);
    public static final Opt stabilizer = bool(Cat.OPT, "stabilizer", "FPS Stabilizer", "Alivia o jogo por 3s quando detecta travada", true);
    public static final Opt memOpt = bool(Cat.OPT, "memOpt", "Memory Optimization", "Limpa a memória quando passa de 85%", true);
    public static final Opt dynamicRD = bool(Cat.OPT, "dynamicRD", "Dynamic Render Distance", "Ajusta a distância conforme o FPS", false);

    // ---- Monitoramento ----
    public static final Opt fpsCounter = bool(Cat.MONITOR, "fpsCounter", "FPS Counter", "FPS atual e mínimo", true);
    public static final Opt frameTime = bool(Cat.MONITOR, "frameTime", "Frame Time", "Tempo de cada quadro em ms", false);
    public static final Opt memUsage = bool(Cat.MONITOR, "memUsage", "Memory Usage", "Memória usada e disponível", false);
    public static final Opt entityCounter = bool(Cat.MONITOR, "entityCounter", "Entity Counter", "Entidades renderizadas", false);
    public static final Opt chunkCounter = bool(Cat.MONITOR, "chunkCounter", "Chunk Counter", "Chunks carregados/renderizados", false);
    public static final Opt profile = new Opt(Cat.MONITOR, "profile", "Performance Profile", "Clique para aplicar um perfil",
            new int[]{0, 1, 2}, new String[]{"Low", "Balanced", "High"}, 1);

    public static int targetFps = 60;
    private static Configuration cfg;

    public static Opt get(String key) {
        for (Opt o : ALL) if (o.key.equals(key)) return o;
        return null;
    }

    public static List<Opt> of(Cat c) {
        List<Opt> l = new ArrayList<Opt>();
        for (Opt o : ALL) if (o.cat == c) l.add(o);
        return l;
    }

    private static final String[] PK = {"renderDistance", "entityDistance", "entityCulling", "tileCulling", "chunkControl", "vbo",
            "fastGraphics", "smoothLighting", "clouds", "weather", "entityShadows", "particles", "animations",
            "smartAnim", "fastMath", "hudCache", "stabilizer", "memOpt", "dynamicRD"};
    private static final int[][] PV = {
            {1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 2, 0, 1, 1, 1, 1, 1, 1}, // Low
            {3, 2, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0}, // Balanced
            {5, 3, 1, 1, 1, 0, 0, 2, 2, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0}  // High
    };

    public static void applyProfile(int p) {
        for (int i = 0; i < PK.length; i++) get(PK[i]).index = PV[p][i];
        profile.index = p;
    }

    public static void load(File f) {
        cfg = new Configuration(f);
        for (Opt o : ALL) o.index = cfg.getInt(o.key, "options", o.def, 0, o.values.length - 1, o.name);
        targetFps = cfg.getInt("targetFps", "general", 60, 20, 240, "FPS alvo usado pelo Dynamic Render Distance");
        if (cfg.hasChanged()) cfg.save();
    }

    public static void save() {
        if (cfg == null) return;
        for (Opt o : ALL) cfg.get("options", o.key, o.def).set(o.index);
        cfg.save();
    }
}
