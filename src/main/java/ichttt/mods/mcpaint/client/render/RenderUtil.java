package ichttt.mods.mcpaint.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;

public class RenderUtil {
    public static void renderInGui(Matrix4f matrix, int startLeft, int startTop, byte scaleFactor, VertexConsumer builder, int[][] data) {
        for (int x = 0; x < data.length; x++) {
            for (int y = 0; y < data[x].length; y++) {
                int color = data[x][y];
                if ((color >> 24 & 255) == 0) continue;
                float left = startLeft + x * scaleFactor;
                float top = startTop + y * scaleFactor;
                float right = left + scaleFactor;
                float bottom = top + scaleFactor;
                int a = (color >> 24 & 255);
                int r = (color >> 16 & 255);
                int g = (color >> 8 & 255);
                int b = (color & 255);
                builder.addVertex(matrix, left, bottom, 0).setColor(r, g, b, a);
                builder.addVertex(matrix, right, bottom, 0).setColor(r, g, b, a);
                builder.addVertex(matrix, right, top, 0).setColor(r, g, b, a);
                builder.addVertex(matrix, left, top, 0).setColor(r, g, b, a);
            }
        }
    }

    public static void renderInGame(Matrix4f matrix4f, byte scaleFactor, VertexConsumer builder, int[][] picture, int light) {
        for (int x = 0; x < picture.length; x++) {
            int[] yPos = picture[x];
            for (int y = 0; y < yPos.length; y++) {
                int color = picture[x][y];
                float left = ((x * scaleFactor) / 128F) + scaleFactor / 128F;
                float top = 1 - ((y * scaleFactor) / 128F) - scaleFactor / 128F;
                float right = left - (scaleFactor / 128F);
                float bottom = top + (scaleFactor / 128F);
                drawToBuffer(matrix4f, color, builder, left, top, right, bottom, light);
            }
        }
    }

    public static boolean drawToBuffer(Matrix4f matrix4f, int color, VertexConsumer builder, float left, float top, float right, float bottom, int light) {
        int a = (color >> 24 & 255);
        if (a <= 2) return true;
        int r = (color >> 16 & 255);
        int g = (color >> 8 & 255);
        int b = (color & 255);

        builder.addVertex(matrix4f, left, bottom, 0).setColor(r, g, b, a).setLight(light);
        builder.addVertex(matrix4f, right, bottom, 0).setColor(r, g, b, a).setLight(light);
        builder.addVertex(matrix4f, right, top, 0).setColor(r, g, b, a).setLight(light);
        builder.addVertex(matrix4f, left, top, 0).setColor(r, g, b, a).setLight(light);
        return false;
    }

//    public static boolean drawToBuffer(Matrix4f matrix4f, int color, VertexConsumer builder, float left, float top, float right, float bottom, int light) {
//        //See drawRect(int left, int top, int right, int bottom, int color
//        int a = (color >> 24 & 255);
//        if (a <= 2) return true;
//        int r = (color >> 16 & 255);
//        int g = (color >> 8 & 255);
//        int b = (color & 255);
//        builder.addVertex(matrix4f, left, bottom, 0).setColor(r, g, b, a).setUv2(light,0).endVertex();
//        builder.addVertex(matrix4f, right, bottom, 0).setColor(r, g, b, a).setUv2(light,0).endVertex();
//        builder.addVertex(matrix4f, right, top, 0).setColor(r, g, b, a).setUv2(light,0).endVertex();
//        builder.addVertex(matrix4f, left, top, 0).setColor(r, g, b, a).setUv2(light,0).endVertex();
//        return false;
//    }
}
