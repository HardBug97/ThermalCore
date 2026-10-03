package cofh.thermal.core.client.renderer.model;

import cofh.core.client.renderer.model.ModelUtils;
import cofh.core.client.renderer.model.ModelUtils.WrappedBakedModelBuilder;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.RenderHelper;
import cofh.lib.api.item.IFluidContainerItem;
import cofh.lib.util.crafting.ComparableItemStack;
import cofh.lib.util.helpers.MathHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cofh.lib.api.ContainerType.FLUID;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.core.client.ThermalTextures.*;
import static cofh.thermal.lib.util.Constants.DEFAULT_CELL_SIDES_RAW;
import static net.minecraft.core.Direction.*;

public class FluidCellBakedModel extends UnderlayBakedModel {

    private static final Map<List<Integer>, BakedQuad> FACE_QUAD_CACHE = new Object2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<BakedQuad[]> SIDE_QUAD_CACHE = new Int2ObjectOpenHashMap<>();

    private static final Int2ObjectMap<BakedQuad[]> ITEM_UNDERLAY_QUAD_CACHE = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<BakedQuad[]> ITEM_QUAD_CACHE = new Int2ObjectOpenHashMap<>();
    private static final Map<List<Integer>, BlockStateModelPart> MODEL_CACHE = new Object2ObjectOpenHashMap<>();

    public static void clearCache() {

        FACE_QUAD_CACHE.clear();
        SIDE_QUAD_CACHE.clear();

        ITEM_UNDERLAY_QUAD_CACHE.clear();
        ITEM_QUAD_CACHE.clear();
        MODEL_CACHE.clear();
    }

    public FluidCellBakedModel(BlockStateModel originalModel) {

        super(originalModel);
        underlayQuadLevel = 1;
    }

    @Override
    protected void addQuads(WrappedBakedModelBuilder builder, BlockState state, Direction side, ModelData extraData) {

        List<BakedQuad> quads = builder.getQuads(side);
        if (quads.isEmpty()) {
            return;
        }
        BakedQuad baseQuad = quads.get(0);
        int sideIndex = side.get3DDataValue();

        // FACE
        Direction face = extraData.get(ModelUtils.FACING);
        if (side == face) {
            Integer level = extraData.get(ModelUtils.LEVEL);
            if (level == null) {
                // This shouldn't happen, but playing it safe.
                return;
            }
            BakedQuad faceQuad = FACE_QUAD_CACHE.get(Arrays.asList(face.get3DDataValue(), level));
            if (faceQuad == null) {
                faceQuad = ModelUtils.retexture(quads.get(1), getLevelTexture(level));
                FACE_QUAD_CACHE.put(Arrays.asList(face.get3DDataValue(), level), faceQuad);
            }
            builder.addFaceQuad(side, faceQuad);
        }

        // SIDES
        byte[] sideConfigRaw = extraData.get(ModelUtils.SIDES);
        if (sideConfigRaw == null) {
            // This shouldn't happen, but playing it safe.
            return;
        }

        int configHash = Arrays.hashCode(sideConfigRaw);
        BakedQuad[] cachedSideQuads = SIDE_QUAD_CACHE.get(configHash);
        if (cachedSideQuads == null || cachedSideQuads.length < 6) {
            cachedSideQuads = new BakedQuad[6];
        }
        if (cachedSideQuads[sideIndex] == null) {
            cachedSideQuads[sideIndex] = ModelUtils.retexture(baseQuad, getConfigTexture(sideConfigRaw[sideIndex]));
            SIDE_QUAD_CACHE.put(configHash, cachedSideQuads);
        }
        builder.addFaceQuad(side, cachedSideQuads[sideIndex]);

        // FLUID
        super.addUnderlayQuads(builder, state, side, extraData);
    }

