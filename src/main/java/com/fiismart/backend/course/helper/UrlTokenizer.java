package com.fiismart.backend.course.helper;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UrlTokenizer – sanitizează și tokenizează URL-uri pentru lecturi.
 *
 * Tokenizarea înseamnă că URL-ul raw (ex: YouTube watch?v=..., Vimeo, S3)
 * este normalizat într-un format intern sigur și consistent.
 *
 * Suportă:
 *  - YouTube  → embed URL curat
 *  - Vimeo    → embed URL curat
 *  - URL-uri directe (S3, CDN, etc.) → validate și returnate ca atare
 *  - Generare token unic opțional (pentru stocare internă)
 */
@Component
public class UrlTokenizer {

    /**
     * Normalizează un videoUrl.
     *
     * Regula:
     *  - null/blank → null
     *  - pare URL (http/https) → normalizează YouTube/Vimeo, validează altfel
     *  - orice altceva (markdown inline, text) → pass-through. Câmpul `videoUrl`
     *    este folosit de FE și pentru conținut markdown de lecție, nu doar
     *    pentru URL-uri media.
     */
    public String tokenizeVideoUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return null;

        String trimmed = rawUrl.trim();

        if (!looksLikeUrl(trimmed)) {
            // Conținut inline (ex: markdown) — stochează ca atare.
            return trimmed;
        }

        // YouTube
        String youtubeId = extractYoutubeId(trimmed);
        if (youtubeId != null) {
            return "https://www.youtube.com/embed/" + youtubeId;
        }

        // Vimeo
        String vimeoId = extractVimeoId(trimmed);
        if (vimeoId != null) {
            return "https://player.vimeo.com/video/" + vimeoId;
        }

        // Altceva (S3, CDN direct) – validare minimă
        validateUrl(trimmed);
        return trimmed;
    }

    private boolean looksLikeUrl(String s) {
        return s.startsWith("http://") || s.startsWith("https://");
    }


    /**
     * Sanitizează o listă de imageUrl-uri.
     * Elimină null/blank și validează fiecare URL.
     */
    public List<String> tokenizeImageUrls(List<String> rawUrls) {
        if (rawUrls == null) return List.of();
        return rawUrls.stream()
                .filter(u -> u != null && !u.isBlank())
                .map(String::trim)
                .filter(this::looksLikeUrl)
                .collect(Collectors.toList());
    }


    /**
     * Generează un token unic asociat unui URL (pentru stocare internă sau tracking).
     * Format: <prefix>_<uuid_scurt>
     */
    public String generateToken(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return (prefix != null ? prefix : "media") + "_" + uuid;
    }

    /**
     * Extrage ID-ul din URL-uri YouTube.
     * Suportă: watch?v=, youtu.be/, /embed/, /shorts/
     */
    private String extractYoutubeId(String url) {
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return null;

        // youtu.be/<id>
        if (url.contains("youtu.be/")) {
            String path = url.substring(url.indexOf("youtu.be/") + 9);
            return path.split("[?&]")[0];
        }

        // /shorts/<id>
        if (url.contains("/shorts/")) {
            String path = url.substring(url.indexOf("/shorts/") + 8);
            return path.split("[?&/]")[0];
        }

        // /embed/<id>
        if (url.contains("/embed/")) {
            String path = url.substring(url.indexOf("/embed/") + 7);
            return path.split("[?&/]")[0];
        }

        // watch?v=<id>
        if (url.contains("v=")) {
            String query = url.substring(url.indexOf("v=") + 2);
            return query.split("[&]")[0];
        }

        return null;
    }

    /**
     * Extrage ID-ul din URL-uri Vimeo.
     * Suportă: vimeo.com/<id>, /video/<id>
     */
    private String extractVimeoId(String url) {
        if (!url.contains("vimeo.com")) return null;

        try {
            URI uri = new URI(url);
            String path = uri.getPath(); // ex: /123456789 sau /video/123456789
            String[] parts = path.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (parts[i].matches("\\d+")) return parts[i];
            }
        } catch (URISyntaxException ignored) {}

        return null;
    }

    /**
     * Validare minimă: URL-ul trebuie să înceapă cu http:// sau https://
     */
    private void validateUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("URL invalid (trebuie să înceapă cu http/https): " + url);
        }
    }
}
