package com.github.andrew0030.dakimakuramod.util.obj;

import com.github.andrew0030.dakimakuramod.DakimakuraMod;
import com.github.andrew0030.dakimakuramod.DakimakuraModClient;
import com.github.andrew0030.dakimakuramod.dakimakura.Daki;
import com.github.andrew0030.dakimakuramod.dakimakura.client.DakiTexture;
import com.github.andrew0030.dakimakuramod.util.DMRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DakimakuraModel
{
    public static final DakimakuraModel INSTANCE = new DakimakuraModel();

    // Base Texture
    private static final ResourceLocation TEXTURE_BLANK = new ResourceLocation(DakimakuraMod.MODID, "textures/obj/blank.png");
    // Model Paths
    private static final String MODEL_PATH = "models/obj/dakimakura.obj";
    private static final String MODEL_PATH_LOD = "models/obj/dakimakura-lod-%d.obj";
    // Models
    private final ObjVbo DAKIMAKURA_MODEL;
    private final ObjVbo[] DAKIMAKURA_MODEL_LODS;

    public DakimakuraModel()
    {
        DAKIMAKURA_MODEL = new ObjVbo(ObjModel.loadModel(new ResourceLocation(DakimakuraMod.MODID, MODEL_PATH)));
        DAKIMAKURA_MODEL_LODS = new ObjVbo[4];
        for (int i = 0; i < DAKIMAKURA_MODEL_LODS.length; i++)
            DAKIMAKURA_MODEL_LODS[i] = new ObjVbo(ObjModel.loadModel(new ResourceLocation(DakimakuraMod.MODID, String.format(MODEL_PATH_LOD, i + 1))));
    }

    public void render(PoseStack stack, int packedLight, Daki daki, BlockPos pos)
    {
        double distance = 0;
        if (Minecraft.getInstance().player != null)
            distance = Minecraft.getInstance().player.distanceToSqr(pos.getCenter());
        int lod = Mth.floor(distance / 200D);
        this.render(stack, packedLight, daki, lod);
    }

    public void render(PoseStack stack, int packedLight, Daki daki, double x, double y, double z)
    {
        double distance = 0;
        if (Minecraft.getInstance().player != null)
            distance = Minecraft.getInstance().player.distanceToSqr(x, y, z);
        int lod = Mth.floor(distance / 200D);
        this.render(stack, packedLight, daki, lod);
    }

    public void render(PoseStack stack, int packedLight, Daki daki, int lod)
    {
        lod = Mth.clamp(lod, 0, DAKIMAKURA_MODEL_LODS.length);
        stack.pushPose();
        stack.scale(0.6F, 0.6F, 0.6F);
        stack.scale(-1F, 1F, -1F);

        RenderType renderType = DMRenderTypes.getDakimakuraType(TEXTURE_BLANK);
        if(daki != null)
        {
            DakiTexture dakiTexture = DakimakuraModClient.getDakiTextureManager().getTextureForDaki(daki);
            renderType = dakiTexture.isLoaded() ?
                    DMRenderTypes.getSpecialDakimakuraType(dakiTexture.getId()) :
                    renderType;
        }

        renderType.setupRenderState();

        ObjVbo model = lod == 0 ? DAKIMAKURA_MODEL : DAKIMAKURA_MODEL_LODS[lod - 1];
        model.render(stack, packedLight);

        renderType.clearRenderState();

        stack.popPose();
    }
}