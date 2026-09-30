package com.yision.fluidlogistics.content.fluids.pressureGauge;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PipeFlowMeasurement {
    private PipeFlowMeasurement() {}

    public static void update(PressureGaugeBlockEntity gauge) {
        Level level = gauge.getLevel();
        if (gauge.flowWatch != null && !gauge.flowWatch.ready(level.getGameTime()))
            return;
        FluidTransportBehaviour transport = gauge.getBehaviour(FluidTransportBehaviour.TYPE);
        if (transport.phase != FluidTransportBehaviour.UpdatePhase.IDLE) {
            FlowMeasurementUpdates.attach(gauge, FlowMeasurementUpdates.watch(level, List.of(gauge.getBlockPos())));
            return;
        }
        FluidStack fluid = getThroughFluid(transport);
        if (fluid.isEmpty()) {
            FlowMeasurementUpdates.attach(gauge, FlowMeasurementUpdates.watch(level, List.of(gauge.getBlockPos())));
            gauge.setTheoreticalFlowRate(0);
            return;
        }
        Map<BlockPos, Node> graph = readGraph(level, gauge.getBlockPos(), transport, fluid);
        if (graph == null)
            return;
        graph.forEach((pos, node) -> {
            for (BlockPos output : node.outputs)
                graph.get(output).inputs.add(pos);
        });
        graph.values().forEach(node -> node.inputs.sort(BlockPos::compareTo));
        Map<BlockPos, List<BlockPos>> routes = new HashMap<>();
        Map<BlockPos, Integer> outletCounts = new HashMap<>();
        for (Map.Entry<BlockPos, Node> entry : graph.entrySet()) {
            int outlets = entry.getValue().outlets;
            if (outlets == 0)
                continue;
            List<BlockPos> order = orderTowardTarget(graph, entry.getKey());
            routes.put(entry.getKey(), order);
            for (BlockPos pos : order)
                if (graph.get(pos).sourceRate > 0)
                    outletCounts.merge(pos, outlets, Integer::sum);
        }
        Map<BlockPos, Double> measured = new HashMap<>();
        routes.forEach((target, order) -> distribute(graph, order, target, outletCounts, measured));
        watchGraph(level, graph);
        graph.forEach((pos, node) -> {
            if (node.transport.blockEntity instanceof PressureGaugeBlockEntity meter)
                meter.setTheoreticalFlowRate(getThroughFluid(node.transport).isEmpty()
                    ? 0 : measured.getOrDefault(pos, 0d));
        });
    }

    private static FluidStack getThroughFluid(FluidTransportBehaviour transport) {
        FluidStack inbound = FluidStack.EMPTY;
        FluidStack outbound = FluidStack.EMPTY;
        for (Direction side : Iterate.directions) {
            PipeConnection.Flow flow = transport.getFlow(side);
            if (flow == null || !flow.complete)
                continue;
            if (flow.inbound)
                inbound = flow.fluid;
            else
                outbound = flow.fluid;
        }
        return !inbound.isEmpty() && inbound.isFluidEqual(outbound)
            ? inbound : FluidStack.EMPTY;
    }

    private static Map<BlockPos, Node> readGraph(Level level, BlockPos start, FluidTransportBehaviour source,
        FluidStack fluid) {
        Map<BlockPos, Node> graph = new HashMap<>();
        graph.put(start, new Node(source));
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        pending.add(start);
        while (!pending.isEmpty()) {
            BlockPos pos = pending.removeFirst();
            Node node = graph.get(pos);

            if (node.transport.phase != FluidTransportBehaviour.UpdatePhase.IDLE)
                return deferGraph(level, graph);
            for (Direction side : Iterate.directions) {
                PipeConnection.Flow flow = node.transport.getFlow(side);
                if (flow == null || !flow.fluid.isFluidEqual(fluid))
                    continue;
                if (!flow.complete)
                    return deferGraph(level, graph);
                BlockPos next = pos.relative(side);
                if (!level.isLoaded(next))
                    continue;
                if (FluidPropagator.hasFluidCapability(level, next, side.getOpposite())
                    || FluidPropagator.isOpenEnd(level, pos, side)) {
                    if (flow.inbound) {
                        float pressure = node.transport.getConnection(side).getPressure().getFirst();
                        if (pressure > 0)
                            node.sourceRate += (int) Math.max(1, pressure / 2f);
                    } else {
                        node.outlets++;
                    }
                    continue;
                }
                Node known = graph.get(next);
                FluidTransportBehaviour neighbour = known == null ? FluidPropagator.getPipe(level, next) : known.transport;
                if (neighbour != null && neighbour.phase != FluidTransportBehaviour.UpdatePhase.IDLE)
                    return deferGraph(level, graph);
                if (neighbour == null || !matches(neighbour.getFlow(side.getOpposite()), fluid, !flow.inbound))
                    continue;
                if (!flow.inbound)
                    node.outputs.add(next);
                if (known == null) {
                    graph.put(next, new Node(neighbour));
                    pending.addLast(next);
                }
            }
        }
        return graph;
    }

    private static void watchGraph(Level level, Map<BlockPos, Node> graph) {
        FlowMeasurementUpdates.Watch watch = FlowMeasurementUpdates.watch(level, graph.keySet());
        for (Node node : graph.values())
            if (node.transport.blockEntity instanceof PressureGaugeBlockEntity meter)
                FlowMeasurementUpdates.attach(meter, watch);
    }

    private static Map<BlockPos, Node> deferGraph(Level level, Map<BlockPos, Node> graph) {
        watchGraph(level, graph);
        return null;
    }

    private static boolean matches(PipeConnection.Flow flow, FluidStack fluid, boolean inbound) {
        return flow != null && flow.complete && flow.inbound == inbound
            && flow.fluid.isFluidEqual(fluid);
    }

    private static List<BlockPos> orderTowardTarget(Map<BlockPos, Node> graph, BlockPos target) {
        List<BlockPos> order = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> path = new ArrayDeque<>();
        ArrayDeque<Iterator<BlockPos>> edges = new ArrayDeque<>();
        path.push(target);
        edges.push(graph.get(target).inputs.iterator());
        visited.add(target);
        while (!path.isEmpty()) {
            Iterator<BlockPos> iterator = edges.peek();
            if (!iterator.hasNext()) {
                order.add(path.pop());
                edges.pop();
                continue;
            }
            BlockPos next = iterator.next();
            if (!visited.add(next))
                continue;
            path.push(next);
            edges.push(graph.get(next).inputs.iterator());
        }
        return order;
    }

    private static void distribute(Map<BlockPos, Node> graph, List<BlockPos> order, BlockPos target,
        Map<BlockPos, Integer> outletCounts, Map<BlockPos, Double> measured) {
        Map<BlockPos, Integer> indices = new HashMap<>();
        for (int i = 0; i < order.size(); i++)
            indices.put(order.get(i), i);
        Map<BlockPos, Double> amounts = new HashMap<>();
        for (BlockPos pos : order) {
            double passing = amounts.getOrDefault(pos, 0d);
            Node node = graph.get(pos);
            if (node.sourceRate > 0)
                passing += node.sourceRate * graph.get(target).outlets / outletCounts.get(pos);
            if (passing == 0)
                continue;
            measured.merge(pos, passing, Double::sum);
            if (pos.equals(target))
                continue;

            int index = indices.get(pos);
            int branches = 0;
            for (BlockPos output : node.outputs)
                if (indices.getOrDefault(output, -1) > index)
                    branches++;
            for (BlockPos output : node.outputs)
                if (indices.getOrDefault(output, -1) > index)
                    amounts.merge(output, passing / branches, Double::sum);
        }
    }

    private static final class Node {
        private final FluidTransportBehaviour transport;
        private final List<BlockPos> inputs = new ArrayList<>();
        private final List<BlockPos> outputs = new ArrayList<>();
        private double sourceRate;
        private int outlets;

        private Node(FluidTransportBehaviour transport) {
            this.transport = transport;
        }
    }
}
