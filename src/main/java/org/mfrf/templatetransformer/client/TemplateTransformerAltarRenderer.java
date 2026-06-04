package org.mfrf.templatetransformer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.mfrf.templatetransformer.BETemplateTransformerAltar;
import org.mfrf.templatetransformer.Util;

public class TemplateTransformerAltarRenderer implements BlockEntityRenderer<BETemplateTransformerAltar, TemplateTransformerAltarRenderState> {
    private static final float INSERTED_TEMPLATE_Y = 1.0F;
    private static final float RING_Y = 2.0F;
    private static final float RING_RADIUS = 2.0F;
    private static final float FLOAT_AMPLITUDE = 0.12F;
    private static final float RING_ITEM_SCALE = 0.5F;
    private static final float INSERTED_ITEM_SCALE = 0.72F;
    private static final float ITEM_BOB_OFFSET = 0.1F;

    private final ItemModelResolver itemModelResolver;

    public TemplateTransformerAltarRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public TemplateTransformerAltarRenderState createRenderState() {
        return new TemplateTransformerAltarRenderState();
    }

    @Override
    public void extractRenderState(
            BETemplateTransformerAltar blockEntity,
            TemplateTransformerAltarRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, crumblingOverlay);
        state.animationTicks = blockEntity.getRenderAnimationTicks(partialTick);
        state.selectedTemplateIndex = blockEntity.getSelectedTemplateIndex();

        state.insertedTemplate = new ItemStackRenderState();
        ItemStack insertedTemplate = blockEntity.getItem(0);
        this.itemModelResolver.updateForTopItem(
                state.insertedTemplate,
                insertedTemplate,
                ItemDisplayContext.FIXED,
                blockEntity.getLevel(),
                null,
                (int) blockEntity.getBlockPos().asLong()
        );

        state.convertibleTemplates.clear();
        int seed = (int) blockEntity.getBlockPos().asLong();
        java.util.List<Identifier> templateIds = Util.getRegisteredSmithingTemplateIds();
        for (int i = 0; i < templateIds.size(); i++) {
            ItemStackRenderState templateState = new ItemStackRenderState();
            Item templateItem = BuiltInRegistries.ITEM.getValue(templateIds.get(i));
            if (templateItem instanceof SmithingTemplateItem) {
                this.itemModelResolver.updateForTopItem(
                        templateState,
                        new ItemStack(templateItem),
                        ItemDisplayContext.FIXED,
                        blockEntity.getLevel(),
                        null,
                        seed + 31 + i
                );
            }
            state.convertibleTemplates.add(templateState);
        }
    }

    @Override
    public void submit(TemplateTransformerAltarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        submitInsertedTemplate(state, poseStack, submitNodeCollector);
        submitConvertibleTemplateRing(state, poseStack, submitNodeCollector);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    private static void submitInsertedTemplate(TemplateTransformerAltarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        if (state.insertedTemplate.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, INSERTED_TEMPLATE_Y, 0.5F);
        applyItemEntityLikeTransform(state.insertedTemplate, poseStack, state.animationTicks, 0.0F, INSERTED_ITEM_SCALE);
        state.insertedTemplate.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void submitConvertibleTemplateRing(TemplateTransformerAltarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        int templateCount = state.convertibleTemplates.size();
        if (templateCount == 0) {
            return;
        }

        float ringRotation = state.animationTicks * 2.0F;
        for (int i = 0; i < templateCount; i++) {
            ItemStackRenderState templateState = state.convertibleTemplates.get(i);
            if (templateState.isEmpty()) {
                continue;
            }

            float angle = ringRotation + 360.0F * i / templateCount;
            float angleRadians = angle * Mth.DEG_TO_RAD;
            float x = Mth.cos(angleRadians) * RING_RADIUS;
            float z = Mth.sin(angleRadians) * RING_RADIUS;
            float scale = i == state.selectedTemplateIndex ? RING_ITEM_SCALE * 1.25F : RING_ITEM_SCALE;

            poseStack.pushPose();
            poseStack.translate(0.5F + x, RING_Y, 0.5F + z);
            applyItemEntityLikeTransform(templateState, poseStack, state.animationTicks, i * 0.45F, scale);
            templateState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private static void applyItemEntityLikeTransform(ItemStackRenderState itemState, PoseStack poseStack, float animationTicks, float bobOffset, float scale) {
        net.minecraft.world.phys.AABB boundingBox = itemState.getModelBoundingBox();
        float minOffsetY = -((float) boundingBox.minY) + 0.0625F;
        float bob = Mth.sin(animationTicks / 10.0F + bobOffset) * FLOAT_AMPLITUDE + ITEM_BOB_OFFSET;
        float spin = ItemEntity.getSpin(animationTicks, bobOffset);
        poseStack.translate(0.0F, minOffsetY + bob, 0.0F);
        poseStack.mulPose(Axis.YP.rotation(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(12.5F));
        poseStack.scale(scale, scale, scale);
    }
}
