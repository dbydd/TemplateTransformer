package org.mfrf.templatetransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

public class BETemplateTransformerAltar extends BlockEntity implements Nameable, Container {
    private static final int SLOT_TEMPLATE = 0;
    private static final int CONVERSION_DURATION_TICKS = 200;
    private static final int TARGET_SWITCH_DURATION_TICKS = 20;

    private final NonNullList<ItemStack> templateInHere = NonNullList.withSize(1, ItemStack.EMPTY);
    private int selectedTemplateIndex = 0;
    private int conversionTicksRemaining = 0;
    private int pendingSelectionStep = 0;
    private int selectionSwitchTicksRemaining = 0;

    public BETemplateTransformerAltar(BlockPos pos, BlockState state) {
        super(Templatetransformer.BETEMPLATE_TRANSFORMER.get(), pos,state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, templateInHere);
        output.putInt("selected_template_index", selectedTemplateIndex);
        output.putInt("conversion_ticks_remaining", conversionTicksRemaining);
        output.putInt("pending_selection_step", pendingSelectionStep);
        output.putInt("selection_switch_ticks_remaining", selectionSwitchTicksRemaining);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, templateInHere);
        selectedTemplateIndex = input.getIntOr("selected_template_index", 0);
        conversionTicksRemaining = input.getIntOr("conversion_ticks_remaining", 0);
        pendingSelectionStep = input.getIntOr("pending_selection_step", 0);
        selectionSwitchTicksRemaining = input.getIntOr("selection_switch_ticks_remaining", 0);
        normalizeSelectedTemplateIndex();
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return templateInHere.get(SLOT_TEMPLATE).isEmpty();
    }

    @Override
    public ItemStack getItem(int i) {
        if (i != SLOT_TEMPLATE) {
            return ItemStack.EMPTY;
        }

        return templateInHere.get(i);
    }

    @Override
    public ItemStack removeItem(int i, int i1) {
        if (i != SLOT_TEMPLATE || isConverting()) {
            return ItemStack.EMPTY;
        }

        ItemStack split = ContainerHelper.removeItem(templateInHere, i, i1);
        if (!split.isEmpty()) {
            setChanged();
            sync();
        }
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        if (i != SLOT_TEMPLATE || isConverting()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = ContainerHelper.takeItem(templateInHere, i);
        if (!removed.isEmpty()) {
            setChanged();
            sync();
        }
        return removed;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        if (i != SLOT_TEMPLATE) {
            return;
        }

        templateInHere.set(i, itemStack.copyWithCount(Math.min(itemStack.getCount(), getMaxStackSize())));
        setChanged();
        sync();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this,player);
    }

    @Override
    public void clearContent() {
        if (isConverting()) {
            return;
        }

        templateInHere.set(SLOT_TEMPLATE, ItemStack.EMPTY);
        this.setChanged();
        sync();
    }

    @Override
    public Component getName() {
        return Component.translatable("container.mfrf.templatetransformer.template_transformer_altar");
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_TEMPLATE && stack.getItem() instanceof SmithingTemplateItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        super.onDataPacket(net, input);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!isConverting()) {
            super.preRemoveSideEffects(pos, state);
        }
    }

    public boolean isConverting() {
        return conversionTicksRemaining > 0;
    }

    public boolean isSwitchingTarget() {
        return selectionSwitchTicksRemaining > 0;
    }

    public int getConversionTicksRemaining() {
        return conversionTicksRemaining;
    }

    public int getPendingSelectionStep() {
        return pendingSelectionStep;
    }

    public float getConversionProgress(float partialTick) {
        if (!isConverting()) {
            return 0.0F;
        }

        return 1.0F - Mth.clamp((conversionTicksRemaining - partialTick) / (float) CONVERSION_DURATION_TICKS, 0.0F, 1.0F);
    }

    public float getSelectionSwitchProgress(float partialTick) {
        if (!isSwitchingTarget()) {
            return 0.0F;
        }

        return 1.0F - Mth.clamp((selectionSwitchTicksRemaining - partialTick) / (float) TARGET_SWITCH_DURATION_TICKS, 0.0F, 1.0F);
    }

    public Identifier getSelectedTemplateId() {
        List<Identifier> ids = Util.getRegisteredSmithingTemplateIds();
        if (ids.isEmpty()) {
            return null;
        }

        normalizeSelectedTemplateIndex();
        return ids.get(selectedTemplateIndex);
    }

    public int getSelectedTemplateIndex() {
        normalizeSelectedTemplateIndex();
        return selectedTemplateIndex;
    }

    public float getRenderAnimationTicks(float partialTick) {
        if (level == null) {
            return partialTick;
        }

        return level.getGameTime() + partialTick;
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, BETemplateTransformerAltar altar) {
        if (level.isClientSide()) {
            return;
        }

        boolean changed = false;

        if (altar.selectionSwitchTicksRemaining > 0) {
            altar.selectionSwitchTicksRemaining--;
            changed = true;
            if (altar.selectionSwitchTicksRemaining <= 0) {
                altar.finishTargetSwitch();
            }
        }

        if (!altar.isConverting()) {
            if (changed) {
                altar.setChanged();
                altar.sync();
            }
            return;
        }

        altar.conversionTicksRemaining--;
        if (altar.conversionTicksRemaining <= 0) {
            altar.finishConversion();
        }

        altar.setChanged();
        altar.sync();
    }

    public void cycleSelection(int step) {
        List<Identifier> ids = Util.getRegisteredSmithingTemplateIds();
        if (ids.isEmpty() || isConverting() || isSwitchingTarget()) {
            return;
        }

        pendingSelectionStep = step < 0 ? -1 : 1;
        selectionSwitchTicksRemaining = TARGET_SWITCH_DURATION_TICKS;
        setChanged();
        sync();
    }

    public void startConversion() {
        if (isConverting() || isSwitchingTarget() || isEmpty()) {
            return;
        }

        ItemStack stack = templateInHere.get(SLOT_TEMPLATE);
        if (!(stack.getItem() instanceof SmithingTemplateItem)) {
            return;
        }

        Identifier targetId = getSelectedTemplateId();
        if (targetId == null) {
            return;
        }

        if (targetId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return;
        }

        conversionTicksRemaining = CONVERSION_DURATION_TICKS;
        setChanged();
        sync();
    }

    private void finishConversion() {
        ItemStack current = templateInHere.get(SLOT_TEMPLATE);
        Identifier targetId = getSelectedTemplateId();
        if (current.isEmpty() || targetId == null) {
            conversionTicksRemaining = 0;
            return;
        }

        net.minecraft.world.item.Item targetItem = BuiltInRegistries.ITEM.getValue(targetId);
        if (!(targetItem instanceof SmithingTemplateItem)) {
            conversionTicksRemaining = 0;
            return;
        }

        templateInHere.set(SLOT_TEMPLATE, new ItemStack(targetItem, current.getCount()));
        conversionTicksRemaining = 0;
    }

    private void finishTargetSwitch() {
        List<Identifier> ids = Util.getRegisteredSmithingTemplateIds();
        if (!ids.isEmpty() && pendingSelectionStep != 0) {
            normalizeSelectedTemplateIndex();
            selectedTemplateIndex = Math.floorMod(selectedTemplateIndex + pendingSelectionStep, ids.size());
        }

        pendingSelectionStep = 0;
        selectionSwitchTicksRemaining = 0;
    }

    private void normalizeSelectedTemplateIndex() {
        List<Identifier> ids = Util.getRegisteredSmithingTemplateIds();
        if (ids.isEmpty()) {
            selectedTemplateIndex = 0;
            return;
        }

        selectedTemplateIndex = Math.floorMod(selectedTemplateIndex, ids.size());
    }

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
