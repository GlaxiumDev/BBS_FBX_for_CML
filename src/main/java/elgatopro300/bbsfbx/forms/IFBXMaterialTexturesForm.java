package elgatopro300.bbsfbx.forms;

import mchorse.bbs_mod.resources.Link;

import java.util.Map;

/**
 * Implemented via mixin on CML's {@code ModelForm}. Exposes the two layers
 * FS's {@code ModelForm} has and CML's doesn't:
 *
 * <ul>
 *   <li>{@link #bbsFbx$getMaterialTextures()} - the editor-picked, saved,
 *       non-animated per-material override (one texture picked from the
 *       "Pick Texture" material menu).</li>
 *   <li>{@link #bbsFbx$getMaterialTextureOverrides()} - the transient,
 *       per-frame override driven by a per-material animation track. Empty
 *       until per-material keyframe tracks are wired up on the CML side;
 *       reading it is always safe.</li>
 * </ul>
 */
public interface IFBXMaterialTexturesForm
{
    FBXValueLinks bbsFbx$getMaterialTextures();

    Map<String, Link> bbsFbx$getMaterialTextureOverrides();
}
