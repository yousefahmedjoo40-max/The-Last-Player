package com.thelastplayer.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.thelastplayer.entity.EchoEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** A small floating cube of blue glass with two little fins. */
@OnlyIn(Dist.CLIENT)
public class EchoModel extends EntityModel<EchoEntity> {
    private final ModelRenderer body;
    private final ModelRenderer finL;
    private final ModelRenderer finR;

    public EchoModel() {
        this.texWidth = 64;
        this.texHeight = 32;

        body = new ModelRenderer(this, 0, 0);
        body.setPos(0.0F, 17.0F, 0.0F);
        body.addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, 0.0F);

        finL = new ModelRenderer(this, 32, 0);
        finL.setPos(4.5F, 0.0F, 0.0F);
        finL.addBox(0.0F, -2.0F, -1.5F, 1.0F, 4.0F, 3.0F, 0.0F);
        body.addChild(finL);

        finR = new ModelRenderer(this, 32, 0);
        finR.mirror = true;
        finR.setPos(-4.5F, 0.0F, 0.0F);
        finR.addBox(-1.0F, -2.0F, -1.5F, 1.0F, 4.0F, 3.0F, 0.0F);
        body.addChild(finR);
    }

    @Override
    public void setupAnim(EchoEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        body.y = 17.0F + MathHelper.sin(ageInTicks * 0.12F) * 1.2F;
        body.xRot = headPitch * ((float) Math.PI / 180.0F);
        float flap = MathHelper.sin(ageInTicks * 0.5F) * 0.5F;
        finL.zRot = 0.3F + flap;
        finR.zRot = -0.3F - flap;
        // trembling when scared / breaking
        body.x = entity.getMood() >= 2 ? MathHelper.sin(ageInTicks * 2.3F) * 0.5F : 0.0F;
    }

    @Override
    public void renderToBuffer(MatrixStack ms, IVertexBuilder buf, int light, int overlay,
                               float r, float g, float b, float a) {
        body.render(ms, buf, light, overlay, r, g, b, a);
    }
}
