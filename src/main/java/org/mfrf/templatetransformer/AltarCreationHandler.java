package org.mfrf.templatetransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class AltarCreationHandler {
    private AltarCreationHandler() {
    }

    public static boolean tryCreateAltar(ServerLevel level, BlockPos pos, ItemEntity templateEntity) {
        BlockState targetState = level.getBlockState(pos);
        if (!targetState.is(Blocks.SMITHING_TABLE)) {
            return false;
        }

        if (templateEntity == null || !templateEntity.isAlive() || !Util.isValidUpgradePattern(level, templateEntity.getItem())) {
            return false;
        }

        BlockState altarState = Templatetransformer.TEMPLATE_TRANSFORMER_ALTAR.get().defaultBlockState();
        if (altarState.hasProperty(HorizontalDirectionalBlock.FACING) && targetState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            altarState = altarState.setValue(HorizontalDirectionalBlock.FACING, targetState.getValue(HorizontalDirectionalBlock.FACING));
        }

        level.setBlock(pos, altarState, 3);
        templateEntity.discard();
        level.levelEvent(1029, pos, 0);
        return true;
    }
}
