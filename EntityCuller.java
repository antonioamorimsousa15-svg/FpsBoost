package com.fpsboost;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Entity Culling: esconde entidades que estão completamente atrás de blocos opacos. */
public class EntityCuller {
    private static final Map<Integer, Boolean> hidden = new HashMap<Integer, Boolean>();
    private static int cursor = 0;

    public static boolean isHidden(Entity e) {
        Boolean b = hidden.get(e.getEntityId());
        return b != null && b;
    }

    public static void clear() { hidden.clear(); }

    public static void update(WorldClient w, EntityPlayerSP p, int tick) {
        List<Entity> list = w.loadedEntityList;
        int n = list.size();
        if (n == 0) return;
        Vec3 eye = new Vec3(p.posX, p.posY + p.getEyeHeight(), p.posZ);
        int budget = Math.min(n, 48);
        for (int i = 0; i < budget; i++) {
            cursor = (cursor + 1) % n;
            Entity e = list.get(cursor);
            if (!(e instanceof EntityLivingBase) || e == p) continue;
            if (p.getDistanceSqToEntity(e) < 36.0) { hidden.put(e.getEntityId(), Boolean.FALSE); continue; }
            hidden.put(e.getEntityId(), !visible(w, eye, e));
        }
        if (tick % 200 == 0) {
            Iterator<Integer> it = hidden.keySet().iterator();
            while (it.hasNext()) if (w.getEntityByID(it.next()) == null) it.remove();
        }
    }

    private static boolean visible(WorldClient w, Vec3 eye, Entity e) {
        double hw = e.width / 2.0;
        double[][] pts = {
                {0, 0.1, 0}, {0, e.height * 0.5, 0}, {0, e.height * 0.95, 0},
                {hw, e.height * 0.5, 0}, {-hw, e.height * 0.5, 0}, {0, e.height * 0.5, hw}, {0, e.height * 0.5, -hw}
        };
        for (double[] o : pts) {
            Vec3 to = new Vec3(e.posX + o[0], e.posY + o[1], e.posZ + o[2]);
            MovingObjectPosition r = w.rayTraceBlocks(eye, to, false, true, false);
            if (r == null) return true;
            // bateu em algo que não é cubo opaco (vidro, folhas...) -> considera visível
            if (r.getBlockPos() != null && !w.getBlockState(r.getBlockPos()).getBlock().isOpaqueCube()) return true;
        }
        return false;
    }
}
