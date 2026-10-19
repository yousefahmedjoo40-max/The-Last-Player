package com.thelastplayer.client;

import com.thelastplayer.TheLastPlayerMod;
import com.thelastplayer.entity.EchoEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EchoRenderer extends MobRenderer<EchoEntity, EchoModel> {
    private static final ResourceLocation[] TEX = new ResourceLocation[5];
    static {
        for (int i = 0; i < 5; i++) {
            TEX[i] = new ResourceLocation(TheLastPlayerMod.MODID, "textures/entity/echo_" + i + ".png");
        }
    }

    public EchoRenderer(EntityRendererManager manager) {
        super(manager, new EchoModel(), 0.25F);
        this.addLayer(new EchoGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(EchoEntity entity) {
        return TEX[Math.max(0, Math.min(4, entity.getMood()))];
    }
}
