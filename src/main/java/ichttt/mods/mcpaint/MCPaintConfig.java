package ichttt.mods.mcpaint;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class MCPaintConfig {
    static final ModConfigSpec clientSpec;
    public static final Client CLIENT;
    static {
        final Pair<Client, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Client::new);
        clientSpec = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    @SuppressWarnings("CanBeFinal")
    public static class Client {
        Client(ModConfigSpec.Builder builder) {
            builder.comment("Client-only settings").push("Client");

            directApplyStamp = builder
                    .comment("True if stamps should set the picture directly instead of opening the GUI")
                    .translation("mcpaint.config.directapplystamp")
                    .define("directApplyStamp", false);

            optimizePictures = builder
                    .comment("True to allow MCPaint to optimize picture draw calls in the background to improve performance in the long run")
                    .translation("mcpaint.config.optimizepictures")
                    .define("optimizePictures", true);

            maxPaintRenderDistance = builder
                    .comment("Defines how far away the paint on the block should be rendered at max")
                    .translation("mcpaint.config.maxpaintrenderdistance")
                    .defineInRange("maxPaintRenderDistance", 128, 64, 256);

            enableMipMaps = builder
                    .comment("If enabled, mipmaps will be used for far away blocks. Can improve speed and image stability, but also could make images more blurry on farther distance or cause micro lags. Somewhat experimental")
                    .translation("mcpaint.config.enablemipmaps")
                    .define("enableMipMaps", false);

            maxPaintBrightness = builder
                    .comment("Defines the maximum brightness that a picture can have. Helps to reduce oversaturation")
                    .translation("mcpaint.config.maxpaintbrightness")
                    .defineInRange("maxPaintBrightness", 220, 180, 240);

            maxMipSize = builder
                    .comment("The factor how many rects the mip is allowed to have so it is allowed to be used. Saves some memory when performance is not better than no-mip version and provides clearer images, but makes image less stable")
                    .translation("mcpaint.config.maxmipsize")
                    .worldRestart()
                    .defineInRange("maxMipSize", 0.8D, 0D, 1D);

            maxTotalColorDiffPerMip = builder
                    .comment("How much all color channels can differ so they are merged as one channel in a mip. Value multiplied by mip level. Higher values improve performance, but reduce color clarity")
                    .translation("mcpaint.config.totalcolordiffpermap")
                    .worldRestart()
                    .defineInRange("maxTotalColorDiffPerMip", 6, 0, 50);

            maxSingleColorDiffPerMip = builder
                    .comment("How much all color channels can differ so they are merged as one channel in a mip. Value multiplied by mip level. Higher values improve performance, but reduce color clarity")
                    .translation("mcpaint.config.maxsinglecolordiffpermip")
                    .worldRestart()
                    .defineInRange("maxSingleColorDiffPerMip", 4, 0, 20);

            builder.pop();
        }

        public final ModConfigSpec.BooleanValue directApplyStamp;
        public final ModConfigSpec.BooleanValue optimizePictures;
        public final ModConfigSpec.IntValue maxPaintRenderDistance;
        public final ModConfigSpec.BooleanValue enableMipMaps;
        public final ModConfigSpec.IntValue maxPaintBrightness;
        public final ModConfigSpec.DoubleValue maxMipSize;
        public final ModConfigSpec.IntValue maxTotalColorDiffPerMip;
        public final ModConfigSpec.IntValue maxSingleColorDiffPerMip;
    }
}
