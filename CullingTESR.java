package com.fpsboost;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

/** Envolve um renderer de tile entity e pula o desenho quando o TileCuller manda. */
public class CullingTESR extends TileEntitySpecialRenderer<TileEntity> {
    private final TileEntitySpecialRenderer<TileEntity> delegate;

    public CullingTESR(TileEntitySpecialRenderer<TileEntity> delegate) { this.delegate = delegate; }

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage) {
        if (Settings.tileCulling.on() && TileCuller.shouldSkip(te)) return;
        delegate.renderTileEntityAt(te, x, y, z, partialTicks, destroyStage);
    }
}
