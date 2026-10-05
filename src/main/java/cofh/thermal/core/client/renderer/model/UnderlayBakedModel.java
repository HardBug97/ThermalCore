package cofh.thermal.core.client.renderer.model;

import cofh.core.client.renderer.model.ModelUtils;
import cofh.core.client.renderer.model.ModelUtils.FluidCacheWrapper;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.RenderHelper;
import cofh.lib.client.renderer.block.model.RetexturedBakedQuad;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collections;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UnderlayBakedModel extends BakedModelWrapper<BakedModel> implements IDynamicBakedModel {

    private static final Map<FluidCacheWrapper, BakedQuad[]> FLUID_QUAD_CACHE = Collections.synchronizedMap(new Object2ObjectOpenHashMap<>());
    private static final Map<BlockState, BakedQuad[]> UNDERLAY_QUAD_CACHE = Collections.synchronizedMap(new IdentityHashMap<>());

    public static void clearCache() {

        FLUID_QUAD_CACHE.clear();
        UNDERLAY_QUAD_CACHE.clear();
    }

    protected int underlayQuadLevel = 0;

    public UnderlayBakedModel(BakedModel originalModel) {

        super(originalModel);
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable RenderType renderType) {

        return addUnderlayQuads(new LinkedList<>(originalModel.getQuads(state, side, rand, extraData, renderType)), state, side, rand, extraData, renderType);
    }

    // region HELPERS
    protected List<BakedQuad> addUnderlayQuads(LinkedList<BakedQuad> quads, @Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable RenderType renderType) {

        if (side == null || quads.isEmpty()) {
            return quads;
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
                    cachedFluidQuads[sideIndex] = new RetexturedBakedQuad(RenderHelper.mulColor(baseQuad, FluidHelper.color(fluid)), RenderHelper.getFluidTexture(fluid));
                    FLUID_QUAD_CACHE.put(wrapper, cachedFluidQuads);
                }
                quads.offerFirst(cachedFluidQuads[sideIndex]);
            }
        } else if (extraData.has(ModelUtils.UNDERLAY)) {
            ResourceLocation loc = extraData.get(ModelUtils.UNDERLAY);
            BakedQuad[] cachedUnderlayQuads = UNDERLAY_QUAD_CACHE.get(state);
            if (cachedUnderlayQuads == null || cachedUnderlayQuads.length < 6) {
                cachedUnderlayQuads = new BakedQuad[6];
            }
            if (cachedUnderlayQuads[sideIndex] == null) {
                cachedUnderlayQuads[sideIndex] = new RetexturedBakedQuad(baseQuad, RenderHelper.getTexture(loc));
                UNDERLAY_QUAD_CACHE.put(state, cachedUnderlayQuads);
            }
            quads.offerFirst(cachedUnderlayQuads[sideIndex]);
        }
        return quads;
    }
    // endregion
}
