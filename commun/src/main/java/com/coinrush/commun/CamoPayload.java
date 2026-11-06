package com.coinrush.commun;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur vers client : la liste complète des joueurs sous /camo, dessinés avec le skin de Mr le Directeur. Remplace la précédente. */
public record CamoPayload(List<UUID> joueurs) implements CustomPacketPayload {
	public static final int MAX = 64;
	public static final Type<CamoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "camo"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CamoPayload> CODEC =
			StreamCodec.composite(UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), CamoPayload::joueurs, CamoPayload::new);

	@Override
	public Type<CamoPayload> type() {
		return TYPE;
	}
}
