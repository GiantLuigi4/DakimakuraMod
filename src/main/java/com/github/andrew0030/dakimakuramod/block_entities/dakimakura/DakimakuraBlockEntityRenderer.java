package com.github.andrew0030.dakimakuramod.block_entities.dakimakura;

import com.github.andrew0030.dakimakuramod.blocks.DakimakuraBlock;
import com.github.andrew0030.dakimakuramod.itf.PoseStackAccessor;
import com.github.andrew0030.dakimakuramod.util.obj.DakimakuraModel;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class DakimakuraBlockEntityRenderer implements BlockEntityRenderer<DakimakuraBlockEntity>
{
    private final DakimakuraModel dakimakuraModel;

    public DakimakuraBlockEntityRenderer(BlockEntityRendererProvider.Context context)
    {
        this.dakimakuraModel = DakimakuraModel.INSTANCE;
    }

    @Override
    public void render(DakimakuraBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        this.render(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay, -1);
    }

    public void render(DakimakuraBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, int lod)
    {
        // Debug Rendering, used to render the "RenderBoundingBox" of this BlockEntity
//        if(!blockEntity.getBlockState().getValue(DakimakuraBlock.TOP) && blockEntity.hasLevel())
//        {
//            poseStack.pushPose();
//            poseStack.translate(-blockEntity.getBlockPos().getX(), -blockEntity.getBlockPos().getY(), -blockEntity.getBlockPos().getZ());
//            LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.LINES), blockEntity.getRenderBoundingBox(), 1F, 1F, 1F, 1F);
//            poseStack.popPose();
//        }

        Direction facing = blockEntity.getBlockState().getValue(DakimakuraBlock.FACING);
        // If the block is the top part we don't need to render anything
        if(blockEntity.getBlockState().getValue(DakimakuraBlock.TOP))
            return;

        poseStack.pushPose();
        PoseStack.Pose p = ((PoseStackAccessor) poseStack).dakimakuraMod$getSecondToLatest();
        poseStack.last().normal().set(p.normal().invert());
        p.normal().invert();

        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch(facing)
        {
            default -> {}
            case SOUTH -> poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YN.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        }

        if(blockEntity.hasLevel())
        {
            AttachFace face = blockEntity.getBlockState().getValue(DakimakuraBlock.FACE);
            if(face != AttachFace.WALL)
                poseStack.mulPose(Axis.XN.rotationDegrees(90));
            if(face == AttachFace.CEILING)
                poseStack.mulPose(Axis.YN.rotationDegrees(180));
            if(face == AttachFace.FLOOR)
                poseStack.last().normal().rotate(Axis.YN.rotationDegrees(180));
            poseStack.translate(0.0F, 0.5F, -0.380F);
        }

        if(blockEntity.isFlipped())
            poseStack.mulPose(Axis.YN.rotationDegrees(180));

        Lighting.setupLevel(new Matrix4f(poseStack.last().normal().invert()));

        if (lod == -1)
            this.dakimakuraModel.render(poseStack, packedLight, blockEntity.getDaki(), blockEntity.getBlockPos());
        else
            this.dakimakuraModel.render(poseStack, packedLight, blockEntity.getDaki(), lod);

        poseStack.popPose();
        Lighting.setupLevel(new Matrix4f(p.normal()));
    }
}