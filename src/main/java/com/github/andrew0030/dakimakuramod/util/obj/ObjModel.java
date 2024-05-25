package com.github.andrew0030.dakimakuramod.util.obj;

import com.github.andrew0030.dakimakuramod.DakimakuraMod;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.apache.commons.compress.utils.IOUtils;
import org.joml.Matrix3f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public record ObjModel(Vector3f[] v, Vec2[] vt, Vector3f[] vn, Face[] faces)
{
    record VertexKey(Vector3f vert, Vector2f tex, Vector3f norm) {
    }

    public void render(Matrix3f matrix3f, VertexConsumer buffer, int packedLight)
    {
        try
        {
            for (Face face : faces) {
                for (VertexKey key : face.keys(this)) {
                    this.addVertex(matrix3f, buffer, key.vert.x(), key.vert.y(), key.vert.z(), key.tex.x, 1 - key.tex.y, packedLight, key.norm.x(), key.norm.y(), key.norm.z());
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void addVertex(Matrix3f matrix, VertexConsumer buffer, float x, float y, float z, float u, float v, int packedLight, float nx, float ny, float nz) {
        buffer.vertex(x, y, z);
        buffer.color(1f, 1f, 1f, 1f);
        buffer.uv(u, v);
        buffer.overlayCoords(OverlayTexture.NO_OVERLAY);
        buffer.uv2(packedLight);
        normal(buffer, matrix, nx, ny, nz);
        buffer.endVertex();
    }

    private void normal(VertexConsumer bufferBuilder, Matrix3f matrix3f, float x, float y, float z) {
        // Calling 'bufferBuilder.normal(matrix3f, x, y, z)' allocates a Vector3f
        // To avoid allocating so many short-lived vectors we do the transform ourselves instead
        float nx = Math.fma(matrix3f.m00(), x, Math.fma(matrix3f.m10(), y, matrix3f.m20() * z));
        float ny = Math.fma(matrix3f.m01(), x, Math.fma(matrix3f.m11(), y, matrix3f.m21() * z));
        float nz = Math.fma(matrix3f.m02(), x, Math.fma(matrix3f.m12(), y, matrix3f.m22() * z));

        bufferBuilder.normal(nx, ny, nz);
    }

    public static ObjModel loadModel(ResourceLocation resourceLocation)
    {
        byte[] modelData = ObjModel.loadResource(resourceLocation);
        String modelString = new String(modelData);
        String[] modelLines = modelString.split("\\r?\\n");

        ArrayList<Vector3f> vList = new ArrayList<>();
        ArrayList<Vec2> vtList = new ArrayList<>();
        ArrayList<Vector3f> vnList = new ArrayList<>();
        ArrayList<Face> faceList = new ArrayList<>();

        for (String line : modelLines)
        {
            String[] lineSpit = line.split(" ");
            switch (lineSpit[0])
            {
                case "v" -> vList.add(new Vector3f(Float.parseFloat(lineSpit[1]), Float.parseFloat(lineSpit[2]), Float.parseFloat(lineSpit[3])));
                case "vt" -> vtList.add(new Vec2(Float.parseFloat(lineSpit[1]), Float.parseFloat(lineSpit[2])));
                case "vn" -> vnList.add(new Vector3f(Float.parseFloat(lineSpit[1]), Float.parseFloat(lineSpit[2]), Float.parseFloat(lineSpit[3])));
                case "f" -> faceList.add(new Face(lineSpit[1], lineSpit[2], lineSpit[3]));
                default -> {
                }
            }
        }

        Vector3f[] vArray = vList.toArray(new Vector3f[0]);
        Vec2[] vtArray = vtList.toArray(new Vec2[0]);
        Vector3f[] vnArray = vnList.toArray(new Vector3f[0]);
        Face[] faces = faceList.toArray(new Face[0]);

        ObjModel mdl = new ObjModel(vArray, vtArray, vnArray, faces);
        for (Face face : faces) face.computeKeys(mdl);

        return mdl;
    }

    private static byte[] loadResource(ResourceLocation resourceLocation)
    {
        InputStream input = null;
        ByteArrayOutputStream output = null;
        try
        {
            input = ObjModel.class.getClassLoader().getResourceAsStream("assets/" + DakimakuraMod.MODID + "/" + resourceLocation.getPath());
            if (input != null)
            {
                output = new ByteArrayOutputStream();
                IOUtils.copy(input, output);
                output.flush();
                return output.toByteArray();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
        return null;
    }

    private static class Face
    {
        // Vertex
        public final int v1;
        public final int v2;
        public final int v3;
        // Textfinal ure
        public final int vt1;
        public final int vt2;
        public final int vt3;
        // Normfinal al
        public final int vn1;
        public final int vn2;
        public final int vn3;

        VertexKey[] keys;

        public Face(String v1, String v2, String v3)
        {
            String[] s1 = v1.split("/");
            String[] s2 = v2.split("/");
            String[] s3 = v3.split("/");

            this.v1 = Integer.parseInt(s1[0]);
            this.vt1 = Integer.parseInt(s1[1]);
            this.vn1 = Integer.parseInt(s1[2]);

            this.v2 = Integer.parseInt(s2[0]);
            this.vt2 = Integer.parseInt(s2[1]);
            this.vn2 = Integer.parseInt(s2[2]);

            this.v3 = Integer.parseInt(s3[0]);
            this.vt3 = Integer.parseInt(s3[1]);
            this.vn3 = Integer.parseInt(s3[2]);
        }

        private void computeKeys(ObjModel model)
        {
            Vector2f vt1 = new Vector2f(model.vt[this.vt1 - 1].x, model.vt[this.vt1 - 1].y);
            Vector2f vt2 = new Vector2f(model.vt[this.vt2 - 1].x, model.vt[this.vt2 - 1].y);
            Vector2f vt3 = new Vector2f(model.vt[this.vt3 - 1].x, model.vt[this.vt3 - 1].y);

            vt1.x *= 2;
            vt1.y /= 2;
            if (vt1.x > 1) vt1.x -= 1;
            else vt1.y += 0.5;

            vt2.x *= 2;
            vt2.y /= 2;
            if (vt2.x > 1) vt2.x -= 1;
            else vt2.y += 0.5f;

            vt3.x *= 2;
            vt3.y /= 2;
            if (vt3.x > 1) vt3.x -= 1;
            else vt3.y += 0.5f;

            this.keys = new VertexKey[]{
                    new VertexKey(model.v[v1 - 1], vt1, model.vn[vn1 - 1]),
                    new VertexKey(model.v[v2 - 1], vt2, model.vn[vn2 - 1]),
                    new VertexKey(model.v[v3 - 1], vt3, model.vn[vn3 - 1]),
            };
        }

        private VertexKey[] keys(ObjModel model)
        {
            return keys;
        }
    }
}