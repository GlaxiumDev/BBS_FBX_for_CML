package elgatopro300.bbsfbx.model.fbx.loaders;

import elgatopro300.bbsfbx.model.fbx.FBXConverter;
import elgatopro300.bbsfbx.model.fbx.FBXShapeKeyModelCML;
import elgatopro300.bbsfbx.model.fbx.FBXShapeKeyNames;

import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJLoader.BOBJData;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.cubic.model.loaders.IModelLoader;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;

import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registers as BBS CML EDITION's FBX model loader (see the {@code bbs-addon}
 * mixin, {@code ModelManagerMixinCML}, that installs this into
 * {@code ModelManager.loaders}).
 *
 * <p>This is the CML twin of {@code FBXModelLoader} (the BBS FS loader). The
 * pipeline up to "we have a BOBJData" is 100% shared - both loaders call the
 * same {@link FBXAssimpImporter}, {@link FBXConverter}, and
 * {@link FBXModelLoadCache} from the {@code common} module. Everything after
 * that differs because BBS CML EDITION's model/render API diverged from BBS
 * FS's:
 *
 * <ul>
 *   <li>CML's {@code BOBJModel} constructor takes ONE {@code CompiledData}
 *       for the whole model, not a {@code List<CompiledData>} (one per
 *       mesh) like FS's does - so every mesh gets flattened into a single
 *       merged mesh via {@link FBXMeshCompiler#compileMergedWithMaterials},
 *       which also tags each vertex with which FBX material it came from.</li>
 *   <li>Per-material textures: resolved by {@link FBXTextureResolverCML}
 *       into a {@code Link[]} attached to the compiled data.
 *       {@code BOBJModelVAOMixinCML} reads that back at render time to
 *       issue one draw call per material - CML's {@code IModelLoader} has no
 *       {@code findMaterialTexture}/{@code ensureMaterialFolder} helpers and
 *       its {@code ModelInstance} has no {@code materials}/
 *       {@code materialTextures} fields the way FS's does, so this can't
 *       reuse FS's mechanism directly.</li>
 *   <li>Shape keys: {@link FBXShapeKeyModelCML} overrides
 *       {@code getShapeKeys()} (CML's own {@code BOBJModel} hardcodes it
 *       empty), and {@code BOBJModelVAOMixinCML} does the actual per-vertex
 *       blending, mirroring the FS addon's own {@code BOBJModelVAOMixin}.</li>
 * </ul>
 */
public class FBXModelLoaderCML implements IModelLoader
{
    @Override
    public ModelInstance load(String id, ModelManager models, Link model, Collection<Link> links, MapType config)
    {
        Link fbxLink = null;

        for (Link link : links)
        {
            if (link.path.toLowerCase().endsWith(".fbx"))
            {
                fbxLink = link;
                break;
            }
        }

        if (fbxLink == null)
        {
            return null;
        }

        try
        {
            byte[] bytes;
            try (InputStream stream = models.provider.getAsset(fbxLink))
            {
                if (stream == null)
                {
                    return null;
                }
                bytes = stream.readAllBytes();
            }

            long contentHash = FBXModelLoadCache.hash(bytes);
            FBXModelLoadCache.Cached cached = FBXModelLoadCache.get(fbxLink.path, contentHash);

            BOBJData data;
            Set<String> shapeKeyNames;

            if (cached != null)
            {
                data = cached.data;
                shapeKeyNames = cached.shapeKeyNames;

                boolean texturesReextracted = ensureTexturesPresent(bytes, cached.texturedMaterials, models, model);

                if (texturesReextracted)
                {
                    FBXModelLoadCache.invalidate(fbxLink.path);
                }
            }
            else
            {
                AIScene scene = null;
                Set<String> texturedMaterials;

                try
                {
                    scene = FBXAssimpImporter.importScene(bytes);

                    if (scene == null)
                    {
                        return null;
                    }

                    shapeKeyNames = FBXShapeKeyNames.collectShapeKeyNames(scene);
                    data = FBXConverter.convert(scene);
                    texturedMaterials = FBXConverter.extractEmbeddedTextures(scene, models.provider, model);
                }
                finally
                {
                    if (scene != null)
                    {
                        Assimp.aiReleaseImport(scene);
                    }
                }

                FBXModelLoadCache.put(fbxLink.path, contentHash, data, shapeKeyNames, texturedMaterials);
            }

            data.initiateArmatures();

            FBXCompiledData merged = FBXMeshCompiler.compileMergedWithMaterials(data);

            Link[] materialTextures = FBXTextureResolverCML.resolveMaterialTextures(data, merged.materialNames, model, links, models.provider);
            merged.setMaterialTextures(materialTextures);

            BOBJArmature armature = null;
            if (!data.armatures.isEmpty())
            {
                armature = data.armatures.values().iterator().next();
            }

            if (armature == null)
            {
                armature = new BOBJArmature("Armature");
                armature.initArmature();
            }

            FBXShapeKeyModelCML bobjModel = new FBXShapeKeyModelCML(armature, merged, false, shapeKeyNames);

            Animations animations = FBXAnimationConverter.convert(data.actions, models.parser);

            Link textureLink = materialTextures.length > 0 && materialTextures[0] != null
                    ? materialTextures[0]
                    : FBXTextureResolverCML.resolveTexture(data, model, links, models.provider);

            ModelInstance modelInstance = new ModelInstance(id, bobjModel, animations, textureLink);

            if (modelInstance instanceof IMaterialTextureHolder holder)
            {
                List<String> materials = new ArrayList<>();
                Map<String, Link> defaults = new LinkedHashMap<>();

                for (int i = 0; i < merged.materialNames.length; i++)
                {
                    String name = merged.materialNames[i];

                    if (name == null || name.isEmpty())
                    {
                        continue;
                    }

                    materials.add(name);

                    if (i < materialTextures.length && materialTextures[i] != null)
                    {
                        defaults.put(name, materialTextures[i]);
                    }
                }

                holder.bbsFbx$setMaterials(materials);
                holder.bbsFbx$setMaterialTextures(defaults);
            }

            modelInstance.applyConfig(config);
            return modelInstance;
        }
        catch (Throwable e)
        {
            System.err.println("Failed to load FBX model for " + id + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return null;
    }

    /**
     * Same re-extraction safety net as the FS loader: if a cached load's
     * known textured materials are missing their PNG on disk (folder
     * deleted by the user), re-import purely to rerun texture extraction.
     */
    private static boolean ensureTexturesPresent(byte[] bytes, Set<String> texturedMaterials, ModelManager models, Link model)
    {
        if (texturedMaterials == null || texturedMaterials.isEmpty())
        {
            return false;
        }

        boolean missing = false;

        for (String materialName : texturedMaterials)
        {
            File folder = models.provider.getFile(model.combine("textures/" + materialName));
            File target = folder == null ? null : new File(folder, "default.png");

            if (target == null || !target.exists())
            {
                missing = true;
                break;
            }
        }

        if (!missing)
        {
            return false;
        }

        AIScene scene = null;

        try
        {
            scene = FBXAssimpImporter.importScene(bytes);

            if (scene != null)
            {
                FBXConverter.extractEmbeddedTextures(scene, models.provider, model);
            }
        }
        finally
        {
            if (scene != null)
            {
                Assimp.aiReleaseImport(scene);
            }
        }

        return true;
    }
}
