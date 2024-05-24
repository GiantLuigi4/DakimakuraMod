package com.github.andrew0030.dakimakuramod.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderStateShard;

public class DMTransparencyStateShard extends RenderStateShard.TransparencyStateShard {
    public static final DMTransparencyStateShard INSTANCE = new DMTransparencyStateShard();

    private DMTransparencyStateShard() {
        super(
                "daki_transparency_off",
                () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(
                            GlStateManager.SourceFactor.ONE,
                            GlStateManager.DestFactor.ZERO
                    );
                },
                () -> {
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                }
        );
    }
}
