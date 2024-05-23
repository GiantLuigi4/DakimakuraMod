package com.github.andrew0030.dakimakuramod.util.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class SuperVertexBuffer implements Closeable {
    private int vertexBufferId;
    private int indexBufferId;
    private int arrayObjectId;

    private final VertexFormat format;
    private final VertexBuffer.Usage usage;
    private VertexFormat.Mode mode;

    public SuperVertexBuffer(
            VertexFormat pFormat,
            VertexFormat.Mode mode,
            VertexBuffer.Usage pUsage
    ) {
        this.format = pFormat;
        this.mode = mode;
        this.usage = pUsage;

        RenderSystem.assertOnRenderThread();
        this.vertexBufferId = GlStateManager._glGenBuffers();
        this.indexBufferId = GlStateManager._glGenBuffers();
        this.arrayObjectId = GlStateManager._glGenVertexArrays();
    }

    public SuperVertexBuffer setMode(VertexFormat.Mode mode) {
        this.mode = mode;
        return this;
    }

    private int indexCount;

    HashMap<VertexFormatElement, Integer> offsets = new HashMap<>();
    int[] starts;

    public void upload(
            SmartBufferBuilder buffer,
            VertexFormatElement... order
    ) {
        RenderSystem.assertOnRenderThread();

        offsets.clear();

        int total = 0;
        ByteBuffer[] bufs = new ByteBuffer[order.length];
        for (int i = 0; i < order.length; i++) {
            ByteBuffer buf = buffer.getBuffer(order[i]);
            total += buf.limit();
            bufs[i] = buf;
        }

        ByteBuffer blip = MemoryUtil.memAlloc(total);
        int offset = 0;
        starts = new int[order.length];
        for (int i = 0; i < bufs.length; i++) {
            ByteBuffer buf = bufs[i];
            MemoryUtil.memCopy(buf, blip.position(offset).limit(offset + buf.limit()));
            blip.limit(blip.capacity());
            starts[i] = offset;
            offset += buf.limit();
            offsets.put(order[i], starts[i]);
        }
        blip.position(0).limit(blip.limit());

        int usageId = switch (usage) {
            case STATIC -> 35044;
            case DYNAMIC -> 35048;
            default -> throw new RuntimeException("???");
        };

        // upload vertices
        GlStateManager._glBindBuffer(34962, this.vertexBufferId);
        for (int i = 0; i < order.length; i++) {
            VertexFormatElement vertexFormatElement = order[i];
            int index = format.getElements().indexOf(vertexFormatElement);
            vertexFormatElement.setupBufferState(
                    index,
                    starts[i],
                    vertexFormatElement.getByteSize()
            );
        }
        RenderSystem.glBufferData(34962, blip, usageId);

        // upload indices
        this.indexCount = buffer.getIndices().limit() >> 1;
        GlStateManager._glBindBuffer(34963, this.indexBufferId);
        RenderSystem.glBufferData(34963, buffer.getIndices(), usageId);

        MemoryUtil.memFree(blip);
    }

    public void bind() {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(this.arrayObjectId);
    }

    public void draw() {
        RenderSystem.drawElements(mode.asGLMode, this.indexCount, VertexFormat.IndexType.SHORT.asGLType);
    }

    public void unbind() {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(0);
    }

    public void close() {
        if (this.vertexBufferId >= 0) {
            RenderSystem.glDeleteBuffers(this.vertexBufferId);
            this.vertexBufferId = -1;
        }

        if (this.indexBufferId >= 0) {
            RenderSystem.glDeleteBuffers(this.indexBufferId);
            this.indexBufferId = -1;
        }

        if (this.arrayObjectId >= 0) {
            RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
            this.arrayObjectId = -1;
        }
    }

    public void drawWithShader() {
        _drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
    }

    public void _drawWithShader(Matrix4f pModelViewMatrix, Matrix4f pProjectionMatrix, ShaderInstance pShader) {
        for (int i = 0; i < 12; ++i) {
            int j = RenderSystem.getShaderTexture(i);
            pShader.setSampler("Sampler" + i, j);
        }

        if (pShader.MODEL_VIEW_MATRIX != null) {
            pShader.MODEL_VIEW_MATRIX.set(pModelViewMatrix);
        }

        if (pShader.PROJECTION_MATRIX != null) {
            pShader.PROJECTION_MATRIX.set(pProjectionMatrix);
        }

        if (pShader.INVERSE_VIEW_ROTATION_MATRIX != null) {
            pShader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
        }

        if (pShader.COLOR_MODULATOR != null) {
            pShader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        if (pShader.GLINT_ALPHA != null) {
            pShader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }

        if (pShader.FOG_START != null) {
            pShader.FOG_START.set(RenderSystem.getShaderFogStart());
        }

        if (pShader.FOG_END != null) {
            pShader.FOG_END.set(RenderSystem.getShaderFogEnd());
        }

        if (pShader.FOG_COLOR != null) {
            pShader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }

        if (pShader.FOG_SHAPE != null) {
            pShader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }

        if (pShader.TEXTURE_MATRIX != null) {
            pShader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (pShader.GAME_TIME != null) {
            pShader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }

        if (pShader.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            pShader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }

        if (pShader.LINE_WIDTH != null && (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP)) {
            pShader.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
        }

        RenderSystem.setupShaderLights(pShader);
        pShader.apply();
        this.draw();
        pShader.clear();
    }

    public int getOffset(VertexFormatElement element) {
        return offsets.get(element);
    }

    public int getOffset(int dataIndex) {
        return starts[dataIndex];
    }

    public void updateData(ByteBuffer buffer, int offset) {
        GlStateManager._glBindBuffer(34962, this.vertexBufferId);
        GL30.glBufferSubData(34962, offset, buffer);
    }

    public void updateElement(ByteBuffer buffer, VertexFormatElement element) {
        GlStateManager._glBindBuffer(34962, this.vertexBufferId);
        GL30.glBufferSubData(34962, getOffset(element), buffer);
    }

    public void updateElement(ByteBuffer buffer, int dataIndex) {
        GlStateManager._glBindBuffer(34962, this.vertexBufferId);
        GL30.glBufferSubData(34962, getOffset(dataIndex), buffer);
    }
}
