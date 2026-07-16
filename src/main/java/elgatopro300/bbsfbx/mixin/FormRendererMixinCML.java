package elgatopro300.bbsfbx.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Same situation as {@link UIFormPanelMixinCML}: on BBS CML EDITION,
 * {@code form} is declared on the generic base class
 * {@code FormRenderer<T extends Form>}, not on {@code ModelFormRenderer}
 * itself. Mixin's {@code @Shadow} doesn't walk the target's superclass
 * chain, so {@code ModelFormRendererMixinCML} inherits access to this field
 * from here instead of shadowing it directly.
 */
@Mixin(value = FormRenderer.class, remap = false)
public abstract class FormRendererMixinCML
{
    @Shadow
    protected Form form;
}
