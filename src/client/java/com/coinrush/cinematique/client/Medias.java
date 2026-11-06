package com.coinrush.cinematique.client;

import com.coinrush.commun.CinematiqueMediaMorceauPayload;
import com.coinrush.commun.CinematiquePayload;
import com.coinrush.commun.CinematiquePayload.Media;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache des images et sons importés dans une cinématique : un fichier par empreinte SHA-256, {@code <sha>.png} ou {@code <sha>.ogg}.
 * Le serveur envoie la liste des fichiers d'une cinématique ; le client demande ceux qu'il n'a pas, les reçoit par morceaux et vérifie
 * taille, empreinte et signature avant de les garder. Sans dépendance au jeu (testable seul) ; appelé sur le thread du client.
 */
final class Medias {
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	/** Au-delà, les fichiers les moins récents sont effacés jusqu'à {@link #CIBLE}. */
	static final long MAX_CACHE = 96L << 20, CIBLE = 64L << 20;

	private final Path dossier;
	/** Fichiers attendus : empreinte → réception en cours. */
	private final Map<String, Reception> attendus = new LinkedHashMap<>();

	private static final class Reception {
		final Media media;
		final ByteArrayOutputStream tampon = new ByteArrayOutputStream();
		int suivant;

		Reception(Media media) {
			this.media = media;
		}
	}

	Medias(Path dossier) {
		this.dossier = dossier;
	}

	Path chemin(Media m) {
		return dossier.resolve(m.sha() + "." + m.ext());
	}

	Path chemin(String sha, String ext) {
		return dossier.resolve(sha + "." + ext);
	}

	/** Le fichier est en cache, entier. Le touche pour qu'il soit le dernier à partir lors du nettoyage. */
	boolean present(Media m) {
		Path p = chemin(m);
		try {
			if (!Files.isRegularFile(p) || Files.size(p) != m.taille()) return false;
			Files.setLastModifiedTime(p, FileTime.fromMillis(System.currentTimeMillis()));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Une liste reçue du serveur : renvoie les empreintes à demander (vide = tout est en cache) et prépare leur réception.
	 * @throws IllegalArgumentException liste invalide : le client ne répond pas et ne reçoit rien
	 */
	List<String> manifeste(List<Media> medias) {
		CinematiquePayload.validerMedias(medias);
		attendus.clear();
		List<String> manquants = new ArrayList<>();
		for (Media m : medias) {
			if (present(m)) continue;
			manquants.add(m.sha());
			attendus.put(m.sha(), new Reception(m));
		}
		return manquants;
	}

	/** Un morceau reçu. Tout ce qui n'est pas attendu, hors ordre ou trop gros est ignoré, et le fichier concerné abandonné. */
	void morceau(CinematiqueMediaMorceauPayload p) {
		Reception r = attendus.get(p.sha());
		if (r == null) return;
		int total = (r.media.taille() + CinematiqueMediaMorceauPayload.MORCEAU - 1) / CinematiqueMediaMorceauPayload.MORCEAU;
		byte[] d = p.donnees();
		if (d == null || p.index() != r.suivant || p.total() != total || d.length > CinematiqueMediaMorceauPayload.MORCEAU || r.tampon.size() + d.length > r.media.taille()) {
			attendus.remove(p.sha());
			LOGGER.warn("Fichier de cinématique {} abandonné : morceau {} inattendu", p.sha(), p.index());
			return;
		}
		r.tampon.writeBytes(d);
		r.suivant++;
		if (r.suivant < total) return;
		attendus.remove(p.sha());
		try {
			garder(r.media, r.tampon.toByteArray());
		} catch (IOException | IllegalArgumentException e) {
			LOGGER.warn("Fichier de cinématique {} refusé : {}", p.sha(), e.getMessage());
		}
	}

	private void garder(Media m, byte[] octets) throws IOException {
		if (octets.length != m.taille()) throw new IllegalArgumentException("taille");
		if (!sha256(octets).equals(m.sha())) throw new IllegalArgumentException("empreinte");
		if (!signature(octets, m.ext())) throw new IllegalArgumentException("signature");
		Files.createDirectories(dossier);
		Path tmp = dossier.resolve(m.sha() + ".part");
		Files.write(tmp, octets);
		Files.move(tmp, chemin(m), StandardCopyOption.REPLACE_EXISTING);
		nettoyer(chemin(m));
	}

	static String sha256(byte[] octets) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(octets));
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Premiers octets d'un PNG ({@code 89 50 4E 47 0D 0A 1A 0A}) ou d'un OGG ({@code OggS}). */
	static boolean signature(byte[] b, String ext) {
		byte[] attendu = ext.equals(CinematiquePayload.PNG) ? new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10} : new byte[] {'O', 'g', 'g', 'S'};
		if (b.length < attendu.length) return false;
		for (int i = 0; i < attendu.length; i++) if (b[i] != attendu[i]) return false;
		return true;
	}

	/** Dépasse {@link #MAX_CACHE} : efface les plus anciens, jamais {@code garde}. */
	private void nettoyer(Path garde) throws IOException {
		List<Path> fichiers;
		try (Stream<Path> s = Files.list(dossier)) {
			fichiers = s.filter(Files::isRegularFile).sorted(Comparator.comparingLong(Medias::modifie)).toList();
		}
		long total = fichiers.stream().mapToLong(Medias::taille).sum();
		if (total <= MAX_CACHE) return;
		for (Path f : fichiers) {
			if (total <= CIBLE) break;
			if (f.equals(garde)) continue;
			total -= taille(f);
			Files.deleteIfExists(f);
		}
	}

	private static long modifie(Path p) {
		try {
			return Files.getLastModifiedTime(p).toMillis();
		} catch (IOException e) {
			return 0;
		}
	}

	private static long taille(Path p) {
		try {
			return Files.size(p);
		} catch (IOException e) {
			return 0;
		}
	}
}
