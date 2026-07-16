package elgatopro300.bbsfbx.mixin;

import elgatopro300.bbsfbx.BBSFbxAddon;
import elgatopro300.bbsfbx.model.fbx.loaders.FBXModelLoadCache;
import elgatopro300.bbsfbx.model.fbx.loaders.FBXModelLoaderCML;

import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.resources.Link;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CML-target twin of the FS addon's ModelManagerMixin. Registers
 * {@link FBXModelLoaderCML} instead of the FS-only FBXModelLoader.
 *
 * <p>{@code setupLoaders}, {@code isRelodable(Link)}, the public
 * {@code loaders} field, and {@code reload()} all match BBS FS's signatures
 * on BBS CML EDITION too (verified against both mod sources), so this mixin
 * is otherwise identical to the FS one.</p>
 */
@Mixin(value = ModelManager.class, remap = false)
public class ModelManagerMixinCML
{
    @Inject(method = "setupLoaders", at = @At("TAIL"), remap = false)
    private void bbsFbx$registerFbxLoader(CallbackInfo info)
    {
        ModelManager manager = (ModelManager) (Object) this;
        manager.loaders.add(new FBXModelLoaderCML());
        BBSFbxAddon.LOGGER.info("FBX model loader (CML target) registered");
    }

    @Inject(method = "isRelodable", at = @At("HEAD"), cancellable = true, remap = false)
    private void bbsFbx$fbxIsRelodable(Link link, CallbackInfoReturnable<Boolean> info)
    {
        String path = link.path;

        if (path.startsWith(ModelManager.MODELS_PREFIX)
                && !path.contains("/animations/")
                && !path.contains("/shapes/")
                && path.toLowerCase().endsWith(".fbx"))
        {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "reload", at = @At("HEAD"), remap = false)
    private void bbsFbx$clearCacheOnReload(CallbackInfo info)
    {
        int size = FBXModelLoadCache.size();
        if (size > 0)
        {
            FBXModelLoadCache.clear();
            BBSFbxAddon.LOGGER.info("Cleared FBX model load cache ({} entries) for reload", size);
        }
    }
}
