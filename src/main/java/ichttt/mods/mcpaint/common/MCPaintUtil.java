package ichttt.mods.mcpaint.common;

import ichttt.mods.mcpaint.MCPaint;
import ichttt.mods.mcpaint.common.block.TileEntityCanvas;
import ichttt.mods.mcpaint.common.capability.IPaintable;
import ichttt.mods.mcpaint.networking.ClearSidePayload;
import ichttt.mods.mcpaint.networking.PaintDataPayload;
import it.unimi.dsi.fastutil.ints.Int2ByteMap;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class MCPaintUtil {
    public static boolean isPosInvalid(ServerPlayer player, BlockPos pos) {
        if (!player.level().hasChunkAt(pos)) {
            MCPaint.LOGGER.warn("Player" + player.getName() + " is trying to write to unloaded block");
            player.connection.disconnect(Component.literal("Trying to write to unloaded block"));
            return true;
        }

        if (Math.sqrt(player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ())) > (Math.round(3) + 5)) {
            MCPaint.LOGGER.warn("Player" + player.getName() + " is writing to out of reach block!");
            return true;
        }
        return false;
    }

    public static int[][] copyOf(int[][] array) {
        int[][] copy = new int[array.length][];
        for (int i = 0; i < array.length; i++) {
            copy[i] = array[i].clone();
        }
        return copy;
    }

    public static boolean[][] copyOf(boolean[][] array) {
        boolean[][] copy = new boolean[array.length][];
        for (int i = 0; i < array.length; i++) {
            copy[i] = array[i].clone();
        }
        return copy;
    }

    public static void uploadPictureToServer(@Nullable BlockEntity te, Direction facing, byte scaleFactor,
                                             int[][] picture, boolean clear) {
        if (!(te instanceof TileEntityCanvas canvas)) {
            MCPaint.LOGGER.error("Could not set paint! Found block " + (te == null ? "NONE" : te.getType()));
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Could not set paint!"), true);
            return;
        }
        if (clear) {
            PacketDistributor.sendToServer(new ClearSidePayload(te.getBlockPos(), facing));
            canvas.removePaint(facing);
        } else {
            IPaintable paintable = canvas.getPaintFor(facing);
            paintable.setData(scaleFactor, picture, canvas, facing);

            // ★ 使用新的 PaintDataPayload 发送
            PaintDataPayload.createAndSend(
                    te.getBlockPos(),
                    facing,
                    scaleFactor,
                    paintable.getPalette(),
                    picture,
                    PacketDistributor::sendToServer   // Consumer<PaintDataPayload>
            );
        }
    }

//    @Nonnull
//    public static ServerPlayer checkServer(NetworkEvent.Context context) {
//        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER)
//            throw new IllegalArgumentException("Wrong side for server packet handler " + context.getDirection());
//        context.setPacketHandled(true);
//        return Objects.requireNonNull(context.getSender());
//    }
//
//    public static void checkClient(NetworkEvent.Context context) {
//        if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT)
//            throw new IllegalArgumentException("Wrong side for client packet handler: " + context.getDirection());
//        context.setPacketHandled(true);

    public static Int2ByteMap buildReversePalette(int[] palette) {
        Int2ByteMap map = new Int2ByteOpenHashMap();
        for (int i = 0; i < palette.length; i++) {
            map.put(palette[i], (byte) i);
        }
        return map;
    }
}
