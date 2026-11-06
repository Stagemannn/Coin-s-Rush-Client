package com.coinrush.commun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur vers client : place du joueur au classement solo, sur {@code joueurs} classés. {@code place} 0 : la ligne disparaît. */
public record ClassementPayload(int place, int joueurs) implements CustomPacketPayload {
	public static final Type<ClassementPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "classement"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ClassementPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, ClassementPayload::place, ByteBufCodecs.VAR_INT, ClassementPayload::joueurs, ClassementPayload::new);

	public void validate() {
		if (joueurs < 0 || joueurs > 100_000 || place < 0 || place > joueurs) throw new IllegalArgumentException("classement invalide");
	}

	@Override
	public Type<ClassementPayload> type() {
		return TYPE;
	}
}
