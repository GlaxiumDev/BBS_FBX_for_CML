package elgatopro300.bbsfbx.mixin;

import elgatopro300.bbsfbx.forms.FBXMaterialRenderContext;
import elgatopro300.bbsfbx.forms.IFBXMaterialTexturesForm;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.util.math.MatrixStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Fills {@link FBXMaterialRenderContext} with this Form's per-material
 * texture overrides (animation-track overrides layered over editor-picked
 * static ones) right before {@code renderModel} draws the model, and clears
 * it right after - {@code BOBJModelVAOMixinCML} reads it while splitting the
 * per-material draw calls. A no-op (empty map) for every model that isn't a
 * multi-material FBX model, so this doesn't affect cubic/OBJ/single-material
 * rendering at all.
 */
@Mixin(value = ModelFormRenderer.class, remap = false)
public abstract class ModelFormRendererMixinCML extends FormRendererMixinCML
{
    @Inject(method = "renderModel", at = @At("HEAD"), remap = false)
    private void bbsFbx$setMaterialContext(
            IEntity target, Supplier<ShaderProgram> program, MatrixStack stack, ModelInstance model,
            int light, int overlay, Color color, boolean ui, StencilMap stencilMap, float transition,
            boolean renderEquipment, CallbackInfo info)
    {
        if (!(this.form instanceof IFBXMaterialTexturesForm holder))
        {
            FBXMaterialRenderContext.set(null);
            return;
        }

        Map<String, Link> overrides = holder.bbsFbx$getMaterialTextureOverrides();
        Map<String, Link> picked = holder.bbsFbx$getMaterialTextures().get();

        if (overrides.isEmpty() && picked.isEmpty())
        {
            FBXMaterialRenderContext.set(null);
            return;
        }

        Map<String, Link> merged = new HashMap<>(picked);
        merged.putAll(overrides);

        FBXMaterialRenderContext.set(merged);
    }

    @Inject(method = "renderModel", at = @At("RETURN"), remap = false)
    private void bbsFbx$clearMaterialContext(
            IEntity target, Supplier<ShaderProgram> program, MatrixStack stack, ModelInstance model,
            int light, int overlay, Color color, boolean ui, StencilMap stencilMap, float transition,
            boolean renderEquipment, CallbackInfo info)
    {
        FBXMaterialRenderContext.clear();
    }
}
