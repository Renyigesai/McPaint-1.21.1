package ichttt.mods.mcpaint.common.item;

import com.mojang.serialization.Codec;
import ichttt.mods.mcpaint.common.MCPaintUtil;
import ichttt.mods.mcpaint.common.capability.CapabilityPaintable;
import ichttt.mods.mcpaint.common.capability.IPaintable;
import ichttt.mods.mcpaint.common.capability.Paint;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// 1. 定义存储数据的 Record，它需要是可序列化的
public record PaintDataComponent(
        short pixelCountX,
        byte scaleFactor,
        int[][] pictureData,
        int[] palette
) {
    // 可以添加工厂方法用于从 IPaintable 创建
    public static PaintDataComponent from(IPaintable paintable) {
        int[][] pictureData = paintable.getPictureData(true);
        int[][] copied = pictureData == null ? null : MCPaintUtil.copyOf(pictureData);
        return new PaintDataComponent(
                paintable.getPixelCountX(),
                paintable.getScaleFactor(),
                copied,
                paintable.getPalette()
        );
    }

    public static final Codec<PaintDataComponent> CODEC =
            CompoundTag.CODEC.xmap(
                    tag -> {
                        // 从 CompoundTag 读取数据构建 PaintDataComponent
                        Paint tempPaint = new Paint();
                        CapabilityPaintable.readFromNBT(tempPaint, tag);
                        return new PaintDataComponent(
                                tempPaint.getPixelCountX(),
                                tempPaint.getScaleFactor(),
                                tempPaint.getPictureData(true),
                                tempPaint.getPalette()
                        );
                    },
                    component -> {
                        // 将 PaintDataComponent 写入 CompoundTag
                        Paint tempPaint = new Paint();
                        tempPaint.setDataWithPalette(
                                component.scaleFactor(),
                                component.pictureData(),
                                component.palette(),
                                null, null
                        );
                        CompoundTag tag = new CompoundTag();
                        CapabilityPaintable.writeToNBT(tempPaint, tag);
                        return tag;
                    }
            );

    public static final StreamCodec<ByteBuf, PaintDataComponent> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(
                    // 2. 如何从 CompoundTag 解码为 PaintDataComponent
                    tag -> {
                        Paint tempPaint = new Paint();
                        CapabilityPaintable.readFromNBT(tempPaint, tag);
                        return new PaintDataComponent(
                                tempPaint.getPixelCountX(),
                                tempPaint.getScaleFactor(),
                                tempPaint.getPictureData(true),
                                tempPaint.getPalette()
                        );
                    },
                    // 3. 如何将 PaintDataComponent 编码为 CompoundTag
                    component -> {
                        Paint tempPaint = new Paint();
                        tempPaint.setDataWithPalette(
                                component.scaleFactor(),
                                component.pictureData(),
                                component.palette(),
                                null, null
                        );
                        CompoundTag tag = new CompoundTag();
                        CapabilityPaintable.writeToNBT(tempPaint, tag);
                        return tag;
                    }
            );
}