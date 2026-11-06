package com.coinrush.panneau.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nom au-dessus de la tête : celui que le serveur donne au joueur dans le tab, s'il en donne un (déguisement de /disguise).
 * Sans nom de tab, rendu vanilla. Le profil du joueur (skin, UUID) n'est pas touché.
 */
@Mixin(Player.class)
abstract class PlayerNameMixin {
	@Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
	private void coinrush$nomDuTab(CallbackInfoReturnable<Component> cir) {
		Player self = (Player) (Object) this;
		if (!self.level().isClientSide()) return;
		ClientPacketListener connexion = Minecraft.getInstance().getConnection();
		PlayerInfo info = connexion == null ? null : connexion.getPlayerInfo(self.getUUID());
		if (info != null && info.getTabListDisplayName() != null) cir.setReturnValue(info.getTabListDisplayName());
	}
}
