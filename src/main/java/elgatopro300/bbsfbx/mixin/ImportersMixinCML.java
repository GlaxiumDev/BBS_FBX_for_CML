package elgatopro300.bbsfbx.mixin;

import elgatopro300.bbsfbx.importers.FBXImporter;

import mchorse.bbs_mod.importers.Importers;
import mchorse.bbs_mod.importers.types.IImporter;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Identical to the FS addon's ImportersMixin - BBS CML EDITION's
 * {@code Importers} class declares the same
 * {@code private final static List<IImporter> importers} field populated in
 * a static initializer, so the same {@code <clinit>} TAIL injection works.
 */
@Mixin(value = Importers.class, remap = false)
public class ImportersMixinCML
{
    @Shadow
    @Final
    private static List<IImporter> importers;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void bbsFbx$registerFbxImporter(CallbackInfo info)
    {
        importers.add(new FBXImporter());
    }
}
