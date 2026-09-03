package ichttt.mods.mcpaint.networking;

import ichttt.mods.mcpaint.common.MCPaintUtil;
import ichttt.mods.mcpaint.common.block.TileEntityCanvas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ClearSidePayload implements CustomPacketPayload {

    public static final Type<ClearSidePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mcpaint", "clear_side"));

    public static final StreamCodec<FriendlyByteBuf, ClearSidePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> payload.encode(buf),
                    ClearSidePayload::new
            );

    private final BlockPos pos;
    private final Direction facing;

    public ClearSidePayload(BlockPos pos, Direction facing) {
        this.pos = pos;
        this.facing = facing;
    }

    public ClearSidePayload(FriendlyByteBuf buffer) {
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

    public static void handleServer(ClearSidePayload message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (MCPaintUtil.isPosInvalid(player, message.pos)) return;

            BlockEntity te = player.level().getBlockEntity(message.pos);
            if (te instanceof TileEntityCanvas canvas) {
                canvas.removePaint(message.facing);
                te.setChanged();
                // 还可以向其他玩家广播清除
                // 通常服务端会发送 ClearSideClientPayload 给追踪该区块的玩家
            }
        });
    }
}