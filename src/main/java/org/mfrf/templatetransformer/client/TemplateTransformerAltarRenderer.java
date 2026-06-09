package org.mfrf.templatetransformer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.mfrf.templatetransformer.BETemplateTransformerAltar;
import org.mfrf.templatetransformer.Templatetransformer;
import org.mfrf.templatetransformer.Util;

public class TemplateTransformerAltarRenderer implements BlockEntityRenderer<BETemplateTransformerAltar, TemplateTransformerAltarRenderState> {
    private static final Identifier TIME_WARP_TEXTURE = Identifier.fromNamespaceAndPath(Templatetransformer.MODID, "textures/effect/time_warp.png");
    private static final Identifier RIFT_TEXTURE = Identifier.fromNamespaceAndPath(Templatetransformer.MODID, "textures/effect/rift.png");
    private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath(Templatetransformer.MODID, "textures/effect/time_beam.png");

    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final float INSERTED_TEMPLATE_Y = 1.08F;
    private static final float RING_Y = 2.05F;
    private static final float RING_RADIUS = 2.0F;
    private static final float FLOAT_AMPLITUDE = 0.12F;
    private static final float RING_ITEM_SCALE = 0.5F;
    private static final float INSERTED_ITEM_SCALE = 0.72F;
    private static final float ITEM_BOB_OFFSET = 0.1F;
    private static final int CIRCLE_SEGMENTS = 48;

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
        state.pendingSelectionStep = blockEntity.getPendingSelectionStep();
        state.converting = blockEntity.isConverting();
        state.switchingTarget = blockEntity.isSwitchingTarget();
        state.conversionProgress = blockEntity.getConversionProgress(partialTick);
        state.switchProgress = blockEntity.getSelectionSwitchProgress(partialTick);
        state.seed = blockEntity.getBlockPos().asLong();

        state.insertedTemplate = new ItemStackRenderState();
        ItemStack insertedTemplate = blockEntity.getItem(0);
        this.itemModelResolver.updateForTopItem(
                state.insertedTemplate,
                insertedTemplate,
                ItemDisplayContext.FIXED,
                blockEntity.getLevel(),
                null,
                (int) state.seed
        );

