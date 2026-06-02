package com.jsmacrosce.jsmacros.core.extensions;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.Pair;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

public class ExtensionLoader {
    private final Set<Extension> extensions = new HashSet<>();
    private final Set<LanguageExtension> languageExtensions = new HashSet<>();
    private final Set<LibraryExtension> libraryExtensions = new HashSet<>();

    private final Core<?, ?> core;

    private ExtensionClassLoader classLoader;

    private LanguageExtension highestPriorityExtension;

    private boolean loadingDone;

    private final Path extPath;

    public ExtensionLoader(Core<?, ?> core) {
        this.core = core;
        this.extPath = core.config.configFolder.toPath().resolve("Extensions");
    }

    public boolean isExtensionLoaded(String name) {
        if (notLoaded()) {
            loadExtensions();
        }
        return extensions.stream().anyMatch(e -> e.getExtensionName().equals(name));
    }

    public boolean notLoaded() {
        return !loadingDone;
    }

    public LanguageExtension getHighestPriorityExtension() {
        if (notLoaded()) {
            loadExtensions();
        }
        if (highestPriorityExtension == null) {
            highestPriorityExtension = languageExtensions.stream().max(Comparator.comparingInt(LanguageExtension::getPriority)).orElse(null);
        }
        return highestPriorityExtension;
    }

    public Set<Extension> getAllExtensions() {
        if (notLoaded()) {
            loadExtensions();
        }
        return extensions;
    }

    public Set<LanguageExtension> getAllLanguageExtensions() {
        if (notLoaded()) {
            loadExtensions();
        }
        return languageExtensions;
    }

    public Set<LibraryExtension> getAllLibraryExtensions() {
        if (notLoaded()) {
            loadExtensions();
        }
        return libraryExtensions;
    }

    public @Nullable LanguageExtension getExtensionForFile(File file) {
        if (notLoaded()) {
            loadExtensions();
        }
        List<Pair<LanguageExtension.ExtMatch, LanguageExtension>> extensions = this.languageExtensions.stream().map(e -> new Pair<>(e.extensionMatch(file), e)).filter(p -> p.getT().isMatch()).collect(Collectors.toList());
        if (extensions.size() > 1) {
            List<Pair<LanguageExtension.ExtMatch, LanguageExtension>> extensionsByName = extensions.stream().filter(p -> p.getT() == LanguageExtension.ExtMatch.MATCH_WITH_NAME).collect(Collectors.toList());
            if (!extensionsByName.isEmpty()) {
                extensionsByName.sort(Comparator.comparingInt(e -> -e.getU().getPriority()));
                return extensionsByName.get(0).getU();
            }
        }
        if (!extensions.isEmpty()) {
            extensions.sort(Comparator.comparingInt(e -> -e.getU().getPriority()));
            return extensions.get(0).getU();
        }
        return null;
    }

    public Extension getExtensionForName(String extName) {
        if (notLoaded()) {
            loadExtensions();
        }
        return extensions.stream().filter(e -> e.getExtensionName().equals(extName)).findFirst().orElse(null);
    }

    public synchronized void loadExtensions() {
        if (classLoader != null) {
            System.err.println("Extensions already loaded");
            return;
        }
        if (!Files.exists(extPath)) {
            try {
                Files.createDirectories(extPath);
            } catch (Exception e) {
                throw new RuntimeException("Could not create LanguageExtensions directory", e);
            }
        }

        URL[] urls;
        try (Stream<Path> files = Files.list(extPath)) {
            urls = files.filter(Files::isRegularFile).map(e -> {
                try {
                    return e.toUri().toURL();
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }
            }).toArray(URL[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        classLoader = new ExtensionClassLoader(urls);

        // extract lib to dependencies folder
        Path dependenciesPath = extPath.resolve("tmp");
        try {
            Files.createDirectories(dependenciesPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // add internal extensions
        Set<URL> internalExtensions = Extension.getDependenciesInternal(ExtensionLoader.class, "jsmacrosce.extension.json");
        for (URL lib : internalExtensions) {
            System.out.println("Adding internal extension: " + lib);
            // copy resource to dependencies folder
            Path path = dependenciesPath.resolve(lib.getPath().substring(lib.getPath().lastIndexOf('/') + 1));
            try (InputStream stream = lib.openStream()) {
                Files.write(path, stream.readAllBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                System.out.println("Extracted dependency " + path);
                classLoader.addURL(path.toUri().toURL());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // classpath extensions: bundled-internal (graal) + drop-in jars in the Extensions folder
        Set<Extension> classpathExtensions = ServiceLoader.load(Extension.class, classLoader)
            .stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toSet());

        // companion-mod extensions contributed via the "jsmacros" Fabric entrypoint
        List<Extension> entrypointExtensions = loadEntrypointExtensions();

        // entrypoint extensions win over a same-named drop-in jar
        List<Extension> combined = new ArrayList<>(entrypointExtensions);
        combined.addAll(classpathExtensions);
        extensions.addAll(dedupeByName(combined));

        System.out.println("Loaded " + extensions.size() + " extensions");

        // load extension deps — entrypoint extensions ship their runtime via Fabric jar-in-jar,
        // so only classpath extensions that survived dedup need extraction here
        for (Extension extension : classpathExtensions) {
            if (!extensions.contains(extension)) continue;
            try {
                Set<URL> deps = extension.getDependencies();
                if (deps.isEmpty()) {
                    System.out.println("No dependencies for extension: " + extension.getClass().getName());
                }
                for (URL lib : deps) {
                    // copy resource to dependencies folder
                    Path path = dependenciesPath.resolve(lib.getPath().substring(lib.getPath().lastIndexOf('/') + 1));
                    try (InputStream stream = lib.openStream()) {
                        Files.write(path, stream.readAllBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                        System.out.println("Extracted dependency " + path);
                        classLoader.addURL(path.toUri().toURL());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load extension dependencies for: " + extension.getExtensionName(), e);
            }
        }
        Thread.currentThread().setContextClassLoader(classLoader);
        for (Extension extension : extensions) {
            try {
                extension.init(core);
                if (extension instanceof LibraryExtension libExt) {
                    libraryExtensions.add(libExt);
                    for (Class<? extends BaseLibrary> lib : libExt.getLibraries()) {
                        core.libraryRegistry.addLibrary(lib);
                    }
                }
                if (extension instanceof LanguageExtension langExt) {
                    languageExtensions.add(langExt);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load extension: " + extension.getExtensionName(), e);
            }
        }
        loadingDone = true;
    }

    public boolean isGuestObject(Object obj) {
        if (notLoaded()) {
            loadExtensions();
        }
        return languageExtensions.stream().anyMatch(e -> e.isGuestObject(obj));
    }

    static List<Extension> dedupeByName(Collection<Extension> discovered) {
        Map<String, Extension> byName = new LinkedHashMap<>();
        for (Extension ext : discovered) {
            if (byName.putIfAbsent(ext.getExtensionName(), ext) != null) {
                System.out.println("Skipping duplicate extension: " + ext.getExtensionName());
            }
        }
        return new ArrayList<>(byName.values());
    }

    private List<Extension> loadEntrypointExtensions() {
        List<Extension> result = new ArrayList<>();
        for (EntrypointContainer<Extension> container :
                FabricLoader.getInstance().getEntrypointContainers("jsmacros", Extension.class)) {
            try {
                result.add(container.getEntrypoint());
            } catch (Throwable t) {
                System.err.println("Failed to load jsmacros extension entrypoint from "
                        + container.getProvider().getMetadata().getId());
                t.printStackTrace();
            }
        }
        return result;
    }

}
