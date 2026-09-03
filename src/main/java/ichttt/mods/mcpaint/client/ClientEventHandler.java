package ichttt.mods.mcpaint.client;

import ichttt.mods.mcpaint.MCPaint;
import ichttt.mods.mcpaint.client.delegators.BlockColorDelegator;
import ichttt.mods.mcpaint.client.delegators.DelegatingBakedModel;
import ichttt.mods.mcpaint.client.render.ISTERStamp;
import ichttt.mods.mcpaint.client.render.RenderTypeHandler;
import ichttt.mods.mcpaint.client.render.TERCanvas;
import ichttt.mods.mcpaint.client.render.batch.RenderCache;
import ichttt.mods.mcpaint.common.RegistryObjects;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Objects;

public class ClientEventHandler {

    public static void earlySetup(IEventBus bus) {
        bus.addListener(ClientEventHandler::onModelBake);
        bus.addListener(ClientEventHandler::registerModels);
        bus.addListener(ClientEventHandler::setupClient);
        bus.addListener(ClientEventHandler::onRegisterRenders);
        bus.addListener(ClientEventHandler::onRegisterColorHandlers);
    }

    public static void setupClient(FMLClientSetupEvent event) {
        // 强制加载 RenderTypeHandler.CANVAS，避免首次使用时产生卡顿
        event.enqueueWork(RenderTypeHandler.CANVAS::toString);
    }

    public static void registerModels(ModelEvent.RegisterGeometryLoaders event) {
//        ItemProperties.register(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(MCPaint.MODID, "stamp")), "Did not find stamp"), new ResourceLocation(MCPaint.MODID, "shift"), ISTERStamp.INSTANCE);
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            RenderCache.clear();
        }
    }

    @SubscribeEvent
    public static void onConfigChange(ModConfigEvent.Reloading event) {
        ClientHooks.onConfigReload();
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        ClientHooks.onConfigReload();
    }

    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        ResourceLocation toReplace = RegistryObjects.CANVAS_BLOCK.getId();
        String[] variants = new String[] {"normal_cube=false,solid=false", "normal_cube=true,solid=false", "normal_cube=false,solid=true", "normal_cube=true,solid=true"};
        for (String variant : variants) {
            ModelResourceLocation mrl = new ModelResourceLocation(toReplace, variant);
            BakedModel model = event.getModels().get(mrl);
            if (model == null) throw new NullPointerException("Model for " + mrl);
            model = new DelegatingBakedModel(model);
            event.getModels().put(mrl, model);
        }
    }

    public static void onRegisterRenders(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RegistryObjects.CANVAS_BE.get(), TERCanvas::new);
    }

    public static void onRegisterColorHandlers(RegisterColorHandlersEvent.Block event) {
        event.register(new BlockColorDelegator(), RegistryObjects.CANVAS_BLOCK.get());
    }
}
