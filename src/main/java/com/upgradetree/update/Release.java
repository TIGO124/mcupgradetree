package com.upgradetree.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

/**
 * The newest release of the plugin on GitHub and its jar: {@code assetUrl} is the API address of
 * the jar (it works for private repositories with a token), {@code sha256} the checksum GitHub
 * lists for it (null when it lists none) and {@code size} its size in bytes (-1 when unknown).
 */
public record Release(String tag, String version, String page, String assetName, String assetUrl, long size, String sha256) {

    /** Parses the answer of GET /repos/{owner}/{repo}/releases/latest; null when the release has no jar. */
    public static Release parse(String json, String pluginName) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String tag = str(o, "tag_name");
        JsonObject jar = null;
        if (o.get("assets") instanceof JsonArray assets) {
            for (JsonElement e : assets) {
                if (!(e instanceof JsonObject a)) {
                    continue;
                }
                String name = str(a, "name").toLowerCase(Locale.ROOT);
                if (!name.endsWith(".jar") || name.endsWith("-sources.jar") || name.endsWith("-javadoc.jar")) {
                    continue;
                }
                if (name.startsWith(pluginName.toLowerCase(Locale.ROOT))) {
                    jar = a;
                    break;
                }
                if (jar == null) {
                    jar = a;
                }
            }
        }
        if (jar == null || tag.isEmpty()) {
            return null;
        }
        String url = str(jar, "url");
        if (url.isEmpty()) {
            url = str(jar, "browser_download_url");
        }
        long size = jar.get("size") != null && jar.get("size").isJsonPrimitive() ? jar.get("size").getAsLong() : -1;
        String digest = str(jar, "digest");
        String sha = digest.regionMatches(true, 0, "sha256:", 0, 7) ? digest.substring(7).toLowerCase(Locale.ROOT) : null;
        String version = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
        return new Release(tag, version, str(o, "html_url"), str(jar, "name"), url, size, sha);
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || !e.isJsonPrimitive() ? "" : e.getAsString().trim();
    }
}
