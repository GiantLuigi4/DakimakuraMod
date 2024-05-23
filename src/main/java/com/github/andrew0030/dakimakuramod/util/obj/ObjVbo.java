package com.github.andrew0030.dakimakuramod.util.obj;

import com.github.andrew0030.dakimakuramod.util.buffer.SmartBufferBuilder;
import com.github.andrew0030.dakimakuramod.util.buffer.SuperVertexBuffer;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCString;

import java.io.Closeable;
import java.nio.ByteBuffer;

public class ObjVbo implements Closeable {
    private final ObjModel model;
    private final SuperVertexBuffer buffer;
    private ByteBuffer lB;
    private long addr;
    private int capacityQuarter;
    private int capacityHalf;
    private int initialFillIters;

    public ObjVbo(
            ObjModel model
    ) {
        this.model = model;
        this.buffer = new SuperVertexBuffer(
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.TRIANGLES,
                VertexBuffer.Usage.DYNAMIC
        );
        reload();
    }

    protected void createLightBuf() {
        lB = MemoryUtil.memAlloc(buffer.getOffset(1) - buffer.getOffset(0));
        addr = MemoryUtil.memAddress(lB);

        initialFillIters = lB.capacity() >> 5;
        capacityQuarter = lB.capacity() >> 2;
        capacityHalf = capacityQuarter << 1;
    }

    int prevLight = 0;

    public void render(PoseStack stack, int packedLight) {
        if (prevLight != packedLight) {
            prevLight = packedLight;

            long asLong = packedLight | ((long) packedLight << 32L);

            for (int i = 0; i < initialFillIters; i++)
                lB.putLong(i << 3, asLong);
            LibCString.nmemcpy(addr + capacityQuarter, addr, capacityQuarter);
            LibCString.nmemcpy(addr + capacityHalf, addr, capacityHalf);
            buffer.updateElement(lB.position(0).limit(lB.capacity()), 0);
        }

        Lighting.setupLevel(RenderSystem.getModelViewMatrix());
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().last().pose().set(stack.last().pose());
        RenderSystem.getModelViewStack().last().normal().set(stack.last().normal());
        RenderSystem.applyModelViewMatrix();

        buffer.bind();
        buffer.drawWithShader();
        buffer.unbind();

        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupLevel(RenderSystem.getModelViewMatrix());
    }

    public void reload() {
        SmartBufferBuilder smartBufferBuilder = new SmartBufferBuilder(DefaultVertexFormat.NEW_ENTITY);
        model.render(new PoseStack(), smartBufferBuilder, 0);
        buffer.bind();
        buffer.upload(smartBufferBuilder,
                // required elements
                DefaultVertexFormat.ELEMENT_UV2,
                DefaultVertexFormat.ELEMENT_UV1,
                DefaultVertexFormat.ELEMENT_COLOR,
                // used elements
                DefaultVertexFormat.ELEMENT_POSITION,
                DefaultVertexFormat.ELEMENT_UV,
                DefaultVertexFormat.ELEMENT_NORMAL
        );
        buffer.unbind();
        smartBufferBuilder.close();

        createLightBuf();
    }

    public void close() {
        buffer.close();
        MemoryUtil.memFree(lB);
    }
}