    public static BlockStateModelPart forItem(ItemStack stack, BlockStateModelPart model) {

        CompoundTag tag = ItemHelper.getBlockEntityData(stack);
        byte[] sideConfigRaw = getSideConfigRaw(tag);
        int itemHash = new ComparableItemStack(stack).hashCode();
        int level = getLevel(stack);
        int configHash = Arrays.hashCode(sideConfigRaw);

        FluidStack fluid = getFluid(tag);
        int fluidHash = fluid.isEmpty() ? 0 : FluidHelper.fluidHashcode(fluid);

        BlockStateModelPart ret = MODEL_CACHE.get(Arrays.asList(itemHash, level, configHash, fluidHash));
        if (ret == null) {
            WrappedBakedModelBuilder builder = new WrappedBakedModelBuilder(model);

            // FACE
            builder.addFaceQuad(NORTH, ModelUtils.retexture(builder.getQuads(NORTH).get(0), getLevelTexture(level)));

            // FLUID
            if (!fluid.isEmpty()) {
                BakedQuad[] cachedUnderlayQuads = ITEM_UNDERLAY_QUAD_CACHE.get(FluidHelper.fluidHashcode(fluid));
                if (cachedUnderlayQuads == null || cachedUnderlayQuads.length < 6) {
                    cachedUnderlayQuads = new BakedQuad[6];
                    TextureAtlasSprite fluidTexture = RenderHelper.getFluidTexture(fluid);
                    int fluidColor = FluidHelper.color(fluid);

                    cachedUnderlayQuads[0] = ModelUtils.retextureUnderlay(RenderHelper.mulColor(builder.getQuads(DOWN).get(0), fluidColor), fluidTexture);
                    cachedUnderlayQuads[1] = ModelUtils.retextureUnderlay(RenderHelper.mulColor(builder.getQuads(UP).get(0), fluidColor), fluidTexture);
                    cachedUnderlayQuads[2] = ModelUtils.retextureUnderlay(RenderHelper.mulColor(builder.getQuads(NORTH).get(0), fluidColor), fluidTexture);
                    cachedUnderlayQuads[3] = ModelUtils.retextureUnderlay(RenderHelper.mulColor(builder.getQuads(SOUTH).get(0), fluidColor), fluidTexture);
                    cachedUnderlayQuads[4] = ModelUtils.retextureUnderlay(RenderHelper.mulColor(builder.getQuads(WEST).get(0), fluidColor), fluidTexture);
                    cachedUnderlayQuads[5] = ModelUtils.retextureUnderlay(RenderHelper.mulColor(builder.getQuads(EAST).get(0), fluidColor), fluidTexture);
                    ITEM_UNDERLAY_QUAD_CACHE.put(fluidHash, cachedUnderlayQuads);
                }
                builder.addUnderlayQuad(DOWN, cachedUnderlayQuads[0]);
                builder.addUnderlayQuad(UP, cachedUnderlayQuads[1]);
                builder.addUnderlayQuad(NORTH, cachedUnderlayQuads[2]);
                builder.addUnderlayQuad(SOUTH, cachedUnderlayQuads[3]);
                builder.addUnderlayQuad(WEST, cachedUnderlayQuads[4]);
                builder.addUnderlayQuad(EAST, cachedUnderlayQuads[5]);
            }

            // SIDES
            BakedQuad[] cachedQuads = ITEM_QUAD_CACHE.get(configHash);
            if (cachedQuads == null || cachedQuads.length < 6) {
                cachedQuads = new BakedQuad[6];

                cachedQuads[0] = ModelUtils.retexture(builder.getQuads(DOWN).get(0), getConfigTexture(sideConfigRaw[0]));
                cachedQuads[1] = ModelUtils.retexture(builder.getQuads(UP).get(0), getConfigTexture(sideConfigRaw[1]));
                cachedQuads[2] = ModelUtils.retexture(builder.getQuads(NORTH).get(0), getConfigTexture(sideConfigRaw[2]));
                cachedQuads[3] = ModelUtils.retexture(builder.getQuads(SOUTH).get(0), getConfigTexture(sideConfigRaw[3]));
                cachedQuads[4] = ModelUtils.retexture(builder.getQuads(WEST).get(0), getConfigTexture(sideConfigRaw[4]));
                cachedQuads[5] = ModelUtils.retexture(builder.getQuads(EAST).get(0), getConfigTexture(sideConfigRaw[5]));
                ITEM_QUAD_CACHE.put(configHash, cachedQuads);
            }
            builder.addFaceQuad(DOWN, cachedQuads[0]);
            builder.addFaceQuad(UP, cachedQuads[1]);
            builder.addFaceQuad(NORTH, cachedQuads[2]);
            builder.addFaceQuad(SOUTH, cachedQuads[3]);
            builder.addFaceQuad(WEST, cachedQuads[4]);
            builder.addFaceQuad(EAST, cachedQuads[5]);

            ret = builder.build();
            MODEL_CACHE.put(Arrays.asList(itemHash, level, configHash, fluidHash), ret);
        }
        return ret;
    }

    // region HELPERS
    private static TextureAtlasSprite getConfigTexture(byte side) {

        switch (side) {
            case 1:
                return CELL_CONFIG_INPUT;
            case 2:
                return CELL_CONFIG_OUTPUT;
            case 3:
                return CELL_CONFIG_BOTH;
            default:
                return CELL_CONFIG_NONE;
        }
    }

    private static TextureAtlasSprite getLevelTexture(int level) {

        // Creative returned as 9 (Full) or 10 (Empty)
        if (level > 8) {
            return level >= 10 ? FLUID_CELL_LEVEL_0_C : FLUID_CELL_LEVEL_8_C;
        }
        return FLUID_CELL_LEVELS[MathHelper.clamp(level, 0, 8)];
    }

    private static FluidStack getFluid(CompoundTag tag) {

        if (tag == null) {
            return FluidStack.EMPTY;
        }
        ListTag tanks = tag.getListOrEmpty(TAG_TANK_INV);
        if (tanks.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return FluidHelper.parseOptional(ProxyUtils.registryAccess(), tanks.getCompoundOrEmpty(0));
    }

    private static byte[] getSideConfigRaw(CompoundTag tag) {

        if (tag == null) {
            return DEFAULT_CELL_SIDES_RAW;
        }
        byte[] ret = tag.getByteArray(TAG_SIDES).orElse(DEFAULT_CELL_SIDES_RAW);
        return ret.length == 0 ? DEFAULT_CELL_SIDES_RAW : ret;
    }

    private static int getLevel(ItemStack stack) {

        Item item = stack.getItem();
        if (item instanceof IFluidContainerItem fluidContainer) {
            if (fluidContainer.isCreative(stack, FLUID)) {
                return fluidContainer.getFluidAmount(stack) > 0 ? 9 : 10;
            }
            if (fluidContainer.getFluidAmount(stack) > 0) {
                return 1 + Math.min(((IFluidContainerItem) item).getScaledFluidStored(stack, 8), 7);
            }
        }
        return 0;
    }
    // endregion
}
