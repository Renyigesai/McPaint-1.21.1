package ichttt.mods.mcpaint.networking;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import ichttt.mods.mcpaint.MCPaint;
import ichttt.mods.mcpaint.common.MCPaintUtil;
import ichttt.mods.mcpaint.common.block.BlockCanvas;
import ichttt.mods.mcpaint.common.block.TileEntityCanvas;
import it.unimi.dsi.fastutil.ints.Int2ByteMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.function.Consumer;

public class PaintDataPayload implements CustomPacketPayload {
    public static final Type<PaintDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mcpaint", "paint_data_upload"));
    public static final StreamCodec<FriendlyByteBuf, PaintDataPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> payload.write(buf),
                    PaintDataPayload::new
            );

    private final BlockPos pos;
    private final Direction facing;
    private final byte scale;
    private final byte part;
    private final byte maxParts;
    private final int[][] data;
    private final int[] palette;

    // 客户端构造（用于发送）
    public PaintDataPayload(BlockPos pos, Direction facing, byte scale, int[] palette, int[][] data, byte part, byte maxParts) {
        this.pos = pos;
        this.facing = facing;
        this.scale = scale;
        this.palette = palette;
        this.data = data;
        this.part = part;
        this.maxParts = maxParts;
    }

    // 服务端构造（从字节流读取）
    public PaintDataPayload(FriendlyByteBuf buf) {
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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ================= 分片重组逻辑（服务端） =================
    private static final Multimap<BlockPos, PaintDataPayload> partMap = MultimapBuilder.hashKeys().hashSetValues().build();

    public static void handleServer(PaintDataPayload msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (msg.maxParts == 0) {
                handleSide(context, msg.pos, msg.facing, msg.scale, msg.palette, msg.data);
            } else {
                synchronized (partMap) {
                    partMap.put(msg.pos, msg);
                    Collection<PaintDataPayload> messages = partMap.get(msg.pos);
                    if (messages.size() == msg.maxParts) {
                        int[][] fullData = new int[msg.data.length * msg.maxParts][msg.data[0].length];
                        messages.stream().sorted(Comparator.comparingInt(o -> o.part)).forEachOrdered(p -> {
                            int offset = p.data.length * (p.part - 1);
                            for (int i = 0; i < p.data.length; i++) {
                                System.arraycopy(p.data[i], 0, fullData[i + offset], 0, p.data[i].length);
                            }
                        });
                        partMap.removeAll(msg.pos);
                        handleSide(context, msg.pos, msg.facing, msg.scale, msg.palette, fullData);
                    }
                }
            }
        });
    }

    private static void handleSide(IPayloadContext context, BlockPos pos, Direction facing, byte scale, int[] palette, int[][] data) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (player == null || !player.level().isLoaded(pos)) return;

        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof BlockCanvas)) {
            MCPaint.LOGGER.warn("Invalid block at {} selected by {} - not canvas", pos, player.getName());
            return;
        }

        BlockEntity te = player.level().getBlockEntity(pos);
        if (!(te instanceof TileEntityCanvas canvas)) {
            MCPaint.LOGGER.warn("Invalid block entity at {} selected by {}", pos, player.getName());
            return;
        }

        if (data == null) {
            canvas.removePaint(facing);
        } else {
            canvas.getPaintFor(facing).setDataWithPalette(scale, data, palette, canvas, facing);
        }
        te.setChanged();

        // 广播给追踪该区块的玩家
        ServerLevel level = (ServerLevel) player.level();
        LevelChunk chunk = level.getChunkAt(pos);
        if (data == null) {
            PaintDataClientPayload clear = new PaintDataClientPayload(pos, facing, scale, null, null, (byte)0, (byte)0);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), clear);
        } else {
            // 分割并发送
            PaintDataClientPayload.createAndSend(pos, facing, scale, palette, data,
                    payload -> PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), payload));
        }
    }

    public static void createAndSend(BlockPos pos, Direction facing, byte scale, int[] palette, int[][] data,
                                     Consumer<PaintDataPayload> sender) {
        int length = data.length;
        if (length > 0) length *= data[0].length;
        // 根据大小决定是否分片
        if ((length > 32000) || (palette == null && length > 8000)) {
            int partsAsInt = (length / 32000) + 1;
            while (data.length % partsAsInt != 0) {
                partsAsInt++;
                if (partsAsInt > 32) throw new RuntimeException("Image too large");
            }
            byte parts = (byte) partsAsInt;
            for (byte b = 1; b <= parts; b++) {
                PaintDataPayload payload = new PaintDataPayload(pos, facing, scale, palette, data, b, parts);
                sender.accept(payload);
            }
        } else {
            PaintDataPayload payload = new PaintDataPayload(pos, facing, scale, palette, data, (byte)0, (byte)0);
            sender.accept(payload);
        }
    }
}