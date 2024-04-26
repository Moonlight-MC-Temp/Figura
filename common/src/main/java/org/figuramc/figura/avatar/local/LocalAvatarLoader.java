package org.figuramc.figura.avatar.local;

import net.minecraft.Util;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.commons.lang3.concurrent.Computable;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.parsers.AvatarMetadataParser;
import org.figuramc.figura.parsers.BlockbenchModelParser;
import org.figuramc.figura.parsers.LuaScriptParser;
import org.figuramc.figura.utils.FiguraResourceListener;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.IOUtils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.GZIPOutputStream;

/**
 * class used to load avatars from a file
 * and used for hot-swapping
 */
public class LocalAvatarLoader {

	public static final boolean IS_WINDOWS = Util.getPlatform() == Util.OS.WINDOWS;
	private static final HashMap<Path, WatchKey> KEYS = new HashMap<>();

	private static CompletableFuture<Void> tasks;
	private static Path lastLoadedPath;
	private static LoadState loadState = LoadState.UNKNOWN;
	private static String loadError;

	private static WatchService watcher;

	public static final HashMap<ResourceLocation, CompoundTag> CEM_AVATARS = new HashMap<>();
	public static final FiguraResourceListener AVATAR_LISTENER = FiguraResourceListener.createResourceListener("cem", manager -> {
		CEM_AVATARS.clear();
		AvatarManager.clearCEMAvatars();

		for (Map.Entry<ResourceLocation, Resource> cem : manager.listResources("cem", location -> location.getNamespace().equals(FiguraMod.MOD_ID) && location.getPath().endsWith(".nbt")).entrySet()) {
			// id
			ResourceLocation key = cem.getKey();
			String[] split = key.getPath().split("/");
			if (split.length <= 1)
				continue;

			String namespace = split[split.length - 2];
			String path = split[split.length - 1];
			ResourceLocation id = new ResourceLocation(namespace, path.substring(0, path.length() - 5));

			// nbt
			CompoundTag nbt;
			try {
				nbt = NbtIo.readCompressed(cem.getValue().open());
			} catch (Exception e) {
				FiguraMod.LOGGER.error("Failed to load " + id + " avatar", e);
				continue;
			}

			// insert
			FiguraMod.LOGGER.info("Loaded CEM model for " + id);
			CEM_AVATARS.put(id, nbt);
		}
	});

	static {
		try {
			watcher = FileSystems.getDefault().newWatchService();
		} catch (Exception e) {
			FiguraMod.LOGGER.error("Failed to initialize the watcher service", e);
		}
	}

	protected static void async(Runnable toRun) {
		if (tasks == null || tasks.isDone()) {
			tasks = CompletableFuture.runAsync(toRun);
		} else {
			tasks.cancel(true);
			tasks.thenRun(toRun);
		}
	}

