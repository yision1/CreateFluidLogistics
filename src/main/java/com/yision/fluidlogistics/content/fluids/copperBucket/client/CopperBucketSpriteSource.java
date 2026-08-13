package com.yision.fluidlogistics.content.fluids.copperBucket.client;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CopperBucketSpriteSource implements SpriteSource {

    private static final int BUCKET_COLOR_TOLERANCE = 18;

    private static final CopperBucketSpriteSource INSTANCE = new CopperBucketSpriteSource();
    private static final Codec<CopperBucketSpriteSource> CODEC = Codec.unit(INSTANCE);
    private static final SpriteSourceType TYPE = SpriteSources.register(
            FluidLogistics.asResource("copper_bucket").toString(), CODEC);

    private static final ResourceLocation EMPTY_BUCKET_SPRITE = ResourceLocation.withDefaultNamespace("item/bucket");
    private static final ResourceLocation COPPER_BUCKET_SPRITE = FluidLogistics.asResource("item/copper_bucket");

    private CopperBucketSpriteSource() {
    }

    public static void register() {
    }

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        Optional<Resource> emptyBucket = getTexture(resourceManager, EMPTY_BUCKET_SPRITE);
        Optional<Resource> copperBucket = getTexture(resourceManager, COPPER_BUCKET_SPRITE);
        if (emptyBucket.isEmpty() || copperBucket.isEmpty()) {
            FluidLogistics.LOGGER.warn("Cannot generate copper bucket sprites: a base bucket texture is missing");
            return;
        }

        Set<Item> seenBuckets = Collections.newSetFromMap(new IdentityHashMap<>());
        BuiltInRegistries.FLUID.forEach(fluid -> {
            Item bucket = fluid.getBucket();
            if (bucket == Items.AIR || bucket == AllItems.COPPER_BUCKET.get() || !seenBuckets.add(bucket)) {
                return;
            }

            ResourceLocation bucketId = BuiltInRegistries.ITEM.getKey(bucket);
            ItemStack bucketStack = new ItemStack(bucket);
            layerSprites(resourceManager, bucketId).forEach((tintIndex, sourceSprite) -> {
                getTexture(resourceManager, sourceSprite).ifPresent(sourceTexture -> {
                    int tintColor = Minecraft.getInstance().getItemColors().getColor(bucketStack, tintIndex);
                    ResourceLocation generatedSprite = generatedSprite(bucketId, tintIndex);
                    output.add(generatedSprite, () -> compose(generatedSprite, sourceSprite,
                            sourceTexture, emptyBucket.get(), copperBucket.get(), tintColor));
                });
            });
        });
    }

    @Nullable
    private static SpriteContents compose(ResourceLocation generatedSprite,
            ResourceLocation sourceSprite, Resource sourceTexture, Resource emptyBucketTexture,
            Resource copperBucketTexture, int tintColor) {
        SpriteContents source = SpriteLoader.loadSprite(sourceSprite, sourceTexture);
        if (source == null) {
            return null;
        }

        try (source;
                NativeImage emptyBucket = NativeImage.read(emptyBucketTexture.open());
                NativeImage copperBucket = NativeImage.read(copperBucketTexture.open())) {
            if (source.width() != emptyBucket.getWidth() || source.height() != emptyBucket.getHeight()
                    || source.width() != copperBucket.getWidth() || source.height() != copperBucket.getHeight()) {
                FluidLogistics.LOGGER.debug("Skipping copper bucket sprite {} because its frame is not {}x{}",
                        sourceSprite, emptyBucket.getWidth(), emptyBucket.getHeight());
                return null;
            }

            AnimationMetadataSection metadata = sourceTexture.metadata()
                    .getSection(AnimationMetadataSection.SERIALIZER)
                    .orElse(AnimationMetadataSection.EMPTY);
            NativeImage sourceImage = source.getOriginalImage();
            NativeImage result = new NativeImage(sourceImage.getWidth(), sourceImage.getHeight(), false);
            try {
                Set<Integer> emptyBucketColors = new HashSet<>();
                for (int y = 0; y < emptyBucket.getHeight(); y++) {
                    for (int x = 0; x < emptyBucket.getWidth(); x++) {
                        emptyBucketColors.add(emptyBucket.getPixelRGBA(x, y));
                    }
                }

                for (int y = 0; y < sourceImage.getHeight(); y++) {
                    for (int x = 0; x < sourceImage.getWidth(); x++) {
                        int frameX = x % source.width();
                        int frameY = y % source.height();
                        int sourcePixel = applyTint(sourceImage.getPixelRGBA(x, y), tintColor);
                        boolean protectedFluidPixel = isProtectedFluidPixel(frameX, frameY);
                        int resultPixel = !protectedFluidPixel && matchesBucketColor(sourcePixel, emptyBucketColors)
                                ? copperBucket.getPixelRGBA(frameX, frameY)
                                : sourcePixel;
                        result.setPixelRGBA(x, y, resultPixel);
                    }
                }

                return new SpriteContents(generatedSprite, new FrameSize(source.width(), source.height()),
                        result, metadata, source.forgeMeta);
            } catch (RuntimeException exception) {
                result.close();
                throw exception;
            }
        } catch (IOException exception) {
            FluidLogistics.LOGGER.warn("Unable to generate copper bucket sprite from {}", sourceSprite, exception);
            return null;
        }
    }

    private static boolean isProtectedFluidPixel(int x, int y) {
        return y == 3 && x >= 4 && x <= 11
                || y == 4 && x >= 3 && x <= 12
                || y == 5 && x >= 5 && x <= 10;
    }

    private static boolean matchesBucketColor(int pixel, Set<Integer> bucketColors) {
        for (int bucketColor : bucketColors) {
            if ((pixel >>> 24) != (bucketColor >>> 24)) {
                continue;
            }
            if (Math.abs((pixel & 0xFF) - (bucketColor & 0xFF)) <= BUCKET_COLOR_TOLERANCE
                    && Math.abs((pixel >>> 8 & 0xFF) - (bucketColor >>> 8 & 0xFF)) <= BUCKET_COLOR_TOLERANCE
                    && Math.abs((pixel >>> 16 & 0xFF) - (bucketColor >>> 16 & 0xFF)) <= BUCKET_COLOR_TOLERANCE) {
                return true;
            }
        }
        return false;
    }

    private static int applyTint(int pixel, int tintColor) {
        if (tintColor == -1) {
            return pixel;
        }
        int alpha = (pixel >>> 24) * (tintColor >>> 24) / 255;
        int red = (pixel & 0xFF) * (tintColor >>> 16 & 0xFF) / 255;
        int green = (pixel >>> 8 & 0xFF) * (tintColor >>> 8 & 0xFF) / 255;
        int blue = (pixel >>> 16 & 0xFF) * (tintColor & 0xFF) / 255;
        return alpha << 24 | blue << 16 | green << 8 | red;
    }

    private static Optional<Resource> getTexture(ResourceManager resourceManager, ResourceLocation sprite) {
        return resourceManager.getResource(TEXTURE_ID_CONVERTER.idToFile(sprite));
    }

    public static ResourceLocation sourceSprite(ResourceLocation bucketId) {
        return ResourceLocation.fromNamespaceAndPath(bucketId.getNamespace(), "item/" + bucketId.getPath());
    }

    public static ResourceLocation generatedSprite(ResourceLocation bucketId, int tintIndex) {
        return FluidLogistics.asResource("item/copper_bucket_generated/" + bucketId.getNamespace()
                + "/" + bucketId.getPath() + "/layer" + tintIndex);
    }

    static Map<Integer, ResourceLocation> layerSprites(ResourceManager resourceManager, ResourceLocation bucketId) {
        Map<String, String> textures = new LinkedHashMap<>();
        ResourceLocation itemModel = ResourceLocation.fromNamespaceAndPath(
                bucketId.getNamespace(), "item/" + bucketId.getPath());
        collectModelTextures(resourceManager, itemModel, textures, new HashSet<>());

        Map<Integer, ResourceLocation> sprites = new LinkedHashMap<>();
        textures.forEach((key, value) -> {
            int tintIndex = layerIndex(key);
            if (tintIndex < 0) {
                return;
            }
            resolveTexture(value, textures, new HashSet<>())
                    .ifPresent(sprite -> sprites.put(tintIndex, sprite));
        });
        sprites.putIfAbsent(0, sourceSprite(bucketId));
        return Map.copyOf(sprites);
    }

    private static void collectModelTextures(ResourceManager resourceManager, ResourceLocation modelId,
            Map<String, String> textures, Set<ResourceLocation> visitedModels) {
        if (!visitedModels.add(modelId)) {
            return;
        }

        ResourceLocation modelFile = ResourceLocation.fromNamespaceAndPath(
                modelId.getNamespace(), "models/" + modelId.getPath() + ".json");
        Optional<Resource> modelResource = resourceManager.getResource(modelFile);
        if (modelResource.isEmpty()) {
            return;
        }

        try (Reader reader = modelResource.get().openAsReader()) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            JsonElement parent = model.get("parent");
            if (parent != null && parent.isJsonPrimitive()) {
                ResourceLocation parentId = ResourceLocation.tryParse(parent.getAsString());
                if (parentId != null && !parentId.getPath().startsWith("builtin/")) {
                    collectModelTextures(resourceManager, parentId, textures, visitedModels);
                }
            }

            JsonElement modelTextures = model.get("textures");
            if (modelTextures != null && modelTextures.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : modelTextures.getAsJsonObject().entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        textures.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            FluidLogistics.LOGGER.debug("Unable to resolve bucket model textures from {}", modelFile, exception);
        }
    }

    private static Optional<ResourceLocation> resolveTexture(String value, Map<String, String> textures,
            Set<String> visitedTextures) {
        String resolved = value;
        while (resolved.startsWith("#")) {
            String key = resolved.substring(1);
            if (!visitedTextures.add(key)) {
                return Optional.empty();
            }
            resolved = textures.get(key);
            if (resolved == null) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(ResourceLocation.tryParse(resolved));
    }

    private static int layerIndex(String key) {
        if (!key.startsWith("layer") || key.length() == 5) {
            return -1;
        }
        for (int index = 5; index < key.length(); index++) {
            if (!Character.isDigit(key.charAt(index))) {
                return -1;
            }
        }
        try {
            return Integer.parseInt(key.substring(5));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }
}
