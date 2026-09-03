package ichttt.mods.mcpaint.networking;

import ichttt.mods.mcpaint.common.MCPaintUtil;
import ichttt.mods.mcpaint.common.block.TileEntityCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ClearSideClientPayload implements CustomPacketPayload {

    public static final Type<ClearSideClientPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mcpaint", "clear_side_client"));

    public static final StreamCodec<FriendlyByteBuf, ClearSideClientPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> payload.encode(buf),
                    ClearSideClientPayload::new
            );

    private final BlockPos pos;
    private final Direction facing;

    public ClearSideClientPayload(BlockPos pos, Direction facing) {
        this.pos = pos;
        this.facing = facing;
    }

    public ClearSideClientPayload(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), Direction.from3DDataValue(buffer.readByte()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(facing.get3DDataValue());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(ClearSideClientPayload message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !mc.level.hasChunkAt(message.pos)) return;
            BlockEntity te = mc.level.getBlockEntity(message.pos);
            if (te instanceof TileEntityCanvas canvas) {
                canvas.removePaint(message.facing);
                te.setChanged();
            }
        });
    }
}