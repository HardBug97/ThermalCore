package cofh.thermal.core.gametest;

import cofh.thermal.core.common.block.entity.storage.EnergyCellBlockEntity;
import cofh.thermal.core.common.block.entity.storage.FluidCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import static cofh.lib.api.control.IReconfigurable.SideConfig.*;
import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.lib.util.ThermalIDs.ID_ENERGY_CELL;
import static cofh.thermal.lib.util.ThermalIDs.ID_FLUID_CELL;

public class CellTests {

    private static final BlockPos CELL = new BlockPos(3, 1, 3);

    // A side's capability follows its config, and changing the config invalidates cached lookups.
    public static void energyCellSideConfig(GameTestHelper helper) {

        helper.setBlock(CELL, BLOCKS.get(ID_ENERGY_CELL));
        EnergyCellBlockEntity cell = helper.getBlockEntity(CELL, EnergyCellBlockEntity.class);
        BlockCapabilityCache<EnergyHandler, Direction> cache = BlockCapabilityCache.create(Capabilities.Energy.BLOCK, helper.getLevel(), helper.absolutePos(CELL), Direction.UP);

        cell.reconfigControl().setSideConfig(Direction.UP, SIDE_NONE);
        helper.assertTrue(cache.getCapability() == null, "A side set to none should expose no energy handler");

        cell.reconfigControl().setSideConfig(Direction.UP, SIDE_INPUT);
        EnergyHandler input = cache.getCapability();
        helper.assertTrue(input != null, "An input side should expose an energy handler");
        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(input.insert(500, tx), 500, "energy inserted on an input side");
            helper.assertValueEqual(input.extract(100, tx), 0, "energy extracted from an input side");
            tx.commit();
        }
        try (Transaction tx = Transaction.openRoot()) {
            input.insert(300, tx);
        }
        helper.assertValueEqual(cell.getEnergyStorage().getEnergyStored(), 500, "energy after an aborted insert");

        cell.reconfigControl().setSideConfig(Direction.UP, SIDE_OUTPUT);
        EnergyHandler output = cache.getCapability();
        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(output.insert(100, tx), 0, "energy inserted on an output side");
            helper.assertValueEqual(output.extract(200, tx), 200, "energy extracted from an output side");
            tx.commit();
        }
        helper.assertValueEqual(cell.getEnergyStorage().getEnergyStored(), 300, "energy after extracting");
        helper.succeed();
    }

    public static void fluidCellSideConfig(GameTestHelper helper) {

        helper.setBlock(CELL, BLOCKS.get(ID_FLUID_CELL));
        FluidCellBlockEntity cell = helper.getBlockEntity(CELL, FluidCellBlockEntity.class);
        BlockCapabilityCache<ResourceHandler<FluidResource>, Direction> cache = BlockCapabilityCache.create(Capabilities.Fluid.BLOCK, helper.getLevel(), helper.absolutePos(CELL), Direction.UP);
        FluidResource water = FluidResource.of(Fluids.WATER);

        cell.reconfigControl().setSideConfig(Direction.UP, SIDE_NONE);
        helper.assertTrue(cache.getCapability() == null, "A side set to none should expose no fluid handler");

        cell.reconfigControl().setSideConfig(Direction.UP, SIDE_INPUT);
        ResourceHandler<FluidResource> input = cache.getCapability();
        helper.assertTrue(input != null, "An input side should expose a fluid handler");
        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(input.insert(0, water, 2000, tx), BUCKET_VOLUME, "fluid inserted on an input side (one bucket per operation)");
        }
        helper.assertValueEqual(input.getAmountAsLong(0), 0L, "fluid after an aborted insert");
        try (Transaction tx = Transaction.openRoot()) {
            input.insert(0, water, BUCKET_VOLUME, tx);
            helper.assertValueEqual(input.extract(0, water, 500, tx), 0, "fluid extracted from an input side");
            tx.commit();
        }

        cell.reconfigControl().setSideConfig(Direction.UP, SIDE_OUTPUT);
        ResourceHandler<FluidResource> output = cache.getCapability();
        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(output.insert(0, water, 500, tx), 0, "fluid inserted on an output side");
            helper.assertValueEqual(output.extract(0, water, 500, tx), 500, "fluid extracted from an output side");
            tx.commit();
        }
        helper.assertValueEqual(output.getAmountAsLong(0), 500L, "fluid after extracting");
        helper.succeed();
    }

}
