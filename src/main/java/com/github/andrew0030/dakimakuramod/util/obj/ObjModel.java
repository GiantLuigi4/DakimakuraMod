package com.github.andrew0030.dakimakuramod.util.obj;

import com.github.andrew0030.dakimakuramod.DakimakuraMod;
import com.github.andrew0030.dakimakuramod.util.buffer.SmartBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.apache.commons.compress.utils.IOUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public record ObjModel(Vector3f[] v, Vec2[] vt, Vector3f[] vn, Face[] faces)
{
    record VertexKey(Vector3f vert, Vector2f tex, Vector3f norm) {
    }

    public void render(PoseStack stack, SmartBufferBuilder buffer, int packedLight)
    {
//        buffer.color(255, 255, 255, 255);
//        buffer.overlay(OverlayTexture.NO_OVERLAY);
//        buffer.lightmap(0, 0);

//        for (Vector3f vector3f : v) {
//            buffer.vertex(vector3f.x, vector3f.y, vector3f.z);
//        }
//        for (Vector3f vector3f : vn) {
//            buffer.normal(vector3f.x, vector3f.y, vector3f.z);
//        }
//        for (Vec2 vec2 : vt) {
//            Vector2f vt = new Vector2f(vec2.x, vec2.y);
//            vt.x *= 2;
//            vt.y /= 2;
//            if (vt.x > 1) {
//                vt.x -= 1;
//            } else {
//                vt.y += 0.5;
//            }
//        }

        int index = 0;

        try
        {
//            HashMap<VertexKey, Integer> keys = new HashMap<>();
//            for (Face face : faces) {
//            }

            for (Face face : this.faces)
            {
                Vector3f v1 = v[face.v1 - 1];
                Vector3f v2 = v[face.v2 - 1];
                Vector3f v3 = v[face.v3 - 1];

                Vector2f vt1 = new Vector2f(vt[face.vt1 - 1].x, vt[face.vt1 - 1].y);
                Vector2f vt2 = new Vector2f(vt[face.vt2 - 1].x, vt[face.vt2 - 1].y);
                Vector2f vt3 = new Vector2f(vt[face.vt3 - 1].x, vt[face.vt3 - 1].y);

                vt1.x *= 2;
                if (vt1.x > 1) vt1.x -= 1;
                else vt1.y += 0.5;
                vt1.y /= 2;

                vt2.x *= 2;
                if (vt2.x > 1) vt2.x -= 1;
                else vt2.y += 0.5f;
                vt2.y /= 2;

                vt3.x *= 2;
                if (vt3.x > 1) vt3.x -= 1;
                else vt3.y += 0.5f;
                vt3.y /= 2;

                Vector3f vn1 = vn[face.vn1 - 1];
                Vector3f vn2 = vn[face.vn2 - 1];
                Vector3f vn3 = vn[face.vn3 - 1];

                this.addVertex(buffer, v1.x(), v1.y(), v1.z(), vt1.x, 1-vt1.y, packedLight, vn1.x(), vn1.y(), vn1.z());
                this.addVertex(buffer, v2.x(), v2.y(), v2.z(), vt2.x, 1-vt2.y, packedLight, vn2.x(), vn2.y(), vn2.z());
                this.addVertex(buffer, v3.x(), v3.y(), v3.z(), vt3.x, 1-vt3.y, packedLight, vn3.x(), vn3.y(), vn3.z());
                buffer.index(index++);
                buffer.index(index++);
                buffer.index(index++);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void addVertex(SmartBufferBuilder buffer, float x, float y, float z, float u, float v, int packedLight, float nx, float ny, float nz) {
        buffer.vertex(x, y, z);
        buffer.normal(nx, ny, nz);
        buffer.uv(u, v);
        buffer.color(255, 255, 255, 255);
        buffer.overlay(OverlayTexture.NO_OVERLAY);
        buffer.lightmap(packedLight);
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

        return new ObjModel(vArray, vtArray, vnArray, faces);
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

        public VertexKey[] keys(ObjModel model) {
            Vector2f vt1 = new Vector2f(model.vt[this.vt1 - 1].x, model.vt[this.vt1 - 1].y);
            Vector2f vt2 = new Vector2f(model.vt[this.vt2 - 1].x, model.vt[this.vt2 - 1].y);
            Vector2f vt3 = new Vector2f(model.vt[this.vt3 - 1].x, model.vt[this.vt3 - 1].y);

            vt1.x *= 2;
            if (vt1.x > 1) vt1.x -= 1;
            else vt1.y += 0.5;
            vt1.y /= 2;

            vt2.x *= 2;
            if (vt2.x > 1) vt2.x -= 1;
            else vt2.y += 0.5f;
            vt2.y /= 2;

            vt3.x *= 2;
            if (vt3.x > 1) vt3.x -= 1;
            else vt3.y += 0.5f;
            vt3.y /= 2;

            return new VertexKey[]{
                    new VertexKey(model.v[v1], vt1, model.vn[vn1]),
                    new VertexKey(model.v[v2], vt2, model.vn[vn2]),
                    new VertexKey(model.v[v3], vt3, model.vn[vn3]),
            };
        }
    }
}