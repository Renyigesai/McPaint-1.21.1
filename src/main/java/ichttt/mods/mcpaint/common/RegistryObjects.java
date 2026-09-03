package ichttt.mods.mcpaint.common;

import ichttt.mods.mcpaint.MCPaint;
import ichttt.mods.mcpaint.common.block.BlockCanvas;
import ichttt.mods.mcpaint.common.block.TileEntityCanvas;
import ichttt.mods.mcpaint.common.item.ItemBrush;
import ichttt.mods.mcpaint.common.item.ItemStamp;
import ichttt.mods.mcpaint.common.item.PaintDataComponent;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RegistryObjects {
    private static final DeferredRegister.Items ITEM_REGISTER = DeferredRegister.createItems(MCPaint.MODID);
    private static final DeferredRegister.Blocks BLOCK_REGISTER = DeferredRegister.createBlocks(MCPaint.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPE_REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MCPaint.MODID);

    public static final DeferredRegister.DataComponents COMPONENTS_REGISTER = DeferredRegister.createDataComponents(MCPaint.MODID);

    public static final DeferredRegister<CreativeModeTab> TAB_REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MCPaint.MODID);

    public static final DeferredItem<Item> BRUSH;
    public static final DeferredItem<Item> STAMP;

    public static final DeferredBlock<BlockCanvas> CANVAS_BLOCK;
    
    public static final Supplier<BlockEntityType<TileEntityCanvas>> CANVAS_BE;

    public static final Supplier<DataComponentType<PaintDataComponent>> PAINT_DATA;

    public static final Supplier<CreativeModeTab> MCP_TAB;

    static {
        // ITEMS
        BRUSH = ITEM_REGISTER.register("brush", ItemBrush::new);
        STAMP = ITEM_REGISTER.register("stamp", ItemStamp::new);

        // BLOCKS
        CANVAS_BLOCK = BLOCK_REGISTER.register("canvas", BlockCanvas::new);


        // Block Entity Types
        CANVAS_BE = BLOCK_ENTITY_TYPE_REGISTER.register("canvas_te", () -> BlockEntityType.Builder.of(TileEntityCanvas::new, CANVAS_BLOCK.get()).build(null));
        PAINT_DATA = COMPONENTS_REGISTER.register("paint_data", () -> DataComponentType.<PaintDataComponent>builder().persistent( PaintDataComponent.CODEC ).networkSynchronized(PaintDataComponent.STREAM_CODEC).build());

        MCP_TAB = TAB_REGISTER.register("mcp_tab",()-> CreativeModeTab.builder().icon(()-> BRUSH.get().getDefaultInstance()).title(Component.translatable("mcpaint.gui.tab"))
                .displayItems((itemDisplayParameters, output) -> {
                    output.accept(BRUSH.get());
                    output.accept(STAMP.get());
                })
                .build());
    }

    public static void registerToBus(IEventBus bus) {
        ITEM_REGISTER.register(bus);
        BLOCK_REGISTER.register(bus);
        BLOCK_ENTITY_TYPE_REGISTER.register(bus);
        TAB_REGISTER.register(bus);
        COMPONENTS_REGISTER.register(bus);
    }
}
