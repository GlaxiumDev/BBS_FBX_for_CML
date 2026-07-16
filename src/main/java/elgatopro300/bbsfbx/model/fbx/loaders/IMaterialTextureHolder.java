package elgatopro300.bbsfbx.model.fbx.loaders;

import mchorse.bbs_mod.resources.Link;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implemented via mixin on CML's {@code ModelInstance}. CML's own
 * {@code ModelInstance} only has a single {@code texture} field - unlike BBS
 * FS, it has no idea a model can have several distinct materials. This
 * interface bolts that concept on for FBX models specifically (populated by
 * {@code FBXModelLoaderCML} right after building the instance); models
 * loaded by every other loader (cubic/OBJ/etc.) simply never have it
 * populated and every method here is a safe no-op/empty-collection default.
 */
public interface IMaterialTextureHolder
{
    /** Ordered, distinct FBX material names. Empty for non-FBX models or single-material FBX models. */
    default List<String> bbsFbx$getMaterials()
    {
        return Collections.emptyList();
    }

    void bbsFbx$setMaterials(List<String> materials);

    /** Per-material *default* texture (folder/Kd baked at load time) - not any user override. */
    default Map<String, Link> bbsFbx$getMaterialTextures()
    {
        return Collections.emptyMap();
    }

    void bbsFbx$setMaterialTextures(Map<String, Link> materialTextures);

    /** {@code materialTextures.get(material)}, falling back to {@code fallback} if absent/null. */
    default Link bbsFbx$getMaterialTexture(String material, Link fallback)
    {
        Link link = this.bbsFbx$getMaterialTextures().get(material);

        return link != null ? link : fallback;
    }
}
