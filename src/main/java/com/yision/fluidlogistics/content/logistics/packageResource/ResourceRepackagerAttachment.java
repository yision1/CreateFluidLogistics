package com.yision.fluidlogistics.content.logistics.packageResource;

import com.simibubi.create.api.event.BlockEntityBehaviourEvent;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;

public final class ResourceRepackagerAttachment {
	private ResourceRepackagerAttachment() {
	}

	public static void onAttachBehaviours(BlockEntityBehaviourEvent<RepackagerBlockEntity> event) {
		RepackagerBlockEntity blockEntity = event.getBlockEntity();
		if (blockEntity.repackageHelper != null
			&& blockEntity.repackageHelper.getClass() == PackageRepackageHelper.class) {
			blockEntity.repackageHelper = new ResourceRepackageHelper();
		}
	}
}
