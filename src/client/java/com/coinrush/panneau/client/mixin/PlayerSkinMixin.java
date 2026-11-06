package com.coinrush.panneau.client.mixin;

import com.coinrush.panneau.client.CamoClient;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skin d'un joueur sous /camo : celui de Mr le Directeur, pour le corps, le tab et la 3e personne. Le vrai profil n'est pas touché. */
@Mixin(PlayerInfo.class)
abstract class PlayerSkinMixin {
	@Shadow
	public abstract com.mojang.authlib.GameProfile getProfile();

	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void coinrush$skinDuDirecteur(CallbackInfoReturnable<PlayerSkin> cir) {
		if (!CamoClient.camo(getProfile().getId())) return;
		PlayerSkin vrai = cir.getReturnValue();
		// Garde la cape et l'élytre du joueur : seul le corps prend le skin du Directeur.
		cir.setReturnValue(new PlayerSkin(CamoClient.SKIN.texture(), null, vrai.capeTexture(), vrai.elytraTexture(), CamoClient.SKIN.model(), vrai.secure()));
	}
}
