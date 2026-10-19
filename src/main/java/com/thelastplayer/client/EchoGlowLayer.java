package com.thelastplayer.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.thelastplayer.TheLastPlayerMod;
import com.thelastplayer.entity.EchoEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Draws the glowing heart + eyes (full-bright) on top of the glass body. */
@OnlyIn(Dist.CLIENT)
public class EchoGlowLayer extends LayerRenderer<EchoEntity, EchoModel> {
    private static final ResourceLocation[] GLOW = new ResourceLocation[5];
    static {
        for (int i = 0; i < 5; i++) {
            GLOW[i] = new ResourceLocation(TheLastPlayerMod.MODID, "textures/entity/echo_" + i + "_glow.png");
        }
    }

    public EchoGlowLayer(IEntityRenderer<EchoEntity, EchoModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(MatrixStack ms, IRenderTypeBuffer buffers, int light, EchoEntity echo,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        int mood = Math.max(0, Math.min(4, echo.getMood()));
        IVertexBuilder vb = buffers.getBuffer(RenderType.eyes(GLOW[mood]));
        this.getParentModel().renderToBuffer(ms, vb, 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