        state.convertibleTemplates.clear();
        int seed = (int) state.seed;
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
        submitPortalCore(state, poseStack, submitNodeCollector);
        submitRunePulse(state, poseStack, submitNodeCollector);
        submitInsertedTemplate(state, poseStack, submitNodeCollector);
        submitConvertibleTemplateRing(state, poseStack, submitNodeCollector);
        submitConversionBurst(state, poseStack, submitNodeCollector);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    private static void submitPortalCore(TemplateTransformerAltarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        float progress = easeOutCubic(state.conversionProgress);
        float pulse = 0.5F + 0.5F * Mth.sin(state.animationTicks * 0.12F + (state.seed & 31));
        float radius = 0.42F + 0.1F * pulse + 0.52F * progress;
        float alpha = state.converting ? 0.72F + 0.22F * pulse : 0.36F + 0.12F * pulse;

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.055F + 0.1F * progress, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.animationTicks * (state.converting ? 7.5F : 2.2F)));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        submitNodeCollector.order(10).submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(RIFT_TEXTURE), (pose, consumer) ->
                drawTexturedQuad(pose, consumer, -radius, -radius, radius, radius, 255, 255, 255, floatAlphaToInt(alpha), FULL_BRIGHT)
        );
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.08F + 0.12F * progress, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.animationTicks * 4.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        float halo = radius * 1.28F;
        submitNodeCollector.order(11).submitCustomGeometry(poseStack, RenderTypes.energySwirl(TIME_WARP_TEXTURE, state.animationTicks * 0.012F, state.animationTicks * -0.008F), (pose, consumer) ->
                drawTexturedQuad(pose, consumer, -halo, -halo, halo, halo, 92, 230, 255, floatAlphaToInt(0.42F + progress * 0.35F), FULL_BRIGHT)
        );
        poseStack.popPose();
    }

    private static void submitRunePulse(TemplateTransformerAltarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        float progress = easeOutCubic(state.conversionProgress);
        float baseRadius = 0.72F + 0.22F * Mth.sin(state.animationTicks * 0.06F);
        float ringRadius = baseRadius + 0.38F * progress;
        float ringWidth = 0.045F + 0.035F * progress;
        int alpha = floatAlphaToInt(state.converting ? 0.62F : 0.34F);

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.115F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.animationTicks * 3.1F));
        submitNodeCollector.order(12).submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TIME_WARP_TEXTURE), (pose, consumer) ->
                drawAnnulus(pose, consumer, ringRadius, ringWidth, 80, 220, 255, alpha)
        );
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.13F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.animationTicks * 5.0F + state.switchProgress * state.pendingSelectionStep * 50.0F));
        submitNodeCollector.order(13).submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TIME_WARP_TEXTURE), (pose, consumer) ->
                drawDashedAnnulus(pose, consumer, ringRadius + 0.22F, ringWidth * 0.75F, 24, 182, 82, 255, floatAlphaToInt(0.3F + progress * 0.45F))
        );
        poseStack.popPose();
    }

    private static void submitInsertedTemplate(TemplateTransformerAltarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        if (state.insertedTemplate.isEmpty()) {
            return;
        }

        float progress = easeOutCubic(state.conversionProgress);
        float shake = state.converting ? Mth.sin(state.animationTicks * 2.4F) * progress * 0.035F : 0.0F;

        poseStack.pushPose();
        poseStack.translate(0.5F + shake, INSERTED_TEMPLATE_Y + 0.42F * progress, 0.5F - shake);
        applyItemEntityLikeTransform(state.insertedTemplate, poseStack, state.animationTicks * (1.0F + progress * 1.6F), 0.0F, INSERTED_ITEM_SCALE + progress * 0.16F);
        state.insertedTemplate.submit(poseStack, submitNodeCollector, state.converting ? FULL_BRIGHT : state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void submitConvertibleTemplateRing(TemplateTransformerAltarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        int templateCount = state.convertibleTemplates.size();
        if (templateCount == 0) {
            return;
        }

        float progress = easeOutCubic(state.conversionProgress);
        float switchKick = Mth.sin(state.switchProgress * Mth.PI) * state.pendingSelectionStep * 12.0F;
        float ringRotation = state.animationTicks * (2.0F + progress * 4.8F) + switchKick;
        float radius = Mth.lerp(progress, RING_RADIUS, 0.92F);
        for (int i = 0; i < templateCount; i++) {
            ItemStackRenderState templateState = state.convertibleTemplates.get(i);
            if (templateState.isEmpty()) {
                continue;
            }

            boolean selected = i == state.selectedTemplateIndex;
            float angle = ringRotation + 360.0F * i / templateCount;
            float angleRadians = angle * Mth.DEG_TO_RAD;
            float x = Mth.cos(angleRadians) * radius;
            float z = Mth.sin(angleRadians) * radius;
            float lift = selected ? 0.18F + 0.08F * Mth.sin(state.animationTicks * 0.16F) : 0.0F;
            float scale = selected ? RING_ITEM_SCALE * (1.25F + 0.18F * progress) : RING_ITEM_SCALE * (1.0F - 0.12F * progress);

            if (selected) {
                submitSelectionMarker(state, poseStack, submitNodeCollector, 0.5F + x, RING_Y - 0.18F + lift, 0.5F + z, progress);
            }

            poseStack.pushPose();
            poseStack.translate(0.5F + x, RING_Y + lift, 0.5F + z);
            applyItemEntityLikeTransform(templateState, poseStack, state.animationTicks, i * 0.45F, scale);
            templateState.submit(poseStack, submitNodeCollector, selected || state.converting ? FULL_BRIGHT : state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private static void submitSelectionMarker(TemplateTransformerAltarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, float x, float y, float z, float progress) {
        float radius = 0.28F + progress * 0.08F;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.animationTicks * 8.0F));
        submitNodeCollector.order(9).submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TIME_WARP_TEXTURE), (pose, consumer) ->
                drawAnnulus(pose, consumer, radius, 0.035F, 28, 155, 238, floatAlphaToInt(0.55F))
        );
        poseStack.popPose();
    }

    private static void submitConversionBurst(TemplateTransformerAltarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        if (!state.converting) {
            return;
        }

        float progress = easeOutCubic(state.conversionProgress);
        float flare = Mth.clamp((state.conversionProgress - 0.72F) / 0.28F, 0.0F, 1.0F);
        float height = 1.8F + 2.4F * progress;
        float radius = 0.18F + 0.3F * progress + 0.45F * flare;
        int alpha = floatAlphaToInt(0.28F + 0.42F * flare);

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.animationTicks * 9.0F));
        submitNodeCollector.order(20).submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(BEAM_TEXTURE), (pose, consumer) -> {
            drawVerticalQuad(pose, consumer, radius, height, 80, 230, 255, alpha);
            drawVerticalQuadRotated(pose, consumer, radius * 0.82F, height * 0.92F, 90.0F, 186, 86, 255, floatAlphaToInt(0.22F + 0.34F * flare));
        });
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.56F + 0.54F * progress, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.animationTicks * 12.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        float riftRadius = 0.52F + 0.82F * flare;
        submitNodeCollector.order(21).submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(RIFT_TEXTURE), (pose, consumer) ->
                drawTexturedQuad(pose, consumer, -riftRadius, -riftRadius, riftRadius, riftRadius, 255, 255, 255, floatAlphaToInt(0.25F + flare * 0.65F), FULL_BRIGHT)
        );
        poseStack.popPose();
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

    private static void drawTexturedQuad(PoseStack.Pose pose, VertexConsumer consumer, float minX, float minY, float maxX, float maxY, int red, int green, int blue, int alpha, int light) {
        vertex(pose, consumer, minX, minY, 0.0F, red, green, blue, alpha, 0.0F, 1.0F, light, 0.0F, 0.0F, 1.0F);
        vertex(pose, consumer, maxX, minY, 0.0F, red, green, blue, alpha, 1.0F, 1.0F, light, 0.0F, 0.0F, 1.0F);
        vertex(pose, consumer, maxX, maxY, 0.0F, red, green, blue, alpha, 1.0F, 0.0F, light, 0.0F, 0.0F, 1.0F);
        vertex(pose, consumer, minX, maxY, 0.0F, red, green, blue, alpha, 0.0F, 0.0F, light, 0.0F, 0.0F, 1.0F);
    }

    private static void drawAnnulus(PoseStack.Pose pose, VertexConsumer consumer, float radius, float width, int red, int green, int blue, int alpha) {
        drawAnnulusSegments(pose, consumer, radius, width, 0, CIRCLE_SEGMENTS, red, green, blue, alpha);
    }

    private static void drawDashedAnnulus(PoseStack.Pose pose, VertexConsumer consumer, float radius, float width, int dashCount, int red, int green, int blue, int alpha) {
        int segmentsPerDash = Math.max(1, CIRCLE_SEGMENTS / dashCount);
        for (int segment = 0; segment < CIRCLE_SEGMENTS; segment += segmentsPerDash * 2) {
            drawAnnulusSegments(pose, consumer, radius, width, segment, Math.min(segment + segmentsPerDash, CIRCLE_SEGMENTS), red, green, blue, alpha);
        }
    }

    private static void drawAnnulusSegments(PoseStack.Pose pose, VertexConsumer consumer, float radius, float width, int startSegment, int endSegment, int red, int green, int blue, int alpha) {
        float inner = radius - width;
        float outer = radius + width;
        for (int segment = startSegment; segment < endSegment; segment++) {
            float a0 = Mth.TWO_PI * segment / CIRCLE_SEGMENTS;
            float a1 = Mth.TWO_PI * (segment + 1) / CIRCLE_SEGMENTS;
            float i0x = Mth.cos(a0) * inner;
            float i0z = Mth.sin(a0) * inner;
            float o0x = Mth.cos(a0) * outer;
            float o0z = Mth.sin(a0) * outer;
            float i1x = Mth.cos(a1) * inner;
            float i1z = Mth.sin(a1) * inner;
            float o1x = Mth.cos(a1) * outer;
            float o1z = Mth.sin(a1) * outer;

            vertex(pose, consumer, i0x, 0.0F, i0z, red, green, blue, alpha, 0.0F, 1.0F, FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            vertex(pose, consumer, o0x, 0.0F, o0z, red, green, blue, alpha, 0.0F, 0.0F, FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            vertex(pose, consumer, o1x, 0.0F, o1z, red, green, blue, alpha, 1.0F, 0.0F, FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
            vertex(pose, consumer, i1x, 0.0F, i1z, red, green, blue, alpha, 1.0F, 1.0F, FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void drawVerticalQuad(PoseStack.Pose pose, VertexConsumer consumer, float radius, float height, int red, int green, int blue, int alpha) {
        vertex(pose, consumer, -radius, 0.0F, 0.0F, red, green, blue, alpha, 0.0F, 1.0F, FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        vertex(pose, consumer, radius, 0.0F, 0.0F, red, green, blue, alpha, 1.0F, 1.0F, FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        vertex(pose, consumer, radius, height, 0.0F, red, green, blue, alpha, 1.0F, 0.0F, FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
        vertex(pose, consumer, -radius, height, 0.0F, red, green, blue, alpha, 0.0F, 0.0F, FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
    }

    private static void drawVerticalQuadRotated(PoseStack.Pose pose, VertexConsumer consumer, float radius, float height, float degrees, int red, int green, int blue, int alpha) {
        float angle = degrees * Mth.DEG_TO_RAD;
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        vertex(pose, consumer, -radius * cos, 0.0F, -radius * sin, red, green, blue, alpha, 0.0F, 1.0F, FULL_BRIGHT, sin, 0.0F, cos);
        vertex(pose, consumer, radius * cos, 0.0F, radius * sin, red, green, blue, alpha, 1.0F, 1.0F, FULL_BRIGHT, sin, 0.0F, cos);
        vertex(pose, consumer, radius * cos, height, radius * sin, red, green, blue, alpha, 1.0F, 0.0F, FULL_BRIGHT, sin, 0.0F, cos);
        vertex(pose, consumer, -radius * cos, height, -radius * sin, red, green, blue, alpha, 0.0F, 0.0F, FULL_BRIGHT, sin, 0.0F, cos);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z, int red, int green, int blue, int alpha, float u, float v, int light, float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normalX, normalY, normalZ);
    }

    private static float easeOutCubic(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        float inverse = 1.0F - clamped;
        return 1.0F - inverse * inverse * inverse;
    }

    private static int floatAlphaToInt(float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255);
    }
}