	/**
	 * Loads an NbtCompound from the specified path
	 *
	 * @param path - the file/folder for loading the avatar
	 */
	public static void loadAvatar(Path path, UserData target) {
		loadError = null;
		loadState = LoadState.UNKNOWN;
		resetWatchKeys();
		try {
			path = path == null ? null : path.getFileSystem() == FileSystems.getDefault() ? Path.of(path.toFile().getCanonicalPath()) : path.normalize();
		} catch (IOException e) {
		}
		lastLoadedPath = path;

		if (path == null || target == null)
			return;

		addWatchKey(path, KEYS::put);

		Path finalPath = path;
		async(() -> {
			try {
				// try to load a moon and return early
				final var moonPath = finalPath.resolve("avatar.moon");
				if (Files.exists(moonPath)) {
					target.loadAvatar(NbtIo.readCompressed(moonPath.toFile()));
					return;
				}

				// load as folder
				CompoundTag nbt = new CompoundTag();

				// scripts
				loadState = LoadState.SCRIPTS;
				loadScripts(finalPath, nbt);

				loadGlobalScripts(nbt);
				// custom sounds
				loadState = LoadState.SOUNDS;
				loadSounds(finalPath, nbt);

				// models
				CompoundTag textures = new CompoundTag();
				ListTag animations = new ListTag();
				BlockbenchModelParser modelParser = new BlockbenchModelParser();

				loadState = LoadState.MODELS;
				CompoundTag models = loadModels(finalPath, finalPath, modelParser, textures, animations, "");
				models.putString("name", "models");

				// metadata
				loadState = LoadState.METADATA;
				String _meta = IOUtils.readFile(finalPath.resolve("avatar.json"));
				var metadata = AvatarMetadataParser.read(_meta);

				CompoundTag metaNBT = AvatarMetadataParser.parse(metadata,_meta, IOUtils.getFileNameOrEmpty(finalPath));
				nbt.put("metadata", metaNBT);
				metaNBT.putString("uuid",target.id.toString());

				AvatarMetadataParser.injectToModels(metadata, models);
				AvatarMetadataParser.injectToTextures(metadata, textures);

				if (!models.isEmpty()) nbt.put("models", models);
				if (!textures.isEmpty()) nbt.put("textures", textures);
				if (!animations.isEmpty()) nbt.put("animations", animations);
				CompoundTag metadataTag = nbt.getCompound("metadata");
				if (metadataTag.contains("resources_paths")) {
					loadResources(nbt, metadataTag.getList("resources_paths", Tag.TAG_STRING), finalPath);
					metadataTag.remove("resource_paths");
				}

				// Output moon for debugging
				if (Configs.DUMP_MOONS.value)
					NbtIo.writeCompressed(nbt, finalPath.resolve("compiled.moon").toFile());

				// load
				target.loadAvatar(nbt);
			} catch (Throwable e) {
				loadError = e.getMessage();
				FiguraMod.LOGGER.error("Failed to load avatar from " + finalPath, e);
				FiguraToast.sendToast(FiguraText.of("toast.load_error"), FiguraText.of("gui.load_error." + LocalAvatarLoader.getLoadState()), FiguraToast.ToastType.ERROR);
			}
		});
	}
	private static void loadResources(CompoundTag nbt, ListTag pathsTag, Path parentPath) {
		ArrayList<PathMatcher> pathMatchers = new ArrayList<>();
		FileSystem fs = FileSystems.getDefault();
		for (int i = 0; i < pathsTag.size(); i++) {
			pathMatchers.add(fs.getPathMatcher("glob:".concat(pathsTag.getString(i))));
		}
		Map<String, Path> pathMap = new HashMap<>();
		matchPathsRecursive(pathMap, parentPath, parentPath, pathMatchers);
		CompoundTag resourcesTag = new CompoundTag();
		for (String p:pathMap.keySet()) {
			try (FileInputStream fis = new FileInputStream(pathMap.get(p).toFile())) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				GZIPOutputStream gos = new GZIPOutputStream(baos);
				int i;
				while ((i = fis.read()) != -1) {
					gos.write(i);
				}
				gos.close();
				resourcesTag.put(unixifyPath(p), new ByteArrayTag(baos.toByteArray()));
				baos.close();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		nbt.put("resources", resourcesTag);
	}

	private static String unixifyPath(String original) {
		Path p = Path.of(original);
		String[] components = new String[p.getNameCount()];
		for (int i = 0; i < components.length; i++) {
			components[i] = p.getName(i).toString();
		}
		return String.join("/", components);
	}

	private static void matchPathsRecursive(Map<String, Path> pathMap, Path parent, Path current, ArrayList<PathMatcher> matchers) {
		File f = current.toFile();
		if (f.isFile()) {
			Path relative = parent.toAbsolutePath().relativize(current.toAbsolutePath()).normalize();
			for (PathMatcher m : matchers) {
				if (m.matches(relative)) {
					pathMap.put(relative.toString(), current);
					break;
				}
			}
		}
		else {
			for (File fl :
					current.toFile().listFiles()) {
				matchPathsRecursive(pathMap, parent, fl.toPath(), matchers);
			}
		}
	}

	private static void loadScripts(Path path, CompoundTag nbt) throws IOException {
		List<Path> scripts = IOUtils.getFilesByExtension(path, ".lua");
		if (scripts.size() < 0) return;
		CompoundTag scriptsNbt = new CompoundTag();

		String pathRegex = path.toString().isEmpty() ? "\\Q\\E" : Pattern.quote(path + path.getFileSystem().getSeparator());
		for (Path script : scripts) {
			String name = script.toString()
					.replaceFirst(pathRegex, "")
					.replaceAll("[/\\\\]", ".");
			name = name.substring(0, name.length() - 4);
			scriptsNbt.put(name, LuaScriptParser.parseScript(name, IOUtils.readFile(script)));
		}
		nbt.put("scripts",scriptsNbt);


	}
	private static void loadGlobalScripts(CompoundTag nbt) throws IOException {
		if (!Configs.USE_GLOBAL_SCRIPTS.value) return;
		Path path = IOUtils.getOrCreateDir(FiguraMod.getFiguraDirectory(),"global_scripts");
		addWatchKey(path, KEYS::put);
		List<Path> scripts = IOUtils.getFilesByExtension(path, ".lua");
		if (scripts.size() < 0) return;
		CompoundTag scriptsNbt = nbt.getCompound("scripts");
		if (scriptsNbt == null){
			nbt.put("scripts",scriptsNbt = new CompoundTag());
		}
		String pathRegex = path.toString().isEmpty() ? "\\Q\\E" : Pattern.quote(path + path.getFileSystem().getSeparator());
		for (Path script : scripts) {
			String name = script.toString()
					.replaceFirst(pathRegex, "")
					.replaceAll("[/\\\\]", ".");
			name = name.substring(0, name.length() - 4);
			scriptsNbt.put("global." +name, LuaScriptParser.parseScript(name, IOUtils.readFile(script)));
		}

	}

	private static void loadSounds(Path path, CompoundTag nbt) throws IOException {
		List<Path> sounds = IOUtils.getFilesByExtension(path, ".ogg");
		if (sounds.size() > 0) {
			CompoundTag soundsNbt = new CompoundTag();
			String pathRegex = Pattern.quote(path.toString().isEmpty() ? path.toString() : path + path.getFileSystem().getSeparator());
			for (Path sound : sounds) {
				String name = sound.toString()
						.replaceFirst(pathRegex, "")
						.replaceAll("[/\\\\]", ".");
				name = name.substring(0, name.length() - 4);
				soundsNbt.putByteArray(name, IOUtils.readFileBytes(sound));
			}
			nbt.put("sounds", soundsNbt);
		}
	}

	private static CompoundTag loadModels(Path avatarFolder, Path currentFile, BlockbenchModelParser parser, CompoundTag textures, ListTag animations, String folders) throws Exception {
		CompoundTag result = new CompoundTag();
		List<Path> subFiles = IOUtils.listPaths(currentFile);
		ListTag children = new ListTag();
		if (subFiles != null)
			for (Path file : subFiles) {
				if (IOUtils.isHidden(file))
					continue;
				String name = IOUtils.getFileNameOrEmpty(file);
				if (Files.isDirectory(file)) {
					CompoundTag subfolder = loadModels(avatarFolder, file, parser, textures, animations, folders + name + ".");
					if (!subfolder.isEmpty()) {
						subfolder.putString("name", name);
						BlockbenchModelParser.parseParent(name, subfolder);
						children.add(subfolder);
					}
				} else if (file.toString().toLowerCase(Locale.US).endsWith(".bbmodel")) {
					BlockbenchModelParser.ModelData data = parser.parseModel(avatarFolder, file, IOUtils.readFile(file), name.substring(0, name.length() - 8), folders);
					children.add(data.modelNbt());
					animations.addAll(data.animationList());

					CompoundTag dataTag = data.textures();
					if (dataTag.isEmpty())
						continue;

					if (textures.isEmpty()) {
						textures.put("data", new ListTag());
						textures.put("src", new CompoundTag());
					}

					textures.getList("data", Tag.TAG_COMPOUND).addAll(dataTag.getList("data", Tag.TAG_COMPOUND));
					textures.getCompound("src").merge(dataTag.getCompound("src"));
				}
			}

		if (!children.isEmpty())
			result.put("chld", children);

		return result;
	}
	public static Matcher ValidFileMatcher = Pattern.compile(".*(avatar.json|(\\.lua|\\.bbmodel|\\.ogg|\\.png))$").matcher("");
	/**
	 * Tick the watched key for hotswapping avatars
	 */
	public static void tick() {
		WatchEvent<?> event = null;
		try{

			if(IS_WINDOWS){ // This literally just removes one unix-only check, but it prevents some useless looping :3

				var entries = KEYS.entrySet();
				for (Map.Entry<Path, WatchKey> entry : entries) {
					if(entry == null) continue;
					WatchKey key = entry.getValue();
					if (!key.isValid())
						continue;

					for (WatchEvent<?> watchEvent : key.pollEvents()) {
						if (watchEvent.kind() == StandardWatchEventKinds.OVERFLOW)
							continue;

						event = watchEvent;
						Path path = entry.getKey().resolve((Path) event.context());
						String name = IOUtils.getFileNameOrEmpty(path);

						if (IOUtils.isHidden(path) || !(Files.isDirectory(path) || ValidFileMatcher.reset(name).matches()))
							continue;
						FiguraMod.debug("Detected file changes in the Avatar directory (" + event.context().toString() + "), reloading!");
						AvatarManager.loadLocalAvatar(lastLoadedPath);
						return;
					}
				}
				return;
			}
			boolean reload = false;
			var entries = KEYS.entrySet();
			for (Map.Entry<Path, WatchKey> entry : entries) {
				if(entry == null) continue;
				WatchKey key = entry.getValue();
				if (!key.isValid())
					continue;

				for (WatchEvent<?> watchEvent : key.pollEvents()) {
					WatchEvent.Kind<?> kind = watchEvent.kind();
					if (kind == StandardWatchEventKinds.OVERFLOW)
						continue;

					event = watchEvent;
					Path path = entry.getKey().resolve((Path) event.context());
					String name = IOUtils.getFileNameOrEmpty(path);

					if (IOUtils.isHidden(path) || !(Files.isDirectory(path) || ValidFileMatcher.reset(name).matches()))
						continue;

					// This is it, this is the Unix-only check. I(superpowers04) dunno why only Unix needs to add paths like this
					if (kind == StandardWatchEventKinds.ENTRY_CREATE) 
						addWatchKey(path, KEYS::put);

					reload = true;

				}
			}

			// reload avatar
			if (reload) {
				FiguraMod.debug("Detected file changes in the Avatar directory (" + event.context().toString() + "), reloading!");
				AvatarManager.loadLocalAvatar(lastLoadedPath);
			}
		}catch(java.util.ConcurrentModificationException meow){
			FiguraMod.debug("LocalAvatarLoader.java:tick java.util.ConcurrentModificationException ignored");
		}
	}

	public static void resetWatchKeys() {
		lastLoadedPath = null;
		var values = KEYS.values();
		for (WatchKey key : values)
			key.cancel();
		KEYS.clear();
	}

	/**
	 * register new watch keys
	 *
	 * @param path the path to register the watch key
	 * @param consumer a consumer that will process the watch key and its path
	 */
	protected static void addWatchKey(Path path, BiConsumer<Path, WatchKey> consumer) {
		if (watcher == null || path == null || path.getFileSystem() != FileSystems.getDefault())
			return;

		if (!Files.isDirectory(path) || IOUtils.isHidden(path))
			return;

		try {
			WatchEvent.Kind<?>[] events = {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY};
			WatchKey key = IS_WINDOWS ? path.register(watcher, events, com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE) : path.register(watcher, events);

			consumer.accept(path, key);

			List<Path> children = IOUtils.listPaths(path);
			if (children == null || IS_WINDOWS)
				return;

			for (Path child : children)
				addWatchKey(child, consumer);
		} catch (Exception e) {
			FiguraMod.LOGGER.error("Failed to register watcher for " + path, e);
		}
	}

	public static Path getLastLoadedPath() {
		return lastLoadedPath;
	}

	public static String getLoadState() {
		return loadState.name().toLowerCase(Locale.US);
	}

	public static String getLoadError() {
		return loadError;
	}

	private enum LoadState {
		UNKNOWN,
		SCRIPTS,
		SOUNDS,
		MODELS,
		METADATA
	}
}
