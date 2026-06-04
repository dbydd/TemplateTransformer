package org.mfrf.templatetransformer.client;

import net.neoforged.bus.api.IEventBus;

public final class TemplateTransformerClientBootstrap {
    private TemplateTransformerClientBootstrap() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(TemplateTransformerClientBootstrap::registerRenderers);
    }

    private static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                org.mfrf.templatetransformer.Templatetransformer.BETEMPLATE_TRANSFORMER.get(),
                TemplateTransformerAltarRenderer::new
        );
    }
}
