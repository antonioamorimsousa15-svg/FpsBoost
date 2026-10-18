package com.fpsboost;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.List;

public class FpsGui extends GuiScreen {
    private Settings.Cat tab = Settings.Cat.RENDER;
    private List<Settings.Opt> current;

    @Override
    public void initGui() {
        buttonList.clear();
        Settings.Cat[] cats = Settings.Cat.values();
        int tw = Math.min(110, (width - 10) / cats.length);
        int tx = width / 2 - (tw * cats.length) / 2;
        for (int i = 0; i < cats.length; i++) {
            GuiButton b = new GuiButton(100 + i, tx + i * tw, 6, tw, 20, (cats[i] == tab ? "> " : "") + cats[i].label);
            buttonList.add(b);
        }
        current = Settings.of(tab);
        int bw = Math.min(240, width - 20);
        for (int i = 0; i < current.size(); i++) {
            buttonList.add(new GuiButton(i, width / 2 - bw / 2, 32 + i * 22, bw, 20, current.get(i).text()));
        }
        int doneY = Math.max(height - 26, 32 + 7 * 22 + 14);
        buttonList.add(new GuiButton(200, width / 2 - 60, doneY, 120, 20, "Concluído"));
    }

    private void change(int id, int dir) {
        Settings.Opt o = current.get(id);
        if (o == Settings.profile) {
            Settings.applyProfile(((o.index + dir) % 3 + 3) % 3);
        } else {
            o.step(dir);
        }
        FpsBoost.applyAll();
        Settings.save();
        initGui();
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id >= 100 && b.id < 100 + Settings.Cat.values().length) {
            tab = Settings.Cat.values()[b.id - 100];
            initGui();
        } else if (b.id == 200) {
            mc.displayGuiScreen(null);
        } else if (b.id >= 0 && b.id < current.size()) {
            change(b.id, 1);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn == 1) { // botão direito = volta a opção
            for (Object o : buttonList) {
                GuiButton b = (GuiButton) o;
                if (b.id >= 0 && b.id < current.size() && b.mousePressed(mc, mx, my)) {
                    b.playPressSound(mc.getSoundHandler());
                    change(b.id, -1);
                    return;
                }
            }
        }
        super.mouseClicked(mx, my, btn);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        super.drawScreen(mx, my, pt);
        for (Object o : buttonList) {
            GuiButton b = (GuiButton) o;
            if (b.id >= 0 && b.id < current.size() && mx >= b.xPosition && mx < b.xPosition + b.width
                    && my >= b.yPosition && my < b.yPosition + b.height) {
                drawCenteredString(fontRendererObj, current.get(b.id).desc, width / 2,
                        Math.max(height - 26, 32 + 7 * 22 + 14) - 12, 0xAAAAAA);
            }
        }
    }

    @Override
    public void onGuiClosed() { Settings.save(); }
}
