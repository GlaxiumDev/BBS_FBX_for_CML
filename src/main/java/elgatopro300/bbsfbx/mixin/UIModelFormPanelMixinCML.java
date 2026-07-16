package elgatopro300.bbsfbx.mixin;

import elgatopro300.bbsfbx.forms.IFBXMaterialTexturesForm;
import elgatopro300.bbsfbx.model.fbx.loaders.IMaterialTextureHolder;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.panels.UIModelFormPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * CML's own "Pick Texture" button ({@code UIModelFormPanel.pick}) always
 * writes to the single {@code form.texture} field - it has no idea an FBX
 * model can have several distinct materials, so on a multi-material model
 * only the first material's draw call ever actually changed when a texture
 * was picked; every other material kept rendering whatever
 * {@code FBXTextureResolverCML} put in its {@code textures/<material>/}
 * folder at load time, invisibly ignoring the pick.
 *
 * <p>This replaces the button's click handler (its {@code callback} field is
 * public and mutable) at the tail of the constructor: with 0-1 materials it
 * behaves exactly like CML's original button (single default texture, no
 * menu); with 2+ materials it opens a "which material?" context menu first,
 * mirroring BBS FS's {@code UIModelFormPanel}, and routes the pick to the
 * per-material override registered by {@code ModelFormMixinCML} instead of
 * the single default.
 */
@Mixin(value = UIModelFormPanel.class, remap = false)
public abstract class UIModelFormPanelMixinCML extends UIFormPanelMixinCML
{
    @Shadow
    public UIButton pick;

    @Inject(method = "<init>(Lmchorse/bbs_mod/ui/forms/editors/forms/UIForm;)V", at = @At("RETURN"), remap = false)
    private void bbsFbx$replacePickTextureCallback(mchorse.bbs_mod.ui.forms.editors.forms.UIForm editor, CallbackInfo info)
    {
        if (this.pick == null)
        {
            return;
        }

        this.pick.callback = (b) -> this.bbsFbx$onPickTexture();
    }

    private void bbsFbx$onPickTexture()
    {
        ModelForm modelForm = (ModelForm) this.form;
        ModelInstance model = ModelFormRenderer.getModel(modelForm);

        List<String> materials = model instanceof IMaterialTextureHolder holder
                ? holder.bbsFbx$getMaterials()
                : java.util.Collections.emptyList();

        if (materials.size() <= 1)
        {
            this.bbsFbx$openTexturePicker(null, modelForm, model);
            return;
        }

        UIModelFormPanel self = (UIModelFormPanel) (Object) this;

        self.getContext().replaceContextMenu((menu) ->
        {
            for (String material : materials)
            {
                menu.action(Icons.MATERIAL, IKey.constant(material), () -> this.bbsFbx$openTexturePicker(material, modelForm, model));
            }
        });
    }

    private void bbsFbx$openTexturePicker(String material, ModelForm modelForm, ModelInstance model)
    {
        UIModelFormPanel self = (UIModelFormPanel) (Object) this;
        Link link;
        Consumer<Link> callback;

        if (material == null)
        {
            link = modelForm.texture.get();

            if (model != null && link == null)
            {
                link = model.texture;
            }

            callback = modelForm.texture::set;
        }
        else if (modelForm instanceof IFBXMaterialTexturesForm holder)
        {
            link = holder.bbsFbx$getMaterialTextures().getLink(material);

            if (link == null && model instanceof IMaterialTextureHolder materialHolder)
            {
                Link fallback = modelForm.texture.get() != null ? modelForm.texture.get() : (model != null ? model.texture : null);

                link = materialHolder.bbsFbx$getMaterialTexture(material, fallback);
            }

            callback = (l) -> holder.bbsFbx$getMaterialTextures().setLink(material, l);
        }
        else
        {
            /* IFBXMaterialTexturesForm should always be present (ModelFormMixinCML mixes it into every
             * ModelForm) - this is just a safe fallback in case some other mod's mixin got in the way. */
            link = modelForm.texture.get();
            callback = modelForm.texture::set;
        }

        UITexturePicker picker = UITexturePicker.open(self.getContext(), link, callback);

        if (picker != null)
        {
            picker.withFormPreview(() -> modelForm);
        }
    }
}
