package elgatopro300.bbsfbx.mixin;

import elgatopro300.bbsfbx.forms.FBXValueLinks;
import elgatopro300.bbsfbx.forms.IFBXMaterialTexturesForm;

import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.resources.Link;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Bolts FBX per-material texture storage onto CML's {@code ModelForm}:
 *
 * <ul>
 *   <li>{@code bbsFbx$materialTextures} - a {@link FBXValueLinks}, registered
 *       with {@code ModelForm} the same way every other value is (via
 *       {@code add(...)} at the tail of the constructor), so it saves/loads
 *       with the project exactly like {@code texture} does. Hidden from the
 *       generic value editor ({@code invisible()}) since it's driven entirely
 *       by the material context menu on the "Pick Texture" button, not a
 *       generic settings field.</li>
 *   <li>{@code bbsFbx$materialTextureOverrides} - transient, per-frame,
 *       cleared/repopulated every tick by a (future) per-material keyframe
 *       track using the {@code "texture.materials.<name>"} channel-id
 *       convention. Always safe to read even with no such track wired up
 *       yet - it just stays empty.</li>
 * </ul>
 */
@Mixin(value = ModelForm.class, remap = false)
public abstract class ModelFormMixinCML implements IFBXMaterialTexturesForm
{
    @Unique
    private final FBXValueLinks bbsFbx$materialTextures = new FBXValueLinks("bbs_fbx_material_textures");

    @Unique
    private final Map<String, Link> bbsFbx$materialTextureOverrides = new HashMap<>();

    @Inject(method = "<init>()V", at = @At("RETURN"), remap = false)
    private void bbsFbx$registerMaterialTextures(CallbackInfo info)
    {
        this.bbsFbx$materialTextures.invisible();
        ((ModelForm) (Object) this).add(this.bbsFbx$materialTextures);
    }

    @Override
    public FBXValueLinks bbsFbx$getMaterialTextures()
    {
        return this.bbsFbx$materialTextures;
    }

    @Override
    public Map<String, Link> bbsFbx$getMaterialTextureOverrides()
    {
        return this.bbsFbx$materialTextureOverrides;
    }
}
