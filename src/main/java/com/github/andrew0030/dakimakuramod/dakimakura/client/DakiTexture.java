package com.github.andrew0030.dakimakuramod.dakimakura.client;

import com.github.andrew0030.dakimakuramod.DakimakuraModClient;
import com.github.andrew0030.dakimakuramod.dakimakura.Daki;
import com.github.andrew0030.dakimakuramod.dakimakura.DakiImageData;
import com.github.andrew0030.dakimakuramod.netwok.NetworkUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class DakiTexture implements AutoCloseable
{
    private final Daki daki;
    private int id = -1;
    private static long lastLoad;
    private boolean requested = false;
    private ByteBuffer imageBuffer;

    public DakiTexture(Daki daki)
    {
        this.daki = daki;
    }

    public boolean isLoaded()
    {
        if (this.id == -1)
        {
            if (DakiTexture.lastLoad + 25 < System.currentTimeMillis())
            {
                if (this.load()) {
                    DakiTexture.lastLoad = System.currentTimeMillis();
                } else {
                    if (!this.requested)
                    {
                        DakiTextureManagerClient textureManager = DakimakuraModClient.getDakiTextureManager();
                        int requests = textureManager.getTextureRequests().get();
                        if (requests < 2)
                        {
                            textureManager.getTextureRequests().incrementAndGet();
                            if (this.daki != null)
                            {
                                this.requested = true;
                                NetworkUtil.clientRequestTextures(daki);
                            }
                        }
                    }
                    else
                    {
                        if (this.imageBuffer != null)
                            this.load();
                    }
                }
            }
            return false;
        }
        return true;
    }

    public void createImageBuffer(DakiImageData imageData)
    {
        try (InputStream inputStreamFront = imageData.getTextureFront() != null ? new ByteArrayInputStream(imageData.getTextureFront()) : this.getMissingTexture();
             InputStream inputStreamBack = imageData.getTextureBack() != null ? new ByteArrayInputStream(imageData.getTextureBack()) : this.getMissingTexture()) {

            // Load images using STBImage
            int[] frontWidth = new int[1];
            int[] frontHeight = new int[1];
            int[] frontComp = new int[1];
            int[] backWidth = new int[1];
            int[] backHeight = new int[1];
            int[] backComp = new int[1];
            ByteBuffer imageBufferFront = STBImage.stbi_load_from_memory(this.inputStreamToByteBuffer(inputStreamFront), frontWidth, frontHeight, frontComp, 3);
            ByteBuffer imageBufferBack = STBImage.stbi_load_from_memory(this.inputStreamToByteBuffer(inputStreamBack), backWidth, backHeight, backComp, 3);

            // We determine make sure the daki is within max size
            int maxTexture = Math.max(frontHeight[0], backHeight[0]);
            int textureSize = this.getMaxTextureSize();
            textureSize = Math.min(textureSize, maxTexture);

            // Resize images if needed
            imageBufferFront = this.resize(imageBufferFront, frontWidth[0], frontHeight[0], textureSize / 3, textureSize, this.daki.isSmooth());
            imageBufferBack = this.resize(imageBufferBack, backWidth[0], backHeight[0], textureSize / 3, textureSize, this.daki.isSmooth());

            // Stores the combines front and back images into one buffer
            this.imageBuffer = this.combineImages(imageBufferFront, imageBufferBack, textureSize);

            imageData.clearTextureData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close()
    {
        this.releaseId();
    }

    private boolean load()
    {
        // If the ImageBuffer is null we can't load the image
        if (this.imageBuffer == null) return false;
        this.releaseId(); // We remove former textures
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.getId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST); // LINEAR for smooth
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, 12, 36 * 2, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, this.imageBuffer);

        // Frees the memory associated with the Image Buffer
//        MemoryUtil.memFree(this.imageBuffer);
//        this.imageBuffer = null; TODO: maybe we clear it? I mean we need to at some point but doing it here likely breaks the logic in isLoaded
        return true;
    }

    /**
     * Converts the given {@link InputStream} to a {@link ByteBuffer}
     * @param inputStream The {@link InputStream} to be converted
     * @return A new {@link ByteBuffer} containing all the bytes from the {@link InputStream}
     */
    private ByteBuffer inputStreamToByteBuffer(InputStream inputStream) throws IOException
    {
        byte[] imageData = inputStream.readAllBytes();
        ByteBuffer buffer = ByteBuffer.allocateDirect(imageData.length);
        buffer.put(imageData);
        buffer.flip();
        return buffer;
    }

    private ByteBuffer resize(ByteBuffer imgBuffer, int oldWidth, int oldHeight, int newWidth, int newHeight, boolean isSmooth)
    {
        // Allocates memory for the resized image
        ByteBuffer resizedBuffer = MemoryUtil.memAlloc(newWidth * newHeight * 3); // 3 channels (RGB)

        // TODO: maybe look into using 16 for this for potentially better textures
        // STBImageResize.stbir_resize_uint16_generic();

        // Resizes the image using STBImageResize
        STBImageResize.stbir_resize_uint8(imgBuffer, oldWidth, oldHeight, 0, resizedBuffer, newWidth, newHeight, 0, 3); // 3 channels (RGB)
        MemoryUtil.memFree(imgBuffer); // Frees the memory associated with the original input ByteBuffer
        return resizedBuffer; // Returns the resized image data ByteBuffer
    }

    private ByteBuffer combineImages(ByteBuffer imageBufferFront, ByteBuffer imageBufferBack, int textureSize)
    {
        // Allocate memory for the combined image buffer
        ByteBuffer combinedBuffer = MemoryUtil.memAlloc(textureSize * textureSize * 3); // 3 channels (RGB)
        imageBufferFront.rewind(); // Resets position to start
        combinedBuffer.put(imageBufferFront);
        imageBufferBack.rewind(); // Resets position to start
        combinedBuffer.position(imageBufferFront.capacity()); // Start writing at the middle of the buffer
        combinedBuffer.put(imageBufferBack);
        combinedBuffer.rewind(); // Reset position to start of the combined buffer

        // Frees the memory associated with the original input ByteBuffers
        MemoryUtil.memFree(imageBufferFront);
        MemoryUtil.memFree(imageBufferBack);

        return combinedBuffer;
    }

    /**
     * @return The max texture size an image can be in pixels
     */
    private int getMaxTextureSize()
    {
        int maxGpuSize = DakimakuraModClient.getMaxGpuTextureSize();
//        int maxConfigSize = ConfigHandler.textureMaxSize;
//        return Math.min(maxGpuSize, maxConfigSize);
        return maxGpuSize; // TODO: add config support for max image size
    }

    /**
     * @return An {@link InputStream} representing a missing texture
     */
    private InputStream getMissingTexture()
    {
        return DakiTexture.class.getClassLoader().getResourceAsStream("assets/dakimakuramod/textures/obj/missing.png");
    }

    /**
     * @param value The value for which to calculate the next power of 2.
     * @return The next power of 2 greater than or equal to the given value.
     */
    private int getNextPowerOf2(int value)
    {
        return (int) Math.pow(2, 32 - Integer.numberOfLeadingZeros(value - 1));
    }

    public int getId()
    {
        if (this.id == -1)
            this.id = GL11.glGenTextures();
        return this.id;
    }

    public void releaseId()
    {
        if (this.id != -1)
        {
            GL11.glDeleteTextures(this.id);
            this.id = -1;
        }
    }
}