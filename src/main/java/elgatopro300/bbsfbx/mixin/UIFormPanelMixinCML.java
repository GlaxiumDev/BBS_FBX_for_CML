package elgatopro300.bbsfbx.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * On BBS CML EDITION, {@code form} is declared on the generic base class
 * {@code UIFormPanel<T extends Form>}, not on {@code UIModelFormPanel} itself
 * (unlike BBS FS, where {@code UIModelFormPanel} declares it directly).
 *
 * <p>Mixin's {@code @Shadow} only resolves members against the exact class
 * named in {@code @Mixin(value = ...)}; it does not walk that class's
 * runtime superclass chain. So {@code UIModelFormPanelMixinCML} can't shadow
 * this field directly - it has to inherit access to it (via plain Java
 * {@code extends}) from a mixin that targets the class which actually
 * declares it.
 */
@Mixin(value = UIFormPanel.class, remap = false)
public abstract class UIFormPanelMixinCML
{
    @Shadow
    protected Form form;
}
