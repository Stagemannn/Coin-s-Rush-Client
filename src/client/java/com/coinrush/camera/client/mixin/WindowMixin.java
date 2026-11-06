package com.coinrush.camera.client.mixin;

import com.coinrush.cinematique.client.LecteurClient;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Pendant une cinématique : échelle de l'interface 2 (le jeu la réduit si la fenêtre est trop petite). L'option du joueur n'est pas touchée. */
@Mixin(Window.class)
public abstract class WindowMixin {
	@ModifyVariable(method = "calculateScale", at = @At("HEAD"), argsOnly = true)
	private int coinrush$echelleCinematique(int echelle) {
		return LecteurClient.actif() ? 2 : echelle;
	}
}
