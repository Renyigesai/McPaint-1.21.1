package ichttt.mods.mcpaint.common.item;

import ichttt.mods.mcpaint.MCPaintConfig;
import ichttt.mods.mcpaint.client.ClientHooks;
import ichttt.mods.mcpaint.common.MCPaintUtil;
import ichttt.mods.mcpaint.common.RegistryObjects;
import ichttt.mods.mcpaint.common.block.BlockCanvas;
import ichttt.mods.mcpaint.common.block.TileEntityCanvas;
import ichttt.mods.mcpaint.common.capability.CapabilityPaintable;
import ichttt.mods.mcpaint.common.capability.IPaintable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ItemStamp extends ItemBrush {

    public ItemStamp() {}

    @Override
    protected InteractionResult processMiss(Level world, Player player, InteractionHand hand, ItemStack stack, @Nullable HitResult result) {
        if ((result == null || result.getType() == HitResult.Type.MISS) && player.getPose() == Pose.CROUCHING) {
            IPaintable paint = stack.getCapability(CapabilityPaintable.PAINTABLE_ITEM, null);
            if (paint == null || !paint.hasPaintData()){
                return InteractionResult.PASS;
            }
            paint.clear(null, null);
            // ★ 保存清空后的数据
            PaintDataComponent newComponent = PaintDataComponent.from(paint);
            stack.set(RegistryObjects.PAINT_DATA.get(), newComponent);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult processHit(Level world, Player player, InteractionHand hand, BlockPos pos, BlockState state, Direction facing) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return InteractionResult.FAIL;
        }
        IPaintable paintable = held.getCapability(CapabilityPaintable.PAINTABLE_ITEM);
        if (paintable == null) {
            return InteractionResult.FAIL;
        }

        if (paintable.hasPaintData()) {
            return super.processHit(world, player, hand, pos, state, facing);
        } else if (player.getPose() == Pose.CROUCHING) {
            Direction opposite = facing.getOpposite();
            if (state.getBlock() instanceof BlockCanvas) {
                BlockEntity te = world.getBlockEntity(pos);
                if (te instanceof TileEntityCanvas canvas) {
                    if (canvas.hasPaintFor(opposite)) {
                        paintable.copyFrom(canvas.getPaintFor(opposite), canvas, opposite);
                        PaintDataComponent newComponent = PaintDataComponent.from(paintable);
                        held.set(RegistryObjects.PAINT_DATA.get(),newComponent);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.FAIL;
    }

    @Override
    protected void startPainting(TileEntityCanvas canvas, Level world, ItemStack heldItem, BlockPos pos, Direction facing, BlockState state) {
        if (world.isClientSide) {
            IPaintable heldPaint = Objects.requireNonNull(heldItem.getCapability(CapabilityPaintable.PAINTABLE_ITEM, null));
            if (MCPaintConfig.CLIENT.directApplyStamp.get()) {
                canvas.getPaintFor(facing).copyFrom(heldPaint, canvas, facing);
                MCPaintUtil.uploadPictureToServer(canvas, facing, heldPaint.getScaleFactor(), heldPaint.getPictureData(true), false);
            } else {
                List<IPaintable> paintList = new LinkedList<>();
                if (canvas.hasPaintFor(facing)) {
                    paintList.add(canvas.getPaintFor(facing));
                }
                paintList.add(heldPaint);
                ClientHooks.showGuiDraw(paintList, pos, facing, canvas.getContainedState());
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, List<Component> tooltip, TooltipFlag flagIn) {
        IPaintable paintable = stack.getCapability(CapabilityPaintable.PAINTABLE_ITEM, null);
        if (paintable != null && paintable.hasPaintData()) {
            tooltip.add(Component.translatable("mcpaint.tooltip.stamp.paint"));
        } else {
            tooltip.add(Component.translatable("mcpaint.tooltip.stamp.nopaint"));
        }
        super.appendHoverText(stack, tooltipContext, tooltip, flagIn);
    }
}
