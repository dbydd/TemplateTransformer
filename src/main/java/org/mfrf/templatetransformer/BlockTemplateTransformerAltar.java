package org.mfrf.templatetransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockTemplateTransformerAltar extends Block implements EntityBlock
{
    private static final BooleanProperty FRONT_POWERED = BlockStateProperties.POWERED;
    private static final BooleanProperty LEFT_POWERED = BooleanProperty.create("left_powered");
    private static final BooleanProperty RIGHT_POWERED = BooleanProperty.create("right_powered");

    public BlockTemplateTransformerAltar(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(FRONT_POWERED, false)
                .setValue(LEFT_POWERED, false)
                .setValue(RIGHT_POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, FRONT_POWERED, LEFT_POWERED, RIGHT_POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BETemplateTransformerAltar(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, Templatetransformer.BETEMPLATE_TRANSFORMER.get(), BETemplateTransformerAltar::tick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BETemplateTransformerAltar altar)) {
            return;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        boolean currentFront = hasSignalFromDirection(level, pos, facing);
        boolean currentLeft = hasSignalFromDirection(level, pos, facing.getCounterClockWise());
        boolean currentRight = hasSignalFromDirection(level, pos, facing.getClockWise());

        if (currentLeft && !state.getValue(LEFT_POWERED)) {
            altar.cycleSelection(-1);
        }

        if (currentRight && !state.getValue(RIGHT_POWERED)) {
            altar.cycleSelection(1);
        }

        if (currentFront && !state.getValue(FRONT_POWERED)) {
            altar.startConversion();
        }

        BlockState updatedState = state
                .setValue(FRONT_POWERED, currentFront)
                .setValue(LEFT_POWERED, currentLeft)
                .setValue(RIGHT_POWERED, currentRight);

        if (updatedState != state) {
            level.setBlock(pos, updatedState, 2);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BETemplateTransformerAltar altar)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (altar.isConverting()) {
            return InteractionResult.CONSUME.withoutItem();
        }

        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!altar.canPlaceItem(0, stack) || !altar.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            altar.setItem(0, stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BETemplateTransformerAltar altar)) {
            return InteractionResult.PASS;
        }

        if (altar.isConverting()) {
            return InteractionResult.CONSUME.withoutItem();
        }

        if (level.isClientSide()) {
            return altar.isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS.withoutItem();
        }

        ItemStack extracted = altar.removeItemNoUpdate(0);
        if (extracted.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!player.addItem(extracted)) {
            player.drop(extracted, false);
        }

        altar.setChanged();
        return InteractionResult.SUCCESS.withoutItem();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        net.minecraft.world.Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    private static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type,
            BlockEntityType<E> checkedType,
            BlockEntityTicker<? super E> ticker
    ) {
        return checkedType == type ? (BlockEntityTicker<A>) ticker : null;
    }

    private static boolean hasSignalFromDirection(Level level, BlockPos pos, Direction direction) {
        return level.getSignal(pos.relative(direction), direction) > 0;
    }

}
