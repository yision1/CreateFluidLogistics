package com.yision.fluidlogistics.compat.jade;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.yision.fluidlogistics.content.equipment.mechanicalFluidGun.MechanicalFluidGunBlockEntity;
import com.yision.fluidlogistics.content.fluids.faucet.FaucetBlockEntity;
import com.yision.fluidlogistics.content.fluids.fluidPort.FluidInventoryAccessPortBlockEntity;
import com.yision.fluidlogistics.content.fluids.fluidPort.MultiFluidAccessPortBlockEntity;
import com.yision.fluidlogistics.content.logistics.copperBasket.CopperBasketBlockEntity;
import com.yision.fluidlogistics.content.processing.snowGolemFreezer.SnowGolemFreezerBlock;

import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class FluidLogisticsJadePlugin implements IWailaPlugin {
	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerFluidStorage(MultiFluidTankProvider.INSTANCE, Block.class);
		registration.registerFluidStorage(ContraptionMultiFluidTankProvider.INSTANCE, AbstractContraptionEntity.class);
		registration.registerFluidStorage(ConnectedFluidSourceProvider.INSTANCE, MechanicalFluidGunBlockEntity.class);
		registration.registerFluidStorage(ConnectedFluidSourceProvider.INSTANCE, FaucetBlockEntity.class);
		registration.registerFluidStorage(ConnectedFluidSourceProvider.INSTANCE, CopperBasketBlockEntity.class);
		registration.registerFluidStorage(ConnectedFluidSourceProvider.INSTANCE, MultiFluidAccessPortBlockEntity.class);
		registration.registerFluidStorage(ConnectedFluidSourceProvider.INSTANCE,
			FluidInventoryAccessPortBlockEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerFluidStorageClient(MultiFluidTankProvider.INSTANCE);
		registration.registerFluidStorageClient(ContraptionMultiFluidTankProvider.INSTANCE);
		registration.registerFluidStorageClient(ConnectedFluidSourceProvider.INSTANCE);
		registration.registerBlockComponent(SnowGolemFreezerIconProvider.INSTANCE, SnowGolemFreezerBlock.class);
		registration.usePickedResult(AllBlocks.FACTORY_GAUGE.get());
	}
}
