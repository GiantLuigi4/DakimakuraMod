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
        reload(stack, packedLight);
        buffer.drawWithShader(
                stack.last().pose(),
                RenderSystem.getProjectionMatrix(),
                RenderSystem.getShader()
        );
        VertexBuffer.unbind();
    }

    static BufferBuilder smartBufferBuilder = new BufferBuilder(20);

    public void reload(PoseStack stack, int light) {
        smartBufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);
        model.render(stack.last().normal(), smartBufferBuilder, light);
        buffer.bind();
        BufferBuilder.RenderedBuffer rb = smartBufferBuilder.end();
        buffer.upload(rb);
    }

    public void close() {
        buffer.close();
    }
}
