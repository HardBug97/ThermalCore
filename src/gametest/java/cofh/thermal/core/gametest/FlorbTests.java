package cofh.thermal.core.gametest;

import cofh.thermal.core.common.entity.projectile.ThrownFlorb;
import cofh.thermal.core.common.item.FlorbItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.lib.util.ThermalIDs.ID_FLORB;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

public class FlorbTests {

    public static void thrownFlorbPlacesFluid(GameTestHelper helper) {

        BlockPos ground = new BlockPos(3, 1, 3);
        helper.setBlock(ground, Blocks.STONE);

        ItemStack florb = new ItemStack(ITEMS.get(ID_FLORB));
        helper.assertValueEqual(((FlorbItem) florb.getItem()).fill(florb, new FluidStack(Fluids.WATER, BUCKET_VOLUME), EXECUTE), BUCKET_VOLUME, "fluid filled into the Florb");

        Vec3 start = helper.absoluteVec(new Vec3(3.5, 3.5, 3.5));
        ThrownFlorb thrown = new ThrownFlorb(helper.getLevel(), start.x, start.y, start.z);
        thrown.setItem(florb);
        thrown.setDeltaMovement(0, -0.5, 0);
        helper.getLevel().addFreshEntity(thrown);

        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WATER, ground.above()));
    }

}
