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

public class DrawAbortPayload implements CustomPacketPayload {

    public static final Type<DrawAbortPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mcpaint", "draw_abort"));

    public static final StreamCodec<FriendlyByteBuf, DrawAbortPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> payload.encode(buf),   // 编码器
                    DrawAbortPayload::new                   // 解码器
            );

    private final BlockPos pos;

    public DrawAbortPayload(BlockPos pos) {
        this.pos = pos;
    }

    public DrawAbortPayload(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(DrawAbortPayload message, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 获取服务器玩家，如果转换失败则返回
            if (!(context.player() instanceof ServerPlayer player)) return;

            // 假设 MCPaintUtil.isPosInvalid 在新版仍可用，或自行实现
            if (MCPaintUtil.isPosInvalid(player, message.pos)) return;

            BlockEntity te = player.level().getBlockEntity(message.pos);
            if (te instanceof TileEntityCanvas canvas) {
                boolean hasData = false;
                for (Direction facing : Direction.values()) {
                    if (canvas.hasPaintFor(facing)) {
                        hasData = true;
                        break;
                    }
                }
                if (!hasData && canvas.getContainedState() != null) {
                    player.level().setBlockAndUpdate(message.pos, canvas.getContainedState());
                }
            }
        });
    }
}