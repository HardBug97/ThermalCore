package cofh.thermal.core.client.renderer.model;

import cofh.core.client.renderer.model.ModelUtils;
import cofh.core.client.renderer.model.ModelUtils.FluidCacheWrapper;
import cofh.core.client.renderer.model.ModelUtils.WrappedBakedModelBuilder;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.RenderHelper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static cofh.lib.util.Constants.DIRECTIONS;

public class UnderlayBakedModel extends DelegateBlockStateModel {

    private static final Map<FluidCacheWrapper, BakedQuad[]> FLUID_QUAD_CACHE = new Object2ObjectOpenHashMap<>();
    private static final IdentityHashMap<BlockState, BakedQuad[]> UNDERLAY_QUAD_CACHE = new IdentityHashMap<>();

    public static void clearCache() {

        FLUID_QUAD_CACHE.clear();
        UNDERLAY_QUAD_CACHE.clear();
    }

    protected int underlayQuadLevel = 0;

    public UnderlayBakedModel(BlockStateModel originalModel) {

        super(originalModel);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {

        List<BlockStateModelPart> originalParts = new ArrayList<>();
        delegate.collectParts(level, pos, state, random, originalParts);
        ModelData extraData = level.getModelData(pos);

        for (BlockStateModelPart part : originalParts) {
            WrappedBakedModelBuilder builder = new WrappedBakedModelBuilder(part);
            for (Direction side : DIRECTIONS) {
                addQuads(builder, state, side, extraData);
            }
            parts.add(builder.build());
        }
    }

    // region HELPERS
    protected void addQuads(WrappedBakedModelBuilder builder, BlockState state, Direction side, ModelData extraData) {

        addUnderlayQuads(builder, state, side, extraData);
    }

    protected void addUnderlayQuads(WrappedBakedModelBuilder builder, BlockState state, Direction side, ModelData extraData) {

        List<BakedQuad> quads = builder.getQuads(side);
        if (quads.isEmpty()) {
            return;
        }
        BakedQuad baseQuad = quads.get(underlayQuadLevel);
        int sideIndex = side.get3DDataValue();

        // FLUID
        if (extraData.has(ModelUtils.FLUID)) {
            FluidStack fluid = extraData.get(ModelUtils.FLUID);
            if (fluid != null && !fluid.isEmpty()) {
                FluidCacheWrapper wrapper = new FluidCacheWrapper(state, fluid);
                BakedQuad[] cachedFluidQuads = FLUID_QUAD_CACHE.get(wrapper);
                if (cachedFluidQuads == null || cachedFluidQuads.length < 6) {
                    cachedFluidQuads = new BakedQuad[6];
                }
                if (cachedFluidQuads[sideIndex] == null) {
                    cachedFluidQuads[sideIndex] = ModelUtils.retextureUnderlay(RenderHelper.mulColor(baseQuad, FluidHelper.color(fluid)), RenderHelper.getFluidTexture(fluid));
                    FLUID_QUAD_CACHE.put(wrapper, cachedFluidQuads);
                }
                builder.addUnderlayQuad(side, cachedFluidQuads[sideIndex]);
            }
        } else if (extraData.has(ModelUtils.UNDERLAY)) {
            Identifier loc = extraData.get(ModelUtils.UNDERLAY);
            BakedQuad[] cachedUnderlayQuads = UNDERLAY_QUAD_CACHE.get(state);
            if (cachedUnderlayQuads == null || cachedUnderlayQuads.length < 6) {
                cachedUnderlayQuads = new BakedQuad[6];
            }
            if (cachedUnderlayQuads[sideIndex] == null) {
                cachedUnderlayQuads[sideIndex] = ModelUtils.retextureUnderlay(baseQuad, RenderHelper.getTexture(loc));
                UNDERLAY_QUAD_CACHE.put(state, cachedUnderlayQuads);
            }
            builder.addUnderlayQuad(side, cachedUnderlayQuads[sideIndex]);
        }
    }
    // endregion
}
