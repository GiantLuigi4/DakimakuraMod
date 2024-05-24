package com.github.andrew0030.dakimakuramod.mixins;

import com.github.andrew0030.dakimakuramod.itf.PoseStackAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Deque;

@Mixin(PoseStack.class)
public class PoseStackMixin implements PoseStackAccessor {
    @Shadow @Final private Deque<PoseStack.Pose> poseStack;

    @Override
    public PoseStack.Pose dakimakuraMod$getSecondToLatest() {
        PoseStack.Pose p = poseStack.removeLast();
        PoseStack.Pose p1 = poseStack.getLast();
        poseStack.addLast(p);
        return p1;
    }
}
