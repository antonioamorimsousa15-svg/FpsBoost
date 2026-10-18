package com.fpsboost;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Tile Entity Culling: distância + oclusão (baús) com cache. */
public class TileCuller {
    private static final Map<Long, Boolean> occluded = new HashMap<Long, Boolean>();
    private static boolean installed = false;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void install() {
        if (installed) return;
        installed = true;
        try {
            Map map = TileEntityRendererDispatcher.instance.mapSpecialRenderers;
            for (Object o : new ArrayList(map.entrySet())) {
                Map.Entry en = (Map.Entry) o;
                Class c = (Class) en.getKey();
                if (TileEntityBeacon.class.isAssignableFrom(c)) continue; // feixe do beacon precisa ser global
                Object r = en.getValue();
                if (r instanceof CullingTESR) continue;
                map.put(c, new CullingTESR((TileEntitySpecialRenderer) r));
            }
        } catch (Throwable t) {
            System.out.println("[FpsBoost] Nao foi possivel instalar o Tile Entity Culling: " + t);
        }
    }

    public static void clearCache() { occluded.clear(); }

    public static boolean shouldSkip(TileEntity te) {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (p == null || te.getWorld() == null) return false;
        BlockPos pos = te.getPos();
        double d2 = p.getDistanceSq(pos);
        double max = 16.0 + 32.0 * Settings.entityDistance.value() / 100.0;
        if (d2 > max * max) return true;
        if (d2 < 100.0) return false;
        if (te instanceof TileEntityChest || te instanceof TileEntityEnderChest) {
            long key = pos.toLong();
            Boolean b = occluded.get(key);
            if (b == null) {
                Vec3 eye = new Vec3(p.posX, p.posY + p.getEyeHeight(), p.posZ);
                Vec3 to = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                MovingObjectPosition r = te.getWorld().rayTraceBlocks(eye, to, false, true, false);
                boolean hide = r != null && r.getBlockPos() != null && !r.getBlockPos().equals(pos)
                        && te.getWorld().getBlockState(r.getBlockPos()).getBlock().isOpaqueCube();
                b = hide;
                occluded.put(key, b);
            }
            return b;
        }
        return false;
    }
}
