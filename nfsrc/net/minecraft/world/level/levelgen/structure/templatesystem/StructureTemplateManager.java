package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class StructureTemplateManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String STRUCTURE_RESOURCE_DIRECTORY_NAME = "structure";
    private static final String STRUCTURE_GENERATED_DIRECTORY_NAME = "structures";
    private static final String STRUCTURE_FILE_EXTENSION = ".nbt";
    private static final String STRUCTURE_TEXT_FILE_EXTENSION = ".snbt";
    private final Map<ResourceLocation, Optional<StructureTemplate>> structureRepository = Maps.newConcurrentMap();
    private final DataFixer fixerUpper;
    private ResourceManager resourceManager;
    private final Path generatedDir;
    private final List<StructureTemplateManager.Source> sources;
    private final HolderGetter<Block> blockLookup;
    private static final FileToIdConverter RESOURCE_LISTER = new FileToIdConverter("structure", ".nbt");

    public StructureTemplateManager(
        ResourceManager resourceManager, LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer fixerUpper, HolderGetter<Block> blockLookup
    ) {
        this.resourceManager = resourceManager;
        this.fixerUpper = fixerUpper;
        this.generatedDir = levelStorageAccess.getLevelPath(LevelResource.GENERATED_DIR).normalize();
        this.blockLookup = blockLookup;
        Builder<StructureTemplateManager.Source> builder = ImmutableList.builder();
        builder.add(new StructureTemplateManager.Source(this::loadFromGenerated, this::listGenerated));
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            builder.add(new StructureTemplateManager.Source(this::loadFromTestStructures, this::listTestStructures));
        }

        builder.add(new StructureTemplateManager.Source(this::loadFromResource, this::listResources));
        this.sources = builder.build();
    }

    public StructureTemplate getOrCreate(ResourceLocation id) {
        Optional<StructureTemplate> optional = this.get(id);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            StructureTemplate structuretemplate = new StructureTemplate();
            this.structureRepository.put(id, Optional.of(structuretemplate));
            return structuretemplate;
        }
    }

    public Optional<StructureTemplate> get(ResourceLocation id) {
        return this.structureRepository.computeIfAbsent(id, this::tryLoad);
    }

    public Stream<ResourceLocation> listTemplates() {
        return this.sources.stream().flatMap(p_230376_ -> p_230376_.lister().get()).distinct();
    }

    private Optional<StructureTemplate> tryLoad(ResourceLocation id) {
        for (StructureTemplateManager.Source structuretemplatemanager$source : this.sources) {
            try {
                Optional<StructureTemplate> optional = structuretemplatemanager$source.loader().apply(id);
                if (optional.isPresent()) {
                    return optional;
                }
            } catch (Exception exception) {
            }
        }

        return Optional.empty();
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.structureRepository.clear();
    }

    private Optional<StructureTemplate> loadFromResource(ResourceLocation id) {
        ResourceLocation resourcelocation = RESOURCE_LISTER.idToFile(id);
        return this.load(() -> this.resourceManager.open(resourcelocation), p_230366_ -> LOGGER.error("Couldn't load structure {}", id, p_230366_));
    }

    private Stream<ResourceLocation> listResources() {
        return RESOURCE_LISTER.listMatchingResources(this.resourceManager).keySet().stream().map(RESOURCE_LISTER::fileToId);
    }

    private Optional<StructureTemplate> loadFromTestStructures(ResourceLocation id) {
        return this.loadFromSnbt(id, Paths.get(StructureUtils.testStructuresDir));
    }

    private Stream<ResourceLocation> listTestStructures() {
        Path path = Paths.get(StructureUtils.testStructuresDir);
        if (!Files.isDirectory(path)) {
            return Stream.empty();
        } else {
            List<ResourceLocation> list = new ArrayList<>();
            this.listFolderContents(path, "minecraft", ".snbt", list::add);
            return list.stream();
        }
    }

    private Optional<StructureTemplate> loadFromGenerated(ResourceLocation id) {
        if (!Files.isDirectory(this.generatedDir)) {
            return Optional.empty();
        } else {
            Path path = this.createAndValidatePathToGeneratedStructure(id, ".nbt");
            return this.load(() -> new FileInputStream(path.toFile()), p_230400_ -> LOGGER.error("Couldn't load structure from {}", path, p_230400_));
        }
    }

    private Stream<ResourceLocation> listGenerated() {
        if (!Files.isDirectory(this.generatedDir)) {
            return Stream.empty();
        } else {
            try {
                List<ResourceLocation> list = new ArrayList<>();

                try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(this.generatedDir, p_230419_ -> Files.isDirectory(p_230419_))) {
                    for (Path path : directorystream) {
                        String s = path.getFileName().toString();
                        Path path1 = path.resolve("structures");
                        this.listFolderContents(path1, s, ".nbt", list::add);
                    }
                }

                return list.stream();
            } catch (IOException ioexception) {
                return Stream.empty();
            }
        }
    }

    private void listFolderContents(Path folder, String namespace, String extension, Consumer<ResourceLocation> output) {
        int i = extension.length();
        Function<String, String> function = p_230358_ -> p_230358_.substring(0, p_230358_.length() - i);

        try (Stream<Path> stream = Files.find(
                folder, Integer.MAX_VALUE, (p_352038_, p_352039_) -> p_352039_.isRegularFile() && p_352038_.toString().endsWith(extension)
            )) {
            stream.forEach(p_352044_ -> {
                try {
                    output.accept(ResourceLocation.fromNamespaceAndPath(namespace, function.apply(this.relativize(folder, p_352044_))));
                } catch (ResourceLocationException resourcelocationexception) {
                    LOGGER.error("Invalid location while listing folder {} contents", folder, resourcelocationexception);
                }
            });
        } catch (IOException ioexception) {
            LOGGER.error("Failed to list folder {} contents", folder, ioexception);
        }
    }

    private String relativize(Path root, Path path) {
        return root.relativize(path).toString().replace(File.separator, "/");
    }

    private Optional<StructureTemplate> loadFromSnbt(ResourceLocation id, Path p_path) {
        if (!Files.isDirectory(p_path)) {
            return Optional.empty();
        } else {
            Path path = FileUtil.createPathToResource(p_path, id.getPath(), ".snbt");

            try {
                Optional optional;
                try (BufferedReader bufferedreader = Files.newBufferedReader(path)) {
                    String s = IOUtils.toString(bufferedreader);
                    optional = Optional.of(this.readStructure(NbtUtils.snbtToStructure(s)));
                }

                return optional;
            } catch (NoSuchFileException nosuchfileexception) {
                return Optional.empty();
            } catch (CommandSyntaxException | IOException ioexception) {
                LOGGER.error("Couldn't load structure from {}", path, ioexception);
                return Optional.empty();
            }
        }
    }

    private Optional<StructureTemplate> load(StructureTemplateManager.InputStreamOpener inputStream, Consumer<Throwable> onError) {
        try {
            Optional optional;
            try (
                InputStream inputstream = inputStream.open();
                InputStream inputstream1 = new FastBufferedInputStream(inputstream);
            ) {
                optional = Optional.of(this.readStructure(inputstream1));
            }

            return optional;
        } catch (FileNotFoundException filenotfoundexception) {
            return Optional.empty();
        } catch (Throwable throwable1) {
            onError.accept(throwable1);
            return Optional.empty();
        }
    }

    private StructureTemplate readStructure(InputStream stream) throws IOException {
        CompoundTag compoundtag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        return this.readStructure(compoundtag);
    }

    public StructureTemplate readStructure(CompoundTag nbt) {
        StructureTemplate structuretemplate = new StructureTemplate();
        int i = NbtUtils.getDataVersion(nbt, 500);
        structuretemplate.load(this.blockLookup, DataFixTypes.STRUCTURE.updateToCurrentVersion(this.fixerUpper, nbt, i));
        return structuretemplate;
    }

    public boolean save(ResourceLocation id) {
        Optional<StructureTemplate> optional = this.structureRepository.get(id);
        if (optional.isEmpty()) {
            return false;
        } else {
            StructureTemplate structuretemplate = optional.get();
            Path path = this.createAndValidatePathToGeneratedStructure(id, ".nbt");
            Path path1 = path.getParent();
            if (path1 == null) {
                return false;
            } else {
                try {
                    Files.createDirectories(Files.exists(path1) ? path1.toRealPath() : path1);
                } catch (IOException ioexception) {
                    LOGGER.error("Failed to create parent directory: {}", path1);
                    return false;
                }

                CompoundTag compoundtag = structuretemplate.save(new CompoundTag());

                try {
                    try (OutputStream outputstream = new FileOutputStream(path.toFile())) {
                        NbtIo.writeCompressed(compoundtag, outputstream);
                    }

                    return true;
                } catch (Throwable throwable1) {
                    return false;
                }
            }
        }
    }

    public Path createAndValidatePathToGeneratedStructure(ResourceLocation location, String extension) {
        if (location.getPath().contains("//")) {
            throw new ResourceLocationException("Invalid resource path: " + location);
        } else {
            try {
                Path path = this.generatedDir.resolve(location.getNamespace());
                Path path1 = path.resolve("structures");
                Path path2 = FileUtil.createPathToResource(path1, location.getPath(), extension);
                if (path2.startsWith(this.generatedDir) && FileUtil.isPathNormalized(path2) && FileUtil.isPathPortable(path2)) {
                    return path2;
                } else {
                    throw new ResourceLocationException("Invalid resource path: " + path2);
                }
            } catch (InvalidPathException invalidpathexception) {
                throw new ResourceLocationException("Invalid resource path: " + location, invalidpathexception);
            }
        }
    }

    public void remove(ResourceLocation id) {
        this.structureRepository.remove(id);
    }

    @FunctionalInterface
    interface InputStreamOpener {
        InputStream open() throws IOException;
    }

    static record Source(Function<ResourceLocation, Optional<StructureTemplate>> loader, Supplier<Stream<ResourceLocation>> lister) {
    }
}
