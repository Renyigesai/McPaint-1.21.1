package ichttt.mods.mcpaint;

import ichttt.mods.mcpaint.client.ClientEventHandler;
import ichttt.mods.mcpaint.common.RegistryObjects;
import ichttt.mods.mcpaint.common.capability.CapabilityPaintable;
import ichttt.mods.mcpaint.common.capability.Paint;
import ichttt.mods.mcpaint.common.item.PaintDataComponent;
import ichttt.mods.mcpaint.networking.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MCPaint.MODID)
public class MCPaint {
    public static final String MODID = "mcpaint";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public MCPaint(IEventBus bus, ModContainer modContainer) {
        if (FMLEnvironment.dist.isClient()) {
            ClientEventHandler.earlySetup(bus);
        }
        RegistryObjects.registerToBus(bus);
        modContainer.registerConfig(ModConfig.Type.CLIENT, MCPaintConfig.clientSpec);
    }



    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class MCPaintBusEvent {
        @SubscribeEvent
        public static void register(final RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0");
            registrar.playToServer(PaintDataPayload.TYPE, PaintDataPayload.STREAM_CODEC, PaintDataPayload::handleServer);
            registrar.playToServer(DrawAbortPayload.TYPE, DrawAbortPayload.STREAM_CODEC, DrawAbortPayload::handleServer);
            registrar.playToServer(ClearSidePayload.TYPE, ClearSidePayload.STREAM_CODEC, ClearSidePayload::handleServer);
            registrar.playToClient(PaintDataClientPayload.TYPE, PaintDataClientPayload.STREAM_CODEC, PaintDataClientPayload::handleClient);
            registrar.playToClient(ClearSideClientPayload.TYPE, ClearSideClientPayload.STREAM_CODEC, ClearSideClientPayload::handleClient);
        }

        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(
                    CapabilityPaintable.PAINTABLE_BLOCK,
                    RegistryObjects.CANVAS_BE.get(),
                    (blockEntity, side) -> blockEntity.getPaintable()
            );

            event.registerItem(
                    CapabilityPaintable.PAINTABLE_ITEM,
                    (stack, ctx) -> {
                        PaintDataComponent dataComponent = (PaintDataComponent) stack.get(RegistryObjects.PAINT_DATA.get());

                        Paint paint = new Paint();
                        if (dataComponent != null) {
                            paint.setDataWithPalette(
                                    dataComponent.scaleFactor(),
                                    dataComponent.pictureData(),
                                    dataComponent.palette(),
                                    null, null
                            );
                        }
                        System.out.println("Providing Paint for stack: " + stack + " -> " + paint);
                        return paint;
                    },
                    RegistryObjects.BRUSH.get(),RegistryObjects.STAMP.get()
            );
        }
    }

}
