package com.upgradetree;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Runs before the server loads its registries. Registers the "upgradetree:open" dialog and puts it
 * in the vanilla quick_actions tag, so the client's Controls › Quick Actions key opens it. The
 * dialog's button sends a custom click that {@link com.upgradetree.listener.QuickActionListener}
 * turns into the menu. The dialog is registered once for everyone, so it cannot follow each
 * player's language: it shows the texts of every language (server language first).
 */
public final class UpgradeTreeBootstrap implements PluginBootstrap {

    public static final Key OPEN_KEY = Key.key("upgradetree", "open");
    public static final TypedKey<Dialog> OPEN_DIALOG = DialogKeys.create(OPEN_KEY);

    private static final String FALLBACK_TITLE = "<gradient:#7DD3FC:#C084FC><b>Upgrade Tree</b></gradient>";
    private static final String FALLBACK_BODY = "<gray>Open your upgrade tree.";
    private static final String FALLBACK_OPEN = "<green><b>▶ Open the tree";

    @Override
    public void bootstrap(BootstrapContext ctx) {
        Path dir = ctx.getDataDirectory();
        YamlConfiguration config = load(dir.resolve("config.yml"));
        String language = config.getString("language", "tr").trim().toLowerCase(Locale.ROOT);
        boolean quickAction = config.getBoolean("access.quick-action", true);
        Set<String> titles = new LinkedHashSet<>();
        Set<String> bodies = new LinkedHashSet<>();
        Set<String> opens = new LinkedHashSet<>();
        for (String code : languages(dir, language)) {
            YamlConfiguration lang = langFile(dir, code);
            titles.add(lang.getString("quick-title", FALLBACK_TITLE));
            bodies.add(lang.getString("quick-body", FALLBACK_BODY));
            opens.add(lang.getString("quick-open", FALLBACK_OPEN));
        }
        MiniMessage mm = MiniMessage.miniMessage();
        Component title = mm.deserialize(String.join(" <dark_gray>•</dark_gray> ", titles));
        List<DialogBody> body = new ArrayList<>();
        for (String b : bodies) {
            body.add(DialogBody.plainMessage(mm.deserialize(b)));
        }
        Component open = mm.deserialize(String.join(" <gray>/</gray> ", opens));
        int width = opens.size() > 1 ? 300 : 200;

        // never let the optional shortcut stop the plugin from loading
        try {
            register(ctx, title, body, open, width, quickAction);
        } catch (RuntimeException ex) {
            ctx.getLogger().warn("Quick Actions dialog could not be registered, the key will not work: {}", ex.toString());
        }
    }

    /** The server language first, then the built-in ones, then every lang/*.yml of the server. */
    private static Set<String> languages(Path dir, String server) {
        Set<String> codes = new LinkedHashSet<>();
        codes.add(server);
        codes.add("tr");
        codes.add("en");
        File[] files = dir.resolve("lang").toFile().listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                codes.add(f.getName().substring(0, f.getName().length() - 4).toLowerCase(Locale.ROOT));
            }
        }
        codes.removeIf(c -> !c.matches("[a-z0-9_-]{2,10}"));
        return codes;
    }

    private static void register(BootstrapContext ctx, Component title, List<DialogBody> body, Component open, int width,
                                 boolean quickAction) {
        ctx.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose().newHandler(event ->
                event.registry().register(OPEN_DIALOG, b -> b
                        .base(DialogBase.builder(title)
                                .canCloseWithEscape(true)
                                .pause(false)
                                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                                .body(body)
                                .build())
                        .type(DialogType.notice(ActionButton.builder(open)
                                .width(width)
                                .action(DialogAction.customClick(OPEN_KEY, null))
                                .build())))));

        if (quickAction) {
            ctx.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG).newHandler(event ->
                    event.registrar().addToTag(DialogTagKeys.QUICK_ACTIONS, List.of(OPEN_DIALOG))));
        }
    }

    /** lang/&lt;language&gt;.yml of the server, else the built-in file, else English defaults. */
    private YamlConfiguration langFile(Path dir, String language) {
        if (!language.matches("[a-z0-9_-]{2,10}")) {
            language = "en";
        }
        Path file = dir.resolve("lang").resolve(language + ".yml");
        if (Files.isRegularFile(file)) {
            YamlConfiguration y = load(file);
            if (y.contains("quick-open")) {
                return y;
            }
        }
        try (InputStream in = getClass().getResourceAsStream("/lang/" + language + ".yml")) {
            if (in != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            // fall back to the English defaults below
        }
        return new YamlConfiguration();
    }

    private static YamlConfiguration load(Path file) {
        YamlConfiguration y = new YamlConfiguration();
        if (Files.isRegularFile(file)) {
            try {
                y.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
            } catch (Exception ignored) {
                // a broken file is reported later by the plugin itself
            }
        }
        return y;
    }
}
