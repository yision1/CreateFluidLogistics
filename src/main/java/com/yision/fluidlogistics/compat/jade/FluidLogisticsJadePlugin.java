package com.yision.fluidlogistics.compat.jade;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.yision.fluidlogistics.content.equipment.mechanicalFluidGun.MechanicalFluidGunBlockEntity;
import com.yision.fluidlogistics.content.fluids.faucet.FaucetBlockEntity;
import com.yision.fluidlogistics.content.fluids.fluidInventoryAccessPort.FluidInventoryAccessPortBlockEntity;
import com.yision.fluidlogistics.content.fluids.horizontalMultiFluidTank.HorizontalMultiFluidTankBlockEntity;
import com.yision.fluidlogistics.content.fluids.multiFluidAccessPort.MultiFluidAccessPortBlockEntity;
import com.yision.fluidlogistics.content.fluids.multiFluidTank.MultiFluidTankBlockEntity;
import com.yision.fluidlogistics.content.logistics.copperBasket.CopperBasketBlockEntity;
import com.yision.fluidlogistics.content.processing.snowGolemFreezer.SnowGolemFreezerBlock;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class FluidLogisticsJadePlugin implements IWailaPlugin {

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerFluidStorage(MultiFluidTankProvider.INSTANCE, MultiFluidTankBlockEntity.class);
		registration.registerFluidStorage(MultiFluidTankProvider.INSTANCE, HorizontalMultiFluidTankBlockEntity.class);
		registration.registerFluidStorage(ContraptionMultiFluidTankProvider.INSTANCE, AbstractContraptionEntity.class);
		registration.registerBlockDataProvider(ConnectedFluidSourceProvider.INSTANCE, CopperBasketBlockEntity.class);
		registration.registerBlockDataProvider(ConnectedFluidSourceProvider.INSTANCE,
			MechanicalFluidGunBlockEntity.class);
		registration.registerBlockDataProvider(ConnectedFluidSourceProvider.INSTANCE, FaucetBlockEntity.class);
		registration.registerBlockDataProvider(ConnectedFluidSourceProvider.INSTANCE,
			MultiFluidAccessPortBlockEntity.class);
		registration.registerBlockDataProvider(ConnectedFluidSourceProvider.INSTANCE,
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
