package org.mfrf.templatetransformer.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import java.util.ArrayList;
import java.util.List;

public class TemplateTransformerAltarRenderState extends BlockEntityRenderState {
    public ItemStackRenderState insertedTemplate = new ItemStackRenderState();
    public final List<ItemStackRenderState> convertibleTemplates = new ArrayList<>();
    public int selectedTemplateIndex;
    public float animationTicks;
}
