package com.yision.fluidlogistics.content.logistics.packageResource;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.event.BlockEntityBehaviourEvent;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.yision.fluidlogistics.FluidLogistics;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = FluidLogistics.MODID)
public final class ResourceRepackagerAttachment {
	private ResourceRepackagerAttachment() {
	}

	@SubscribeEvent
	public static void onAttachBehaviours(BlockEntityBehaviourEvent event) {
		event.forType(AllBlockEntityTypes.REPACKAGER.get(), blockEntity -> {
			if (blockEntity.repackageHelper != null
				&& blockEntity.repackageHelper.getClass() == PackageRepackageHelper.class) {
				blockEntity.repackageHelper = new ResourceRepackageHelper();
			}
		});
	}
}
