package com.github.andrew0030.dakimakuramod.dakimakura.client;

import com.github.andrew0030.dakimakuramod.DakimakuraModClient;
import com.github.andrew0030.dakimakuramod.dakimakura.Daki;
import com.github.andrew0030.dakimakuramod.dakimakura.DakiImageData;
import com.github.andrew0030.dakimakuramod.netwok.NetworkUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class DakiTexture extends AbstractTexture
{
    private static long lastLoad;
    private boolean requested = false;
    private final Daki daki;
//    private BufferedImage bufferedImageFull;
    private ByteBuffer imageBuffer;

    public DakiTexture(Daki daki)
    {
        this.daki = daki;
    }

    public boolean isLoaded()
    {
        if (this.id == -1) //TODO maybe change this?
        {
            if (DakiTexture.lastLoad + 25 < System.currentTimeMillis())
            {
                if (this.load())
                {
                    lastLoad = System.currentTimeMillis();
                }
                else
                {
                    if (!this.requested)
                    {
                        DakiTextureManagerClient textureManager = DakimakuraModClient.getDakiTextureManager();
                        int requests = textureManager.getTextureRequests().get();
                        if (requests < 2)
                        {
                            textureManager.getTextureRequests().incrementAndGet();
                            if (this.daki != null)
                            {
                                requested = true;
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
            // TODO make images scale to width size/3 to fix scaling issue son low res
            imageBufferFront = this.resize(imageBufferFront, frontWidth[0], frontHeight[0], textureSize / 2, textureSize, this.daki.isSmooth());
            imageBufferBack = this.resize(imageBufferBack, backWidth[0], backHeight[0], textureSize / 2, textureSize, this.daki.isSmooth());

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

    @Override
    public void load(ResourceManager resourceManager) {}

    private boolean load()
    {
        if (this.imageBuffer == null)
            return false;

        this.releaseId();
        try {
            //TODO make sure this works? Probably replace with a cleaner implementation
            NativeImage image = NativeImage.read(NativeImage.Format.RGB, this.imageBuffer);
            TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGB, this.getId(), 0, image.getWidth(), image.getHeight());
            this.bind();
            image.upload(0, 0, 0, false);

            //TODO make sure this is ok here I guess?
            MemoryUtil.memFree(this.imageBuffer);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
        ByteBuffer resizedBuffer = ByteBuffer.allocateDirect(newWidth * newHeight * 3); // 3 channels (RGB)
        // Resizes the image using STBImageResize
        //TODO: maybe look into using 16 for this for potentially better textures
//        STBImageResize.stbir_resize_uint16_generic();
        STBImageResize.stbir_resize_uint8(imgBuffer, oldWidth, oldHeight, 0, resizedBuffer, newWidth, newHeight, 0, 3); // 3 channels (RGB)
        // Frees the memory associated with the original input ByteBuffer
        MemoryUtil.memFree(imgBuffer);
        // Returns the resized image data ByteBuffer
        return resizedBuffer;
    }

    private ByteBuffer combineImages(ByteBuffer imageBufferFront, ByteBuffer imageBufferBack, int textureSize)
    {
        // Allocate memory for the combined image buffer
        ByteBuffer combinedBuffer = ByteBuffer.allocateDirect(textureSize * textureSize * 3); // 3 channels (RGB)
        // Copy front image to the left side of combined image
        imageBufferFront.rewind(); // Resets position to start
        combinedBuffer.put(imageBufferFront);
        // Copy back image to the right side of combined image
        imageBufferBack.rewind(); // Resets position to start
        combinedBuffer.position(textureSize / 2 * 3); // Start writing at the middle of the buffer
        combinedBuffer.put(imageBufferBack);
        // Reset position to start of the combined buffer
        combinedBuffer.rewind();

        MemoryUtil.memFree(imageBufferFront);
        MemoryUtil.memFree(imageBufferBack);

        return combinedBuffer;
    }

    /**
     * @param value The value for which to calculate the next power of 2.
     * @return The next power of 2 greater than or equal to the given value.
     */
    private int getNextPowerOf2(int value)
    {
        return (int) Math.pow(2, 32 - Integer.numberOfLeadingZeros(value - 1));
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
}