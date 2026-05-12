package org.mfrf.templatetransformer.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.mfrf.templatetransformer.AltarCreationHandler;
import org.mfrf.templatetransformer.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AnvilBlock.class)
public abstract class FallingBlockEntityMixin {
    private static final double REQUIRED_FALL_DISTANCE = 128.0D;

    @Inject(method = "onBrokenAfterFall", at = @At(value = "RETURN"))
    private void tryCreateAltar(Level level, BlockPos pos, FallingBlockEntity entity, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }


        BlockPos targetPos = pos.below();
        BlockState targetState = serverLevel.getBlockState(targetPos);
        if (!targetState.is(Blocks.SMITHING_TABLE)) {
            return;
        }

        List<ItemEntity> templates = level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos),
                itemEntity -> Util.isValidUpgradePattern(serverLevel, itemEntity.getItem())
        );

        if (templates.isEmpty()) {
            return;
        }

        if (AltarCreationHandler.tryCreateAltar(serverLevel, targetPos, templates.getFirst())) {
            entity.discard();
        }
    }
}
