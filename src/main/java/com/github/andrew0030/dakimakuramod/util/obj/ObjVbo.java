package com.github.andrew0030.dakimakuramod.util.obj;

import com.github.andrew0030.dakimakuramod.util.buffer.SmartBufferBuilder;
import com.github.andrew0030.dakimakuramod.util.buffer.SuperVertexBuffer;
import com.github.andrew0030.dakimakuramod.util.iteration.LinkedStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.nio.ByteBuffer;

public class ObjVbo implements Closeable {
    private final ObjModel model;
    private final SuperVertexBuffer buffer;
    private ByteBuffer lB;
    private ByteBuffer nB;
    private final LinkedStack<Vector3f> norms = new LinkedStack<>();

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

    protected void createBuffers() {
        lB = MemoryUtil.memAlloc(buffer.getOffset(1) - buffer.getOffset(0));
        nB = MemoryUtil.memAlloc(buffer.getOffset(2) - buffer.getOffset(1));
    }

    int prevLight = 0;

    public void render(PoseStack stack, int packedLight) {
        // upload light
        if (prevLight != packedLight) {
            prevLight = packedLight;

            long asLong = packedLight | ((long) packedLight << 32L);

            for (int i = 0; i < lB.capacity() / 8; i++) lB.putLong(i << 3, asLong);
            buffer.updateElement(lB.position(0).limit(lB.capacity()), 0);
        }

        // upload normals
        {
            Matrix3f matrix3f = stack.last().normal();

            int index = 0;
            for (Vector3f norm : norms) {
                float x = norm.x;
                float y = norm.y;
                float z = norm.z;

                float nx = Math.fma(matrix3f.m00(), x, Math.fma(matrix3f.m10(), y, matrix3f.m20() * z));
                float ny = Math.fma(matrix3f.m01(), x, Math.fma(matrix3f.m11(), y, matrix3f.m21() * z));
                float nz = Math.fma(matrix3f.m02(), x, Math.fma(matrix3f.m12(), y, matrix3f.m22() * z));

                nB.put(index, SmartBufferBuilder.normalIntValue(nx));
                nB.put(index + 1, SmartBufferBuilder.normalIntValue(ny));
                nB.put(index + 2, SmartBufferBuilder.normalIntValue(nz));
                index += 3;
            }

            buffer.updateElement(nB.position(0).limit(nB.capacity()), 1);
        }

        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().last().pose().set(stack.last().pose());
        RenderSystem.applyModelViewMatrix();

        buffer.bind();
        buffer.drawWithShader();
        buffer.unbind();

        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
    }

    public void reload() {
        norms.clear();

        SmartBufferBuilder smartBufferBuilder = new SmartBufferBuilder(DefaultVertexFormat.NEW_ENTITY);
        model.render(smartBufferBuilder, 0, norms);
        buffer.bind();
        buffer.upload(smartBufferBuilder,
                // mutable elements
                DefaultVertexFormat.ELEMENT_UV2,
                DefaultVertexFormat.ELEMENT_NORMAL,
                // immutable elements
                DefaultVertexFormat.ELEMENT_POSITION,
                DefaultVertexFormat.ELEMENT_UV,
                // unused for dakis
                DefaultVertexFormat.ELEMENT_COLOR,
                DefaultVertexFormat.ELEMENT_UV1
        );
        buffer.unbind();
        smartBufferBuilder.close();

        createBuffers();
    }

    public void close() {
        buffer.close();
        MemoryUtil.memFree(lB);
        MemoryUtil.memFree(nB);
    }
}
