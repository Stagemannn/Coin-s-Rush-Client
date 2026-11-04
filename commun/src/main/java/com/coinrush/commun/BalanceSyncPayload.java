package com.coinrush.commun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur vers client : le solde du joueur, en unités. */
public record BalanceSyncPayload(long units) implements CustomPacketPayload {
	public static final Type<BalanceSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "balance"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BalanceSyncPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_LONG, BalanceSyncPayload::units, BalanceSyncPayload::new);

	@Override
	public Type<BalanceSyncPayload> type() {
		return TYPE;
	}
}
