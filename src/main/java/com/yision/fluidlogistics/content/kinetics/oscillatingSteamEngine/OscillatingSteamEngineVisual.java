package com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine.OscillatingSteamEngineRenderState.Part;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

public class OscillatingSteamEngineVisual extends AbstractBlockEntityVisual<OscillatingSteamEngineBlockEntity>
	implements SimpleDynamicVisual {
	private final Map<Part, TransformedInstance> instances = new EnumMap<>(Part.class);
	private final PoseStack pose = new PoseStack();

	public OscillatingSteamEngineVisual(VisualizationContext context, OscillatingSteamEngineBlockEntity be,
		float partialTick) {
		super(context, be, partialTick);
		for (Part part : Part.values())
			instances.put(part, instancerProvider().instancer(InstanceTypes.TRANSFORMED,
				Models.partial(part.model())).createInstance());
		animate();
	}

	@Override
	public void beginFrame(Context context) {
		animate();
	}

	@Override
	public void update(float partialTick) {
		animate();
	}

	private void animate() {
		OscillatingSteamEngineRenderState state = OscillatingSteamEngineRenderState.of(blockEntity);
		for (var entry : instances.entrySet()) {
			Part part = entry.getKey();
			TransformedInstance instance = entry.getValue();
			instance.handle().setVisible(state.isVisible(part));
			pose.pushPose();
			TransformStack.of(pose).translate(getVisualPosition());
			state.transform(pose, part);
			instance.setTransform(pose).setChanged();
			pose.popPose();
		}
	}

	@Override
	public void updateLight(float partialTick) {
		for (TransformedInstance instance : instances.values())
			relight(instance);
	}

	@Override
	protected void _delete() {
		instances.values().forEach(TransformedInstance::delete);
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		instances.values().forEach(consumer);
	}
}
