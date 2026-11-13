package com.upgradetree;

import com.upgradetree.config.Style;
import com.upgradetree.util.Text;
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
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs before the server loads its registries. Registers the "upgradetree:open" dialog and puts it
 * in the vanilla quick_actions tag, so the client's Controls › Quick Actions key opens it. The
 * dialog's button sends a custom click that {@link com.upgradetree.listener.QuickActionListener}
 * turns into the menu. Its texts are the quick-* keys of messages.yml.
 */
public final class UpgradeTreeBootstrap implements PluginBootstrap {

    public static final Key OPEN_KEY = Key.key("upgradetree", "open");
    public static final TypedKey<Dialog> OPEN_DIALOG = DialogKeys.create(OPEN_KEY);

    private static final String FALLBACK_TITLE = "<accent><b>Upgrade Tree";
    private static final String FALLBACK_BODY = "<body>Open your upgrade tree.";
    private static final String FALLBACK_OPEN = "<good><b>Open the tree";

    @Override
    public void bootstrap(BootstrapContext ctx) {
        Path dir = ctx.getDataDirectory();
        YamlConfiguration config = load(dir.resolve("config.yml"));
        boolean quickAction = config.getBoolean("access.quick-action", true);
        Text.palette(Style.of(config.getString("menu.style"), Style.DEFAULT).palette);
        YamlConfiguration messages = messages(dir);
        // Text.mm knows the plugin's colour names (<accent>, <body>...) used by messages.yml
        Component title = Text.mm(messages.getString("quick-title", FALLBACK_TITLE));
        List<DialogBody> body = List.of(DialogBody.plainMessage(Text.mm(messages.getString("quick-body", FALLBACK_BODY))));
        Component open = Text.mm(messages.getString("quick-open", FALLBACK_OPEN));

        // never let the optional shortcut stop the plugin from loading
        try {
            register(ctx, title, body, open, 200, quickAction);
        } catch (RuntimeException ex) {
            ctx.getLogger().warn("Quick Actions dialog could not be registered, the key will not work: {}", ex.toString());
        }
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

    /** The server's messages.yml when it has the dialog texts, else the built-in file (else the defaults above). */
    private YamlConfiguration messages(Path dir) {
        Path file = dir.resolve("messages.yml");
        if (Files.isRegularFile(file)) {
            YamlConfiguration y = load(file);
            if (y.contains("quick-open")) {
                return y;
            }
        }
        try (InputStream in = getClass().getResourceAsStream("/messages.yml")) {
            if (in != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            // fall back to the defaults above
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
