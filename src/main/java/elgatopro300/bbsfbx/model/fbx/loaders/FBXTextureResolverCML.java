package elgatopro300.bbsfbx.model.fbx.loaders;

import elgatopro300.bbsfbx.model.fbx.FBXMesh;

import mchorse.bbs_mod.bobj.BOBJLoader.BOBJData;
import mchorse.bbs_mod.bobj.BOBJLoader.BOBJMesh;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * CML-target texture resolution.
 *
 * <p>BBS FS's {@code ModelInstance} carries a {@code materials} list and a
 * {@code materialTextures} map, resolved per mesh via
 * {@code IModelLoader.findMaterialTexture}/{@code ensureMaterialFolder}.
 * BBS CML EDITION's {@code ModelInstance} has neither those fields nor those
 * static helpers, and its {@code BOBJModel} only holds one CompiledData -
 * so per-material textures on this target work differently:
 *
 * <ul>
 *   <li>{@link #resolveMaterialTextures} resolves ONE texture per distinct
 *       FBX material and hands back a {@code Link[]} index-aligned with
 *       {@code FBXCompiledData.materialNames}. {@code BOBJModelVAOMixinCML}
 *       reads this back off the compiled data at render time and issues one
 *       {@code glDrawArrays} call per material - CML's engine has no native
 *       concept of per-vertex material groups, so that mixin adds it.</li>
 *   <li>{@link #resolveTexture} resolves a single fallback texture for the
 *       whole model (used as {@code ModelInstance.texture}, e.g. for the
 *       thumbnail/editor preview and as a last-resort default).</li>
 * </ul>
 *
 * Each material is resolved through the same convention BBS FS uses:
 * <ol>
 *   <li>A {@code textures/<material>/default.png} folder among the model's
 *       links.</li>
 *   <li>The mesh's own diffuse texture file, if the FBX referenced one
 *       directly.</li>
 *   <li>A baked solid-color PNG under {@code textures/<material>/default.png},
 *       if the material was a flat Base Color with no image texture (CML has
 *       no {@code LinkUtils.color()} synthetic-link convention like FS does,
 *       so this writes an actual small PNG instead).</li>
 *   <li>Any image file among the model's links, as an absolute last resort.</li>
 * </ol>
 */
public final class FBXTextureResolverCML
{
    private FBXTextureResolverCML() {}

    public static Link[] resolveMaterialTextures(BOBJData data, String[] materialNames, Link model, Collection<Link> links, AssetProvider provider)
    {
        Map<String, FBXMesh> representative = new HashMap<>();

        for (BOBJMesh mesh : data.meshes)
        {
            if (mesh instanceof FBXMesh fbxMesh)
            {
                representative.putIfAbsent(mesh.name == null ? "" : mesh.name, fbxMesh);
            }
        }

        Link[] result = new Link[materialNames.length];

        for (int i = 0; i < materialNames.length; i++)
        {
            String materialName = materialNames[i];
            FBXMesh fbxMesh = representative.get(materialName);

            result[i] = resolveOne(materialName, fbxMesh, model, links, provider);
        }

        return result;
    }

    public static Link resolveTexture(BOBJData data, Link model, Collection<Link> links, AssetProvider provider)
    {
        if (data.meshes.isEmpty() || !(data.meshes.get(0) instanceof FBXMesh mesh))
        {
            return firstImageLink(links);
        }

        Link resolved = resolveOne(mesh.name, mesh, model, links, provider);

        return resolved != null ? resolved : firstImageLink(links);
    }

    private static Link resolveOne(String materialName, FBXMesh mesh, Link model, Collection<Link> links, AssetProvider provider)
    {
        if (materialName != null && !materialName.isEmpty())
        {
            Link folderTexture = findMaterialTexture(links, model, materialName);

            if (folderTexture != null)
            {
                return folderTexture;
            }
        }

        if (mesh != null && mesh.texture != null && !mesh.texture.isEmpty())
        {
            Link specificLink = model.combine(mesh.texture);

            if (links.contains(specificLink))
            {
                return specificLink;
            }

            for (Link l : links)
            {
                if (l.path.endsWith(mesh.texture))
                {
                    return l;
                }
            }
        }

        if (materialName != null && !materialName.isEmpty() && mesh != null && mesh.color != null)
        {
            Link baked = bakeSolidColor(provider, model, materialName, mesh.color);

            if (baked != null)
            {
                return baked;
            }
        }

        return firstImageLink(links);
    }

    private static Link firstImageLink(Collection<Link> links)
    {
        for (Link l : links)
        {
            String path = l.path.toLowerCase();

            if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg"))
            {
                return l;
            }
        }

        return null;
    }

    /**
     * Local reimplementation of BBS FS's
     * {@code IModelLoader.findMaterialTexture} - BBS CML EDITION's
     * IModelLoader doesn't declare it.
     */
    private static Link findMaterialTexture(Collection<Link> links, Link model, String material)
    {
        String prefix = model.toString();
        String folder = "/" + material + "/";

        for (Link link : links)
        {
            String string = link.toString();

            if (string.startsWith(prefix) && string.contains(folder) && string.endsWith(".png"))
            {
                return link;
            }
        }

        return null;
    }

    private static Link bakeSolidColor(AssetProvider provider, Link model, String material, float[] color)
    {
        try
        {
            File folder = provider.getFile(model.combine("textures/" + material));

            if (folder == null)
            {
                return null;
            }

            File targetFile = new File(folder, "default.png");

            if (!targetFile.exists())
            {
                int r = clampToByte(color[0]);
                int g = clampToByte(color[1]);
                int b = clampToByte(color[2]);
                int argb = (0xFF << 24) | (r << 16) | (g << 8) | b;

                int size = 16;
                BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                int[] pixels = new int[size * size];
                Arrays.fill(pixels, argb);
                image.setRGB(0, 0, size, size, pixels, 0, size);

                folder.mkdirs();
                ImageIO.write(image, "png", targetFile);
            }

            return model.combine("textures/" + material + "/default.png");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static int clampToByte(float value)
    {
        return Math.max(0, Math.min(255, Math.round(value * 255f)));
    }
}
