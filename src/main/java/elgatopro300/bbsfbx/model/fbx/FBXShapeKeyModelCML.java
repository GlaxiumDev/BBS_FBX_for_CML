package elgatopro300.bbsfbx.model.fbx;

import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * CML twin of the FS addon's {@code FBXShapeKeyModel}. BBS CML EDITION's own
 * {@code BOBJModel.getShapeKeys()} is hardcoded to return an empty set (see
 * its source), which is what tells the rest of the engine "this model has no
 * shape keys, don't bother asking for weights" - overriding it here is what
 * actually turns shape keys on for an FBX model on this target, feeding
 * {@code ModelInstance.hasShapeKeys()} / the shape-key UI real key names.
 *
 * <p>CML's {@code BOBJModel.copy()} is hardcoded to
 * {@code new BOBJModel(...)} rather than using the runtime type, so it's
 * overridden here too - otherwise a copy (used for e.g. pose-editor preview)
 * would silently downgrade back to the base class and lose its shape keys.
 */
public class FBXShapeKeyModelCML extends BOBJModel
{
    private final Set<String> shapeKeys;

    public FBXShapeKeyModelCML(BOBJArmature armature, BOBJLoader.CompiledData meshData, boolean simple, Set<String> shapeKeys)
    {
        super(armature, meshData, simple);

        LinkedHashSet<String> copy = new LinkedHashSet<>();

        if (shapeKeys != null)
        {
            copy.addAll(shapeKeys);
        }

        this.shapeKeys = Collections.unmodifiableSet(copy);
    }

    @Override
    public Set<String> getShapeKeys()
    {
        return this.shapeKeys;
    }

    @Override
    public IModel copy()
    {
        FBXShapeKeyModelCML model = new FBXShapeKeyModelCML(this.getArmature().copy(), this.getMeshData(), false, this.shapeKeys);

        model.setup();

        return model;
    }
}
