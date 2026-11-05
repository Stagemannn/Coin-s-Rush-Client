package com.coinrush.carte.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lecture du panel joueurs du serveur (lecture seule) avec le jeton personnel reçu à la connexion : position de soi et des
 * coéquipiers chaque seconde, profil toutes les 3 s, carte toutes les 5 s, tuiles à la demande. Tout se passe hors du thread
 * de rendu ; l'écran lit les champs publics. Le serveur filtre : aucune autre position que la nôtre et celle de l'équipe n'arrive jamais.
 */
final class CarteDonnees implements AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger("coinrush");

	record Boite(String type, String nom, String prix, int x1, int z1, int x2, int z2) { }

	record Point(Component nom, int couleur, double x, double z) { }

	record Equipier(UUID id, String nom, double x, double z) { }

	record Profil(String pseudo, String coins, Map<String, Long> stats) { }

	/** Tuile reçue : octets PNG pas encore transformés en texture (le thread de rendu s'en charge). */
	static final class Tuile {
		volatile byte[] png;
		volatile String etag;
		volatile boolean occupee;
		volatile long reessai;
		/** Réservé au thread de rendu. */
		Object texture;
	}

	volatile double[] moi;
	volatile List<Equipier> equipe = List.of();
	volatile List<Boite> boites = List.of();
	volatile List<Point> points = List.of(), pnj = List.of();
	volatile Profil profil;
	volatile String erreur;
	final Map<String, Tuile> tuiles = new ConcurrentHashMap<>();

	private final String base, cookie;
	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(3, r -> {
		Thread t = new Thread(r, "coinrush-carte");
		t.setDaemon(true);
		return t;
	});

	/** @param url adresse reçue du serveur, par exemple {@code http://hote:8766/?t=jeton} */
	CarteDonnees(String url) {
		URI u = URI.create(url);
		base = u.getScheme() + "://" + u.getRawAuthority();
		String t = null;
		for (String p : String.valueOf(u.getRawQuery()).split("&")) if (p.startsWith("t=")) t = p.substring(2);
		cookie = "crpj=" + t;
		exec.scheduleWithFixedDelay(() -> tente(this::position), 0, 1, TimeUnit.SECONDS);
		exec.scheduleWithFixedDelay(() -> tente(this::profil), 0, 3, TimeUnit.SECONDS);
		exec.scheduleWithFixedDelay(() -> tente(this::carte), 0, 5, TimeUnit.SECONDS);
	}

	@Override
	public void close() {
		exec.shutdownNow();
	}

	private interface Tache {
		void run() throws Exception;
	}

	private void tente(Tache t) {
		try {
			t.run();
			erreur = null;
		} catch (Exception e) {
			erreur = e.getMessage() != null ? e.getMessage() : e.toString();
			LOG.debug("Carte : {}", erreur);
		}
	}

	private HttpRequest.Builder requete(String chemin) {
		return HttpRequest.newBuilder(URI.create(base + chemin)).timeout(Duration.ofSeconds(8)).header("Cookie", cookie).GET();
	}

	private JsonObject json(String chemin) throws IOException, InterruptedException {
		HttpResponse<String> r = http.send(requete(chemin).build(), HttpResponse.BodyHandlers.ofString());
		JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
		if (r.statusCode() != 200) throw new IOException(o.has("erreur") ? o.get("erreur").getAsString() : "HTTP " + r.statusCode());
		return o;
	}

	private void position() throws Exception {
		JsonObject o = json("/api/position");
		JsonElement m = o.get("moi");
		moi = m == null || m.isJsonNull() ? null : new double[] {m.getAsJsonObject().get("x").getAsDouble(), m.getAsJsonObject().get("z").getAsDouble()};
		List<Equipier> l = new ArrayList<>();
		for (JsonElement e : o.getAsJsonArray("equipe")) {
			JsonObject j = e.getAsJsonObject();
			l.add(new Equipier(UUID.fromString(j.get("id").getAsString()), j.get("nom").getAsString(), j.get("x").getAsDouble(), j.get("z").getAsDouble()));
		}
		equipe = l;
	}

	private void profil() throws Exception {
		JsonObject o = json("/api/profil");
		Map<String, Long> st = new java.util.LinkedHashMap<>();
		for (var e : o.getAsJsonObject("stats").entrySet()) st.put(e.getKey(), e.getValue().getAsLong());
		profil = new Profil(o.get("pseudo").getAsString(), o.get("coins").getAsString(), st);
	}

	private void carte() throws Exception {
		JsonObject o = json("/api/carte");
		List<Boite> b = new ArrayList<>();
		for (JsonElement e : o.getAsJsonArray("zones")) {
			JsonObject j = e.getAsJsonObject();
			b.add(new Boite(j.get("type").getAsString(), j.get("nom").getAsString(), j.has("prix") && !j.get("prix").isJsonNull() ? j.get("prix").getAsString() : null,
					j.get("x1").getAsInt(), j.get("z1").getAsInt(), j.get("x2").getAsInt(), j.get("z2").getAsInt()));
		}
		boites = b;
		points = points(o.getAsJsonArray("points"));
		pnj = points(o.getAsJsonArray("pnj"));
	}

	private static List<Point> points(JsonArray a) {
		List<Point> l = new ArrayList<>();
		for (JsonElement e : a) {
			JsonObject j = e.getAsJsonObject();
			int couleur = j.has("couleur") ? hex(j.get("couleur").getAsString()) : 0xFFEE55;
			l.add(new Point(composant(j.getAsJsonArray("nom")), couleur, j.get("x").getAsDouble(), j.get("z").getAsDouble()));
		}
		return l;
	}

	static int hex(String c) {
		try {
			return Integer.parseInt(c.startsWith("#") ? c.substring(1) : c, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return 0xFFFFFF;
		}
	}

	/** Morceaux {t, c, b, i, u, s} du serveur en texte du jeu : du texte brut coloré, jamais interprété. */
	static Component composant(JsonArray segs) {
		MutableComponent racine = Component.empty();
		for (JsonElement e : segs) {
			JsonObject s = e.getAsJsonObject();
			Style st = Style.EMPTY;
			if (s.has("c") && !s.get("c").isJsonNull()) st = st.withColor(hex(s.get("c").getAsString()));
			st = st.withBold(s.get("b").getAsBoolean()).withItalic(s.get("i").getAsBoolean())
					.withUnderlined(s.get("u").getAsBoolean()).withStrikethrough(s.get("s").getAsBoolean());
			racine.append(Component.literal(s.get("t").getAsString()).withStyle(st));
		}
		return racine;
	}

	/** Demande une tuile (ou sa revalidation, si on en a déjà une) sans bloquer l'appelant. */
	void tuile(int k, int x, int y, boolean revalider) {
		String cle = k + "/" + x + "/" + y;
		Tuile t = tuiles.computeIfAbsent(cle, c -> new Tuile());
		if (t.occupee || System.currentTimeMillis() < t.reessai || (t.texture != null || t.png != null) && !revalider) return;
		t.occupee = true;
		exec.execute(() -> {
			try {
				HttpRequest.Builder b = requete("/tuile/" + cle + ".png");
				if (t.etag != null) b.header("If-None-Match", t.etag);
				HttpResponse<byte[]> r = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
				if (r.statusCode() == 200) {
					t.etag = r.headers().firstValue("ETag").orElse(null);
					t.png = r.body();
				} else if (r.statusCode() == 202) {
					t.reessai = System.currentTimeMillis() + 1200; // en cours de dessin côté serveur
				} else if (r.statusCode() != 304) {
					t.reessai = System.currentTimeMillis() + 5000;
				}
			} catch (Exception e) {
				t.reessai = System.currentTimeMillis() + 5000;
			} finally {
				t.occupee = false;
			}
		});
	}
}
