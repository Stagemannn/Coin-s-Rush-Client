package com.coinrush.cinematique.client;

import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import net.minecraft.client.sounds.JOrbisAudioStream;
import org.lwjgl.openal.AL10;

/**
 * Un son OGG importé joué par OpenAL, hors du moteur de sons du jeu (qui ne connaît que les sons de ses packs de ressources). Décodé avec le
 * décodeur du jeu, joué sans position (identique dans les deux oreilles). Réservé à l'étape de lecture : toute erreur remonte à l'appelant,
 * qui ignore ce son sans arrêter la cinématique.
 */
final class SonImporte {
	private final int source, tampon;

	private SonImporte(int source, int tampon) {
		this.source = source;
		this.tampon = tampon;
	}

	/** @param volume gain final (déjà multiplié par le volume général du jeu) */
	static SonImporte jouer(Path fichier, float volume, float hauteur) throws IOException {
		ByteBuffer pcm;
		javax.sound.sampled.AudioFormat format;
		try (InputStream in = Files.newInputStream(fichier); JOrbisAudioStream flux = new JOrbisAudioStream(in)) {
			pcm = flux.readAll();
			format = flux.getFormat();
		}
		OptionalInt buffer = new SoundBuffer(pcm, format).releaseAlBuffer();
		if (buffer.isEmpty()) throw new IOException("tampon OpenAL indisponible");
		int source = AL10.alGenSources();
		AL10.alSourcei(source, AL10.AL_BUFFER, buffer.getAsInt());
		AL10.alSourcef(source, AL10.AL_GAIN, Math.max(0f, Math.min(1f, volume)));
		AL10.alSourcef(source, AL10.AL_PITCH, hauteur);
		AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
		AL10.alSource3f(source, AL10.AL_POSITION, 0, 0, 0);
		AL10.alSourcePlay(source);
		if (AL10.alGetError() != AL10.AL_NO_ERROR) {
			AL10.alDeleteSources(source);
			AL10.alDeleteBuffers(buffer.getAsInt());
			throw new IOException("OpenAL a refusé le son");
		}
		return new SonImporte(source, buffer.getAsInt());
	}

	boolean fini() {
		return AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING;
	}

	void arreter() {
		AL10.alSourceStop(source);
		AL10.alDeleteSources(source);
		AL10.alDeleteBuffers(tampon);
	}
}
