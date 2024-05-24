package com.github.andrew0030.dakimakuramod.util.obj;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;

import java.io.Closeable;

public class ObjVbo implements Closeable {
    private final ObjModel model;
    private final VertexBuffer buffer;

    public ObjVbo(
            ObjModel model
    ) {
        this.model = model;
        this.buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    }

    public void render(PoseStack stack, int packedLight) {
        Lighting.setupLevel(RenderSystem.getModelViewMatrix());

        reload(stack, packedLight);
        buffer.bind();
        buffer.drawWithShader(
                new Matrix4f(),
                RenderSystem.getProjectionMatrix(),
                RenderSystem.getShader()
        );
        buffer.unbind();

        Lighting.setupLevel(RenderSystem.getModelViewMatrix());
    }

    static BufferBuilder smartBufferBuilder = new BufferBuilder(20);

    public void reload(PoseStack stack, int light) {
        smartBufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);
        model.render(stack, smartBufferBuilder, light);
        buffer.bind();
        BufferBuilder.RenderedBuffer rb = smartBufferBuilder.end();
        buffer.upload(rb);
        buffer.unbind();
    }

    public void close() {
        buffer.close();
    }
}
