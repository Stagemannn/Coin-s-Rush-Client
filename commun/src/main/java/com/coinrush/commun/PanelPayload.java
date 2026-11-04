package com.coinrush.commun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur vers client : le panneau complet (JSON de {@link Panel}). Le client le valide et ignore ce qui est invalide. */
public record PanelPayload(String json) implements CustomPacketPayload {
	public static final Type<PanelPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "panel"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PanelPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32767), PanelPayload::json, PanelPayload::new);

	public static PanelPayload of(Panel panel) {
		return new PanelPayload(panel.toJson());
	}

	@Override
	public Type<PanelPayload> type() {
		return TYPE;
	}
}
