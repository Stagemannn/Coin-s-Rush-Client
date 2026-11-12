package com.coinrush.inventaire.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Profil du joueur (pseudo, Coins, stats) lu sur le panel joueurs. Pas de fil permanent : {@link #actuel()}, appelé à chaque image
 * par l'inventaire, relance une lecture en arrière-plan quand la dernière a plus de 3 secondes.
 */
public final class ProfilClient {
	public record Profil(String pseudo, String coins, Map<String, Long> stats) { }

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "coinrush-profil");
		t.setDaemon(true);
		return t;
	});

	private static volatile String base, cookie;
	private static volatile Profil profil;
	private static volatile long dernier;
	private static volatile boolean occupe;

	private ProfilClient() { }

	/** Adresse reçue du serveur à la connexion ({@code http://hote:8766/?t=jeton}) ; null à la déconnexion. */
	static void configurer(String url) {
		profil = null;
		dernier = 0;
		if (url == null) {
			base = null;
			cookie = null;
			return;
		}
		URI u = URI.create(url);
		String t = null;
		for (String p : String.valueOf(u.getRawQuery()).split("&")) if (p.startsWith("t=")) t = p.substring(2);
		cookie = "crpj=" + t;
		base = u.getScheme() + "://" + u.getRawAuthority();
	}

	/** Dernier profil lu, ou null s'il n'est pas encore arrivé (ou si le panel est indisponible). */
	public static Profil actuel() {
		String b = base;
		if (b != null && !occupe && System.currentTimeMillis() - dernier > 3000) {
			occupe = true;
			dernier = System.currentTimeMillis();
			String c = cookie;
			EXEC.execute(() -> {
				try {
					HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(b + "/api/profil")).timeout(Duration.ofSeconds(8)).header("Cookie", c).GET().build(),
							HttpResponse.BodyHandlers.ofString());
					if (r.statusCode() == 200) {
						JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
						Map<String, Long> st = new LinkedHashMap<>();
						for (var e : o.getAsJsonObject("stats").entrySet()) st.put(e.getKey(), e.getValue().getAsLong());
						if (b.equals(base)) profil = new Profil(o.get("pseudo").getAsString(), o.get("coins").getAsString(), st);
					}
				} catch (Exception e) {
					// panel injoignable : on garde le dernier profil, nouvel essai dans 3 s
				} finally {
					occupe = false;
				}
			});
		}
		return profil;
	}
}
