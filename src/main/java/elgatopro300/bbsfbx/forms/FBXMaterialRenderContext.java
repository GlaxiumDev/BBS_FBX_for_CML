package elgatopro300.bbsfbx.forms;

import mchorse.bbs_mod.resources.Link;

import java.util.Collections;
import java.util.Map;

/**
 * Carries the current {@code ModelForm}'s per-material texture overrides
 * across the call from {@code ModelFormRenderer} (which knows the Form) down
 * to {@code BOBJModelVAOMixinCML}'s per-material draw split (which only
 * knows the compiled mesh data, not which Form is rendering it - several
 * Forms can share one loaded FBX {@code ModelInstance}, each wanting its own
 * override). {@code ModelFormRendererMixinCML} sets this right before
 * rendering and clears it right after; every render call starts from a
 * clean slate so a leaked value (e.g. an exception skipping the clear)
 * cannot outlive the next frame's render.
 */
public final class FBXMaterialRenderContext
{
    private static final ThreadLocal<Map<String, Link>> CURRENT = new ThreadLocal<>();

    private FBXMaterialRenderContext() {}

    public static void set(Map<String, Link> overrides)
    {
        CURRENT.set(overrides == null || overrides.isEmpty() ? null : overrides);
    }

    public static void clear()
    {
        CURRENT.remove();
    }

    /**
     * Resolves the texture to use for {@code material} on the model
     * currently being drawn, in priority order:
     * animation-track override &gt; editor-picked static override &gt;
     * {@code loadTimeDefault} (the folder/Kd texture baked in at model load).
     */
    public static Link resolve(String material, Link loadTimeDefault)
    {
        Map<String, Link> overrides = CURRENT.get();

        if (overrides == null)
        {
            return loadTimeDefault;
        }

        Link override = overrides.get(material);

        return override != null ? override : loadTimeDefault;
    }

    public static Map<String, Link> empty()
    {
        return Collections.emptyMap();
    }
}
