package com.yision.fluidlogistics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenu;
import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import com.yision.fluidlogistics.api.packager.client.PackageResourceClient;
import com.yision.fluidlogistics.content.equipment.handPointer.client.FrogportSelectionHandler;
import com.yision.fluidlogistics.content.equipment.handPointer.client.HandPointerModeManager;
import com.yision.fluidlogistics.content.equipment.handPointer.client.HandPointerInteractionHandler;
import com.yision.fluidlogistics.content.fluids.copperBucket.client.CopperBucketColor;
import com.yision.fluidlogistics.content.fluids.copperBucket.client.CopperBucketModel;
import com.yision.fluidlogistics.content.fluids.copperBucket.client.CopperBucketSpriteSource;
import com.yision.fluidlogistics.content.logistics.fluidPackage.client.FluidPackageClientRendering;
import com.yision.fluidlogistics.content.schematics.client.FluidSchematicColors;
import com.yision.fluidlogistics.content.schematics.client.FluidSchematicGuiGraphics;
import com.yision.fluidlogistics.client.event.FluidSlotClickHandler;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.AABBOutline;
import com.yision.fluidlogistics.ponder.CopperBasinPonderPlugin;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import com.yision.fluidlogistics.ponder.CopperFrogportPonderPlugin;
import com.yision.fluidlogistics.ponder.FluidFactoryGaugePonderPlugin;
import com.yision.fluidlogistics.ponder.FluidLogisticsPonderPlugin;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.AllItems;
import com.yision.fluidlogistics.registry.AllPartialModels;
import com.yision.fluidlogistics.registry.AllSpriteShifts;
import com.yision.fluidlogistics.registry.AllFluidLogisticsParticleTypes;
import com.yision.fluidlogistics.render.FluidSlotAmountRenderer;
import com.yision.fluidlogistics.render.FactoryPanelFluidPreviewRenderer;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@Mod(value = FluidLogistics.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = FluidLogistics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class FluidLogisticsClient {
    public FluidLogisticsClient(ModContainer container) {
        PackageResourceClient.registerStockKeeperAmountRenderer(
                PackageResourceTypes.FLUID, FluidSlotAmountRenderer::renderInStockKeeper);
        PackageResourceClient.registerFactoryPanelPreviewRenderer(
                PackageResourceTypes.FLUID, FactoryPanelFluidPreviewRenderer::render);
        com.yision.fluidlogistics.api.factorygauge.client.FactoryGaugeClient.registerModels(
                AllItems.FLUID_GAUGE_TYPE_ID,
                com.yision.fluidlogistics.api.factorygauge.client.FactoryGaugeModelSet.fromRoot(
                        FluidLogistics.asResource("block/fluid_factory_gauge")));
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.register(new HandPointerInteractionHandler());
        NeoForge.EVENT_BUS.register(FluidSlotClickHandler.class);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        AllSpriteShifts.register();
        PonderIndex.addPlugin(new CopperBasinPonderPlugin());
        PonderIndex.addPlugin(new FluidFactoryGaugePonderPlugin());
        PonderIndex.addPlugin(new FluidLogisticsPonderPlugin());
        PonderIndex.addPlugin(new CopperFrogportPonderPlugin());
        event.enqueueWork(() -> {
            RadialWrenchMenu
                .registerBlacklistedBlock(AllBlocks.MECHANICAL_FLUID_GUN.getId());
            FluidPackageClientRendering.registerFlywheelVisualizer();
        });
        FluidLogistics.LOGGER.info("HELLO FROM CLIENT SETUP");
        FluidLogistics.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        FluidPackageClientRendering.registerEntityRenderers(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.wrapLayer(Create.asResource("schematic"), original -> (graphics, deltaTracker) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null
                    && AllItems.FLUID_SCHEMATIC.isIn(minecraft.player.getMainHandItem())) {
                original.render(new FluidSchematicGuiGraphics(graphics), deltaTracker);
                return;
            }
            original.render(graphics, deltaTracker);
        });
    }

    @SubscribeEvent
    static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation modelLocation : AllPartialModels.customModelLocations()) {
            event.register(ModelResourceLocation.standalone(modelLocation));
        }

        AllPartialModels.register();

        for (ResourceLocation modelLocation : com.yision.fluidlogistics.api.factorygauge.client.FactoryGaugeClient
                .customModelLocations()) {
            event.register(ModelResourceLocation.standalone(modelLocation));
        }
        com.yision.fluidlogistics.api.factorygauge.client.FactoryGaugeClient.freezeModels();
    }

    @SubscribeEvent
    static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation bucketLocation = ModelResourceLocation.inventory(AllItems.COPPER_BUCKET.getId());
        var bucketModel = event.getModels().get(bucketLocation);
        if (bucketModel != null) {
            event.getModels().put(bucketLocation, new CopperBucketModel(bucketModel));
        }
    }

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new CopperBucketColor(), AllItems.COPPER_BUCKET.get());
    }

    @SubscribeEvent
    static void onRegisterSpriteSourceTypes(RegisterSpriteSourceTypesEvent event) {
        event.register(FluidLogistics.asResource("copper_bucket"), CopperBucketSpriteSource.TYPE);
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        AllFluidLogisticsParticleTypes.registerFactories(event);
    }

    @SubscribeEvent
    static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }

        event.addPackFinders(
                FluidLogistics.asResource("resourcepacks/cu_again_for_fluidlogistics"),
                PackType.CLIENT_RESOURCES,
                Component.translatable("resourcepack.fluidlogistics.cu_again_for_fluidlogistics"),
                PackSource.DEFAULT,
                false,
                Pack.Position.TOP);
    }

    @EventBusSubscriber(modid = FluidLogistics.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        static void prepareFluidSchematicOutline(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                return;
            }

            AABBOutline outline = getActiveFluidSchematicOutline();
            if (outline != null) {
                outline.getParams().colored(FluidSchematicColors.COPPER);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        static void onRenderWorld(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                return;
            }

            renderDeployedFluidSchematicOutline(event);

            if (HandPointerModeManager.getCurrentMode() != HandPointerModeManager.SelectionMode.FROGPORT) {
                return;
            }

            PoseStack ms = event.getPoseStack();
            ms.pushPose();
            SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

            FrogportSelectionHandler.tickChainTarget(Minecraft.getInstance());
            FrogportSelectionHandler.drawChainContour(ms, buffer, camera);

            buffer.draw();
            ms.popPose();
        }

        private static AABBOutline getActiveFluidSchematicOutline() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null
                    || !AllItems.FLUID_SCHEMATIC.isIn(minecraft.player.getMainHandItem())
                    || !CreateClient.SCHEMATIC_HANDLER.isActive()) {
                return null;
            }
            return CreateClient.SCHEMATIC_HANDLER.getOutline();
        }

        private static void renderDeployedFluidSchematicOutline(RenderLevelStageEvent event) {
            AABBOutline outline = getActiveFluidSchematicOutline();
            if (outline == null || !CreateClient.SCHEMATIC_HANDLER.isDeployed()) {
                return;
            }

            PoseStack ms = event.getPoseStack();
            ms.pushPose();
            SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

            CreateClient.SCHEMATIC_HANDLER.getTransformation().applyTransformations(ms, camera);
            outline.getParams()
                .clearTextures()
                .colored(FluidSchematicColors.COPPER)
                .lineWidth(1 / 16f);
            outline.render(ms, buffer, Vec3.ZERO, AnimationTickHolder.getPartialTicks());

            buffer.draw();
            ms.popPose();
        }
    }
}
