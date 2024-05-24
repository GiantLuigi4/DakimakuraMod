package com.github.andrew0030.dakimakuramod.util.buffer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class SmartBufferBuilder implements Closeable {
    public ByteBuffer getBuffer(VertexFormatElement vertexFormatElement) {
        BufBuilder buf = buffers.get(vertexFormatElement);
        buf.buffer.limit(buf.nextElementByte);
        return buf.buffer;
    }

    public ByteBuffer getIndices() {
        return indices.buffer.limit(indices.nextElementByte);
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    class BufBuilder {
        private int nextElementByte;
        ByteBuffer buffer = MemoryUtil.memAlloc(2097152);

        private void ensureCapacity(int pIncreaseAmount) {
            if (nextElementByte + pIncreaseAmount > this.buffer.capacity()) {
                int i = this.buffer.capacity();
                int j = i + roundUp(pIncreaseAmount);
                LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", i, j);
                ByteBuffer dst = MemoryUtil.memAlloc(j);
                MemoryUtil.memCopy(buffer.position(0), dst);
                dst.position(0);
                MemoryUtil.memFree(buffer);
                this.buffer = dst;
            }
        }

        private static int roundUp(int pX) {
            int i = 2097152;
            if (pX == 0) {
                return i;
            } else {
                if (pX < 0) {
                    i *= -1;
                }

                int j = pX % i;
                return j == 0 ? pX : pX + i - j;
            }
        }

        public void putByte(int pIndex, byte pByteValue) {
            this.buffer.put(nextElementByte + pIndex, pByteValue);
        }

        public void putShort(int pIndex, short pShortValue) {
            this.buffer.putShort(nextElementByte + pIndex, pShortValue);
        }

        public void putFloat(int pIndex, float pFloatValue) {
            this.buffer.putFloat(nextElementByte + pIndex, pFloatValue);
        }

        public void putInt(int pIndex, int pIntValue) {
            this.buffer.putInt(nextElementByte + pIndex, pIntValue);
        }

        public void advance(int amount) {
            nextElementByte += amount;
        }

        public void uvShort(short pU, short pV) {
            this.putInt(0, pU | (pV << 16));
        }
    }

    VertexFormat format;
    Map<VertexFormatElement, BufBuilder> buffers;
    BufBuilder POSITION;
    BufBuilder COLOR;
    BufBuilder NORMAL;
    BufBuilder LIGHTMAP;
    BufBuilder OVERLAY;
    BufBuilder UV;
    BufBuilder indices = new BufBuilder();

    private static final Logger LOGGER = LogUtils.getLogger();

    public SmartBufferBuilder(VertexFormat format) {
        this.format = format;
        buffers = new HashMap<>();
        // TODO: allow element buffers to be merged?
        // TODO: allow manually setting stride lengths on elements
        //       ex for why: color will always be 255 on each component, so stride can be 1
        for (VertexFormatElement element : format.getElements()) {
            buffers.put(element, new BufBuilder());
        }
        POSITION = buffers.get(DefaultVertexFormat.ELEMENT_POSITION);
        COLOR = buffers.get(DefaultVertexFormat.ELEMENT_COLOR);
        NORMAL = buffers.get(DefaultVertexFormat.ELEMENT_NORMAL);
        UV = buffers.get(DefaultVertexFormat.ELEMENT_UV);
        OVERLAY = buffers.get(DefaultVertexFormat.ELEMENT_UV1);
        LIGHTMAP = buffers.get(DefaultVertexFormat.ELEMENT_UV2);
    }

    public void index(int idx) {
        indices.ensureCapacity(2);
        indices.putShort(0, (short) idx);
        indices.advance(2);
    }

    public SmartBufferBuilder vertex(float pX, float pY, float pZ) {
        POSITION.ensureCapacity(3 * 4);
        POSITION.putFloat(0, pX);
        POSITION.putFloat(4, pY);
        POSITION.putFloat(8, pZ);
        POSITION.advance(3 * 4);
        return this;
    }

    public SmartBufferBuilder vertex(double pX, double pY, double pZ) {
        POSITION.ensureCapacity(3 * 4);
        POSITION.putFloat(0, (float) pX);
        POSITION.putFloat(4, (float) pY);
        POSITION.putFloat(8, (float) pZ);
        POSITION.advance(3 * 4);
        return this;
    }

    public SmartBufferBuilder color(int pRed, int pGreen, int pBlue, int pAlpha) {
        COLOR.ensureCapacity(4);
        COLOR.putByte(0, (byte) pRed);
        COLOR.putByte(1, (byte) pGreen);
        COLOR.putByte(2, (byte) pBlue);
        COLOR.putByte(3, (byte) pAlpha);
        COLOR.advance(4);
        return this;
    }

    public SmartBufferBuilder color(float pRed, float pGreen, float pBlue, float pAlpha) {
        COLOR.ensureCapacity(4);
        COLOR.putByte(0, (byte) (pRed * 255));
        COLOR.putByte(1, (byte) (pGreen * 255));
        COLOR.putByte(2, (byte) (pBlue * 255));
        COLOR.putByte(3, (byte) (pAlpha * 255));
        COLOR.advance(4);
        return this;
    }

    public SmartBufferBuilder uv(float pU, float pV) {
        UV.ensureCapacity(4 * 2);
        UV.putFloat(0, pU);
        UV.putFloat(4, pV);
        UV.advance(4 * 2);
        return this;
    }

    public SmartBufferBuilder overlay(int pOverlayUV) {
        return this.overlay(pOverlayUV & '\uffff', pOverlayUV >> 16 & '\uffff');
    }

    public SmartBufferBuilder overlay(int pU, int pV) {
        OVERLAY.ensureCapacity(2 * 2);
        OVERLAY.uvShort((short) pU, (short) pV);
        OVERLAY.advance(2 * 2);
        return this;
    }

    public SmartBufferBuilder lightmap(int pOverlayUV) {
        return this.lightmap(pOverlayUV & '\uffff', pOverlayUV >> 16 & '\uffff');
    }

    public SmartBufferBuilder lightmap(int pU, int pV) {
        LIGHTMAP.ensureCapacity(2 * 2);
        LIGHTMAP.uvShort((short) pU, (short) pV);
        LIGHTMAP.advance(2 * 2);
        return this;
    }

    public SmartBufferBuilder normal(float pX, float pY, float pZ) {
        NORMAL.ensureCapacity(3);
        NORMAL.putByte(0, normalIntValue(pX));
        NORMAL.putByte(1, normalIntValue(pY));
        NORMAL.putByte(2, normalIntValue(pZ));
        NORMAL.advance(3);
        return this;
    }

    public SmartBufferBuilder normal(double pX, double pY, double pZ) {
        NORMAL.ensureCapacity(3);
        NORMAL.putByte(0, normalIntValue((float) pX));
        NORMAL.putByte(1, normalIntValue((float) pY));
        NORMAL.putByte(2, normalIntValue((float) pZ));
        NORMAL.advance(3);
        return this;
    }

    public static byte normalIntValue(float pNum) {
        return (byte)((int)(Mth.clamp(pNum, -1.0F, 1.0F) * 127.0F) & 255);
    }

    public static float normalFloatValue(byte pNum) {
        return pNum / 127f;
    }

    public void close() {
        for (BufBuilder value : buffers.values())
            MemoryUtil.memFree(value.buffer);
        MemoryUtil.memFree(indices.buffer);
    }
}
