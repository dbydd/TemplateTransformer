package org.mfrf.templatetransformer.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = "templatetransformer", dist = net.neoforged.api.distmarker.Dist.CLIENT)
public class TemplateTransformerClientMod {
    public TemplateTransformerClientMod(IEventBus modEventBus, ModContainer modContainer) {
        TemplateTransformerClientBootstrap.init(modEventBus);
    }
}
