package ichttt.mods.mcpaint.networking;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import ichttt.mods.mcpaint.common.MCPaintUtil;
import ichttt.mods.mcpaint.common.block.TileEntityCanvas;
import it.unimi.dsi.fastutil.ints.Int2ByteMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;

public class PaintDataClientPayload implements CustomPacketPayload {
    public static final Type<PaintDataClientPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mcpaint", "paint_data_download"));
    public static final StreamCodec<FriendlyByteBuf, PaintDataClientPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> payload.write(buf),
                    PaintDataClientPayload::new
            );

    // 字段与 PaintDataPayload 完全一样，构造、读写也相同（可复制）

    private final BlockPos pos;
    private final Direction facing;
    private final byte scale;
    private final byte part;
    private final byte maxParts;
    private final int[][] data;
    private final int[] palette;

    public PaintDataClientPayload(BlockPos pos, Direction facing, byte scale, int[] palette, int[][] data, byte part, byte maxParts) {
        this.pos = pos;
        this.facing = facing;
        this.scale = scale;
        this.palette = palette;
        this.data = data;
        this.part = part;
        this.maxParts = maxParts;
    }

    // 服务端构造（从字节流读取）
    public PaintDataClientPayload(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.scale = buf.readByte();
        this.facing = Direction.from3DDataValue(buf.readByte());
        this.part = buf.readByte();
        this.maxParts = buf.readByte();
        short rows = buf.readShort();
        short cols = buf.readShort();

        byte paletteLength = buf.readByte();
        if (paletteLength <= 0) {
            this.palette = null;
        } else {
            this.palette = new int[paletteLength];
            for (int i = 0; i < paletteLength; i++) {
                this.palette[i] = buf.readInt();
            }
        }

        this.data = new int[rows][cols];
        Int2ByteMap reversePalette = this.palette == null ? null : MCPaintUtil.buildReversePalette(this.palette);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (this.palette == null) {
                    this.data[i][j] = buf.readInt();
                } else {
                    this.data[i][j] = this.palette[buf.readByte()];
                }
            }
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeByte(scale);
        buf.writeByte(facing.get3DDataValue());
        buf.writeByte(this.part);
        buf.writeByte(this.maxParts);
        short rows = (short) (this.maxParts == 0 ? data.length : data.length / this.maxParts);
        buf.writeShort(rows);
        buf.writeShort((short) data[0].length);

        buf.writeByte(this.palette == null ? 0 : this.palette.length);
        if (this.palette != null) {
            for (int c : this.palette) {
                buf.writeInt(c);
            }
        }

        Int2ByteMap reversePalette = this.palette == null ? null : MCPaintUtil.buildReversePalette(this.palette);
        int offset = this.maxParts == 0 ? rows : rows * this.part;
        for (int i = offset - rows; i < offset; i++) {
            for (int value : this.data[i]) {
                if (reversePalette != null) {
                    buf.writeByte(reversePalette.get(value));
                } else {
                    buf.writeInt(value);
                }
            }
        }
    }

    // 客户端分片重组
    private static final Multimap<BlockPos, PaintDataClientPayload> clientPartMap = MultimapBuilder.hashKeys().hashSetValues().build();

    public static void handleClient(PaintDataClientPayload msg,IPayloadContext context) {
        context.enqueueWork(() -> {
            if (msg.maxParts == 0) {
                applyToWorld(msg);
            } else {
                synchronized (clientPartMap) {
                    clientPartMap.put(msg.pos, msg);
                    Collection<PaintDataClientPayload> messages = clientPartMap.get(msg.pos);
                    if (messages.size() == msg.maxParts) {
                        int[][] fullData = new int[msg.data.length * msg.maxParts][msg.data[0].length];
                        messages.stream().sorted(Comparator.comparingInt(o -> o.part)).forEachOrdered(p -> {
                            int offset = p.data.length * (p.part - 1);
                            for (int i = 0; i < p.data.length; i++) {
                                System.arraycopy(p.data[i], 0, fullData[i + offset], 0, p.data[i].length);
                            }
                        });
                        clientPartMap.removeAll(msg.pos);
                        PaintDataClientPayload combined = new PaintDataClientPayload(msg.pos, msg.facing, msg.scale, msg.palette, fullData, (byte)0, (byte)0);
                        applyToWorld(combined);
                    }
                }
            }
        });
    }

    private static void applyToWorld(PaintDataClientPayload msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.hasChunkAt(msg.pos)) return;
        BlockEntity te = mc.level.getBlockEntity(msg.pos);
        if (!(te instanceof TileEntityCanvas canvas)) return;
        if (msg.data == null) {
            canvas.removePaint(msg.facing);
        } else {
            canvas.getPaintFor(msg.facing).setDataWithPalette(msg.scale, msg.data, msg.palette, canvas, msg.facing);
        }
        te.setChanged();
    }

    // 静态方法 createAndSend 用于自动分割
    public static void createAndSend(BlockPos pos, Direction facing, byte scale, int[] palette, int[][] data, Consumer<PaintDataClientPayload> sender) {
        int length = data.length;
        if (length > 0) length *= data[0].length;
        if ((length > 32000) || (palette == null && length > 8000)) {
            int partsAsInt = (length / 32000) + 1;
            while (data.length % partsAsInt != 0) {
                partsAsInt++;
                if (partsAsInt > 32) throw new RuntimeException("Too many parts");
            }
            byte parts = (byte) partsAsInt;
            for (byte b = 1; b <= parts; b++) {
                sender.accept(new PaintDataClientPayload(pos, facing, scale, palette, data, b, parts));
            }
        } else {
            sender.accept(new PaintDataClientPayload(pos, facing, scale, palette, data, (byte)0, (byte)0));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}