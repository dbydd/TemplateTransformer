package org.mfrf.templatetransformer.client;

/**
 * Client bootstrap is wired from the main mod constructor via a dist guard.
 * Keeping this class non-annotated avoids registering a second @Mod entrypoint
 * with the same mod id on the client.
 */
public final class TemplateTransformerClientMod {
    private TemplateTransformerClientMod() {
    }
}
