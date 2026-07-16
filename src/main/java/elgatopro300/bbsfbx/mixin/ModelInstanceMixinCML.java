package elgatopro300.bbsfbx.mixin;

import elgatopro300.bbsfbx.model.fbx.loaders.IMaterialTextureHolder;
import elgatopro300.bbsfbx.model.fbx.loaders.IShapeKeyHolder;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.render.vao.BOBJModelVAO;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.util.math.MatrixStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * CML twin of the FS addon's ModelInstanceMixin. Only the last parameter's
 * type differs between the two mods' {@code render(...)} overloads (CML
 * takes a plain {@code Link defaultTexture} where FS takes a
 * {@code Function<String, Link> textureResolver}), so the redirected call
 * and its target ({@code BOBJModelVAO.updateMesh}) are otherwise identical.
 *
 * <p>Also implements {@link IMaterialTextureHolder}: CML's {@code ModelInstance}
 * has no concept of "this model has several materials" the way FS's does, so
 * that concept is bolted on here purely for FBX models. {@code FBXModelLoaderCML}
 * populates it right after building the instance; every other loader leaves it
 * at its empty defaults.
 */
@Mixin(value = ModelInstance.class, remap = false)
public class ModelInstanceMixinCML implements IMaterialTextureHolder
{
    @Unique
    private List<String> bbsFbx$materials = Collections.emptyList();

    @Unique
    private Map<String, Link> bbsFbx$materialTextures = Collections.emptyMap();

    @Override
    public List<String> bbsFbx$getMaterials()
    {
        return this.bbsFbx$materials;
    }

    @Override
    public void bbsFbx$setMaterials(List<String> materials)
    {
        this.bbsFbx$materials = materials == null ? Collections.emptyList() : materials;
    }

    @Override
    public Map<String, Link> bbsFbx$getMaterialTextures()
    {
        return this.bbsFbx$materialTextures;
    }

    @Override
    public void bbsFbx$setMaterialTextures(Map<String, Link> materialTextures)
    {
        this.bbsFbx$materialTextures = materialTextures == null ? Collections.emptyMap() : materialTextures;
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/function/Supplier;Lmchorse/bbs_mod/utils/colors/Color;IILmchorse/bbs_mod/ui/framework/elements/utils/StencilMap;Lmchorse/bbs_mod/obj/shapes/ShapeKeys;Lmchorse/bbs_mod/resources/Link;)V",
            at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/cubic/render/vao/BOBJModelVAO;updateMesh(Lmchorse/bbs_mod/ui/framework/elements/utils/StencilMap;)V"),
            remap = false
    )
    private void bbsFbx$redirectUpdateMesh(
            BOBJModelVAO vao, StencilMap stencilMap,
            MatrixStack stack, Supplier<ShaderProgram> program, Color color,
            int light, int overlay, StencilMap stencilMap2, ShapeKeys keys,
            Link defaultTexture)
    {
        if (vao instanceof IShapeKeyHolder holder)
        {
            holder.bbsFbx$setShapeKeys(keys);
        }
        vao.updateMesh(stencilMap);
    }
}
