package com.coinrush.cinematique.client;

import static org.junit.jupiter.api.Assertions.*;

import com.coinrush.commun.CinematiqueMediaMorceauPayload;
import com.coinrush.commun.CinematiquePayload.Media;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MediasTest {
	@TempDir Path dir;

	private static byte[] ogg(int taille) {
		byte[] b = new byte[taille];
		Arrays.fill(b, (byte) 7);
		System.arraycopy(new byte[] {'O', 'g', 'g', 'S'}, 0, b, 0, 4);
		return b;
	}

	private static Media media(byte[] b, String ext) {
		return new Media(Medias.sha256(b), ext, b.length);
	}

	private static void envoyer(Medias m, Media media, byte[] b) {
		int morceau = CinematiqueMediaMorceauPayload.MORCEAU, total = (b.length + morceau - 1) / morceau;
		for (int i = 0; i < total; i++) m.morceau(new CinematiqueMediaMorceauPayload(media.sha(), i, total, Arrays.copyOfRange(b, i * morceau, Math.min(b.length, (i + 1) * morceau))));
	}

	@Test
	void receivesInChunksVerifiesAndCaches() throws IOException {
		Medias m = new Medias(dir.resolve("cache"));
		byte[] b = ogg(600_000); // 3 morceaux
		Media media = media(b, "ogg");
		assertEquals(List.of(media.sha()), m.manifeste(List.of(media)));
		envoyer(m, media, b);
		assertArrayEquals(b, Files.readAllBytes(m.chemin(media)));
		assertTrue(m.manifeste(List.of(media)).isEmpty()); // déjà en cache : rien à demander
	}

	@Test
	void refusesWrongContent() {
		Medias m = new Medias(dir);
		byte[] b = ogg(1000);
		Media media = new Media(Medias.sha256(b), "ogg", 1000);
		m.manifeste(List.of(media));
		byte[] autre = ogg(1000);
		autre[500] = 1; // même taille, autre empreinte
		envoyer(m, media, autre);
		assertFalse(Files.exists(m.chemin(media)));
		// un PNG annoncé mais qui n'en est pas un
		byte[] faux = ogg(100);
		Media png = media(faux, "png");
		m.manifeste(List.of(png));
		envoyer(m, png, faux);
		assertFalse(Files.exists(m.chemin(png)));
	}

	@Test
	void ignoresUnexpectedOrOutOfOrderChunks() {
		Medias m = new Medias(dir);
		byte[] b = ogg(600_000);
		Media media = media(b, "ogg");
		m.morceau(new CinematiqueMediaMorceauPayload(media.sha(), 0, 3, new byte[10])); // jamais demandé
		m.manifeste(List.of(media));
		m.morceau(new CinematiqueMediaMorceauPayload(media.sha(), 1, 3, new byte[10])); // hors ordre : abandonné
		envoyer(m, media, b); // plus attendu : ignoré
		assertFalse(Files.exists(m.chemin(media)));
		m.manifeste(List.of(media)); // le serveur redemande
		envoyer(m, media, b);
		assertTrue(Files.exists(m.chemin(media)));
	}

	@Test
	void refusesOversizedChunkAndBadManifest() {
		Medias m = new Medias(dir);
		byte[] b = ogg(1000);
		Media media = media(b, "ogg");
		m.manifeste(List.of(media));
		m.morceau(new CinematiqueMediaMorceauPayload(media.sha(), 0, 1, new byte[1001])); // plus que la taille annoncée
		assertFalse(Files.exists(m.chemin(media)));
		assertThrows(IllegalArgumentException.class, () -> m.manifeste(List.of(new Media("zz", "ogg", 10))));
		assertThrows(IllegalArgumentException.class, () -> m.manifeste(null));
	}

	@Test
	void signatures() {
		assertTrue(Medias.signature(new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0}, "png"));
		assertFalse(Medias.signature(new byte[] {'O', 'g', 'g', 'S'}, "png"));
		assertTrue(Medias.signature(ogg(10), "ogg"));
		assertFalse(Medias.signature(new byte[2], "ogg"));
	}
}
