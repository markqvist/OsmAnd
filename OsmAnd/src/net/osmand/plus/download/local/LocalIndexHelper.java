package net.osmand.plus.download.local;


import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.IndexConstants.BINARY_DEPTH_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.FONT_INDEX_DIR;
import static net.osmand.IndexConstants.FONT_INDEX_EXT;
import static net.osmand.IndexConstants.GEOTIFF_DIR;
import static net.osmand.IndexConstants.HEIGHTMAP_INDEX_DIR;
import static net.osmand.IndexConstants.HEIGHTMAP_SQLITE_EXT;
import static net.osmand.IndexConstants.MAPS_PATH;
import static net.osmand.IndexConstants.NAUTICAL_INDEX_DIR;
import static net.osmand.IndexConstants.ROADS_INDEX_DIR;
import static net.osmand.IndexConstants.SRTM_INDEX_DIR;
import static net.osmand.IndexConstants.TIF_EXT;
import static net.osmand.IndexConstants.TILES_INDEX_DIR;
import static net.osmand.IndexConstants.VOICE_INDEX_DIR;
import static net.osmand.IndexConstants.WEATHER_EXT;
import static net.osmand.IndexConstants.WEATHER_FORECAST_DIR;
import static net.osmand.IndexConstants.WIKIVOYAGE_INDEX_DIR;
import static net.osmand.IndexConstants.WIKI_INDEX_DIR;
import static net.osmand.plus.download.local.LocalItemType.DEPTH_DATA;
import static net.osmand.plus.download.local.LocalItemType.FONT_DATA;
import static net.osmand.plus.download.local.LocalItemType.MAP_DATA;
import static net.osmand.plus.download.local.LocalItemType.OTHER;
import static net.osmand.plus.download.local.LocalItemType.ROAD_DATA;
import static net.osmand.plus.download.local.LocalItemType.TERRAIN_DATA;
import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;
import static net.osmand.plus.download.local.LocalItemType.TTS_VOICE_DATA;
import static net.osmand.plus.download.local.LocalItemType.VOICE_DATA;
import static net.osmand.plus.download.local.LocalItemType.WEATHER_DATA;
import static net.osmand.plus.download.local.LocalItemType.WIKI_AND_TRAVEL_MAPS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.voice.JsMediaCommandPlayer;
import net.osmand.plus.voice.JsTtsCommandPlayer;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class LocalIndexHelper {

	private final OsmandApplication app;
	private final ResourceManager resourceManager;

	public LocalIndexHelper(@NonNull OsmandApplication app) {
		this.app = app;
		resourceManager = app.getResourceManager();
	}

	@NonNull
	public Map<CategoryType, LocalCategory> loadAllFilesByCategories() {
		Map<CategoryType, LocalCategory> categories = new TreeMap<>();
		collectFiles(categories, getInternalDir(), false);
		collectFiles(categories, getExternalDir(), true);
		return categories;
	}

	@NonNull
	private File getInternalDir() {
		File filesDir = app.getFilesDir();
		File parentDir = filesDir.getParentFile();
		return parentDir != null ? parentDir : filesDir;
	}

	@NonNull
	private File getExternalDir() {
		File appDir = app.getAppPath(null);
		File parentDir = appDir.getParentFile();
		return parentDir != null ? parentDir : appDir;
	}

	private void collectFiles(@NonNull Map<CategoryType, LocalCategory> categories, @NonNull File dir, boolean addUnknown) {
		File[] listFiles = dir.listFiles();
		if (!Algorithms.isEmpty(listFiles)) {
			for (File file : listFiles) {
				addFile(categories, file, addUnknown);

				if (file.isDirectory()) {
					collectFiles(categories, file, addUnknown);
				}
			}
		}
	}

	private void addFile(@NonNull Map<CategoryType, LocalCategory> categories, @NonNull File file, boolean addUnknown) {
		LocalItemType itemType = LocalItemType.getItemType(app, file);
		if (itemType != null && (itemType != OTHER || addUnknown)) {
			CategoryType categoryType = itemType.getCategoryType();
			LocalCategory category = categories.get(categoryType);
			if (category == null) {
				category = new LocalCategory(categoryType);
				categories.put(categoryType, category);
			}
			LocalItem item = new LocalItem(file, itemType);
			LocalItemUtils.updateItem(app, item);
			category.addLocalItem(item);
		}
	}

	private void collectLocalItems(@NonNull List<LocalItem> items, @NonNull LocalItemType type,
	                               @NonNull String downloadName, boolean backuped) {
		String name = Algorithms.capitalizeFirstLetterAndLowercase(downloadName);
		if (type == MAP_DATA) {
			addLocalItem(items, type, name, MAPS_PATH, BINARY_MAP_INDEX_EXT, backuped);
		} else if (type == ROAD_DATA) {
			addLocalItem(items, type, name, ROADS_INDEX_DIR, BINARY_ROAD_MAP_INDEX_EXT, backuped);
		} else if (type == WIKI_AND_TRAVEL_MAPS) {
			addLocalItem(items, type, name, WIKI_INDEX_DIR, BINARY_WIKI_MAP_INDEX_EXT, backuped);
			addLocalItem(items, type, name, WIKIVOYAGE_INDEX_DIR, BINARY_WIKIVOYAGE_MAP_INDEX_EXT, backuped);
		} else if (type == DEPTH_DATA) {
			addLocalItem(items, type, name, NAUTICAL_INDEX_DIR, BINARY_DEPTH_MAP_INDEX_EXT, backuped);
		} else if (type == WEATHER_DATA) {
			addLocalItem(items, type, name, WEATHER_FORECAST_DIR, WEATHER_EXT, backuped);
		} else if (type == TERRAIN_DATA) {
			addLocalItem(items, type, name, GEOTIFF_DIR, TIF_EXT, backuped);
			addLocalItem(items, type, name, SRTM_INDEX_DIR, BINARY_SRTM_MAP_INDEX_EXT, backuped);
		}
	}

	private void addLocalItem(@NonNull List<LocalItem> items, @NonNull LocalItemType type, @NonNull String name,
	                          @NonNull String dirName, @NonNull String extension, boolean backuped) {
		dirName = backuped ? BACKUP_INDEX_DIR : dirName;
		File file = app.getAppPath(dirName + name + extension);
		if (file.exists()) {
			loadLocalData(file, type, items, true, null);
		}
	}

	@NonNull
	public List<LocalItem> getLocalItems(@NonNull String downloadName) {
		List<LocalItem> items = new ArrayList<>();

		collectLocalItems(items, downloadName, false);
		collectLocalItems(items, downloadName, true);

		return items;
	}

	private void collectLocalItems(@NonNull List<LocalItem> items, @NonNull String downloadName, boolean backuped) {
		for (LocalItemType type : getSuggestedItemTypes()) {
			collectLocalItems(items, type, downloadName, backuped);
		}
	}

	@NonNull
	public List<LocalItemType> getSuggestedItemTypes() {
		List<LocalItemType> types = new ArrayList<>();
		types.add(MAP_DATA);
		types.add(ROAD_DATA);
		types.add(TERRAIN_DATA);
		types.add(WIKI_AND_TRAVEL_MAPS);
		types.add(DEPTH_DATA);
		types.add(WEATHER_DATA);
		return types;
	}

	@NonNull
	public List<LocalItem> getLocalIndexItems(boolean readFiles, boolean shouldUpdate,
	                                          @Nullable AbstractLoadLocalIndexTask task,
	                                          @NonNull LocalItemType... types) {
		List<LocalItem> items = new ArrayList<>();
		Map<String, File> indexFiles = resourceManager.getIndexFiles();

		boolean voicesCollected = false;
		for (LocalItemType type : types) {
			switch (type) {
				case WIKI_AND_TRAVEL_MAPS:
					loadDataImpl(app.getAppPath(WIKI_INDEX_DIR), WIKI_AND_TRAVEL_MAPS, BINARY_MAP_INDEX_EXT,
							readFiles, shouldUpdate, items, indexFiles, task);
					loadDataImpl(app.getAppPath(WIKIVOYAGE_INDEX_DIR), WIKI_AND_TRAVEL_MAPS, BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT,
							readFiles, shouldUpdate, items, indexFiles, task);
					break;
				case MAP_DATA:
					loadObfData(app.getAppPath(MAPS_PATH), items, readFiles, shouldUpdate, indexFiles, task);
					break;
				case ROAD_DATA:
					loadObfData(app.getAppPath(ROADS_INDEX_DIR), items, readFiles, shouldUpdate, indexFiles, task);
					break;
				case TILES_DATA:
					loadTilesData(app.getAppPath(TILES_INDEX_DIR), items, shouldUpdate, task);
					loadTilesData(app.getAppPath(HEIGHTMAP_INDEX_DIR), items, shouldUpdate, task);
					break;
				case TTS_VOICE_DATA:
				case VOICE_DATA:
					if (!voicesCollected) {
						loadVoiceData(app.getAppPath(VOICE_INDEX_DIR), items, readFiles, shouldUpdate, indexFiles, task);
						voicesCollected = true;
					}
					break;
				case FONT_DATA:
					loadFontData(app.getAppPath(FONT_INDEX_DIR), items, readFiles, shouldUpdate, indexFiles, task);
					break;
				case DEPTH_DATA:
					loadDataImpl(app.getAppPath(NAUTICAL_INDEX_DIR), DEPTH_DATA, BINARY_MAP_INDEX_EXT,
							readFiles, shouldUpdate, items, indexFiles, task);
					break;
				case WEATHER_DATA:
					loadDataImpl(app.getAppPath(WEATHER_FORECAST_DIR), WEATHER_DATA, WEATHER_EXT,
							readFiles, shouldUpdate, items, indexFiles, task);
					break;
				case TERRAIN_DATA:
					loadDataImpl(app.getAppPath(GEOTIFF_DIR), TERRAIN_DATA, TIF_EXT,
							readFiles, shouldUpdate, items, indexFiles, task);
					loadDataImpl(app.getAppPath(SRTM_INDEX_DIR), TERRAIN_DATA, BINARY_MAP_INDEX_EXT,
							readFiles, shouldUpdate, items, indexFiles, task);
					break;
			}
		}
		return items;
	}

	@NonNull
	public List<LocalItem> getLocalFullMaps(@Nullable AbstractLoadLocalIndexTask task) {
		List<LocalItem> results = new ArrayList<>();
		List<LocalItem> roadOnlyList = new ArrayList<>();

		Map<String, File> indexFiles = resourceManager.getIndexFiles();
		loadObfData(app.getAppPath(MAPS_PATH), results, true, true, indexFiles, task);
		loadObfData(app.getAppPath(ROADS_INDEX_DIR), roadOnlyList, true, true, indexFiles, task);

		LocalItemUtils.addUnique(results, roadOnlyList);

		return results;
	}

	public void loadVoiceData(@NonNull File dir, @NonNull List<LocalItem> items, boolean readFiles,
	                          boolean shouldUpdate, @NonNull Map<String, File> indexFiles,
	                          @Nullable AbstractLoadLocalIndexTask task) {
		if (readFiles && dir.canRead()) {
			File[] files = listFilesSorted(dir);
			if (files.length > 0) {
				loadVoiceDataImpl(files, items, shouldUpdate, task);
			}
		} else {
			List<File> voiceFiles = new ArrayList<>();
			for (File file : indexFiles.values()) {
				if (dir.getPath().equals(file.getParent())) {
					voiceFiles.add(file);
				}
			}
			if (voiceFiles.size() > 0) {
				Collections.sort(voiceFiles);
				loadVoiceDataImpl(voiceFiles.toArray(new File[0]), items, shouldUpdate, task);
			}
		}
	}

	private void loadVoiceDataImpl(@NonNull File[] files, @NonNull List<LocalItem> items,
	                               boolean shouldUpdate, @Nullable AbstractLoadLocalIndexTask task) {
		List<File> voiceFilesList = new ArrayList<>(Arrays.asList(files));
		//First list TTS files, they are preferred
		Iterator<File> iterator = voiceFilesList.iterator();
		while (iterator.hasNext()) {
			File voiceFile = iterator.next();
			if (voiceFile.isDirectory() && (JsTtsCommandPlayer.isMyData(voiceFile))) {
				loadLocalData(voiceFile, TTS_VOICE_DATA, items, shouldUpdate, task);
				iterator.remove();
			}
		}
		//Now list recorded voices
		for (File voiceFile : voiceFilesList) {
			if (voiceFile.isDirectory() && (JsMediaCommandPlayer.isMyData(voiceFile))) {
				loadLocalData(voiceFile, VOICE_DATA, items, shouldUpdate, task);
			}
		}
	}

	private void loadFontData(@NonNull File dir, @NonNull List<LocalItem> items, boolean readFiles,
	                          boolean shouldUpdate, @NonNull Map<String, File> indexFiles, @Nullable AbstractLoadLocalIndexTask task) {
		loadDataImpl(dir, FONT_DATA, FONT_INDEX_EXT, readFiles, shouldUpdate, items, indexFiles, task);
	}

	private void loadTilesData(@NonNull File dir, @NonNull List<LocalItem> items,
	                           boolean shouldUpdate, @Nullable AbstractLoadLocalIndexTask task) {
		if (dir.canRead()) {
			for (File file : listFilesSorted(dir)) {
				if (file.isFile()) {
					String fileName = file.getName();
					boolean tilesData = Algorithms.endsWithAny(fileName, SQLiteTileSource.EXT, HEIGHTMAP_SQLITE_EXT);
					if (tilesData) {
						loadLocalData(file, TILES_DATA, items, shouldUpdate, task);
					}
				} else if (file.isDirectory()) {
					loadLocalData(file, TILES_DATA, items, shouldUpdate, task);
				}
			}
		}
	}

	@NonNull
	private File[] listFilesSorted(@NonNull File dir) {
		File[] listFiles = dir.listFiles();
		if (listFiles == null) {
			return new File[0];
		}
		Arrays.sort(listFiles);
		return listFiles;
	}

	private void loadObfData(@NonNull File dir, @NonNull List<LocalItem> items, boolean readFiles,
	                         boolean shouldUpdate, @NonNull Map<String, File> indexFiles,
	                         @Nullable AbstractLoadLocalIndexTask task) {
		boolean readDir = readFiles && dir.canRead();
		List<File> files = readDir ? Arrays.asList(listFilesSorted(dir)) : new ArrayList<>(indexFiles.values());
		for (File file : files) {
			if (file.isFile() && file.getName().endsWith(BINARY_MAP_INDEX_EXT)
					&& (!readDir || dir.getPath().equals(file.getParent()))) {
				LocalItemType type = LocalItemType.getItemType(app, file);
				if (type != null) {
					loadLocalData(file, type, items, shouldUpdate, task);
				}
			}
		}
	}

	private void loadDataImpl(@NonNull File dir, @NonNull LocalItemType type, @NonNull String extension,
	                          boolean readFiles, boolean shouldUpdate, @NonNull List<LocalItem> items,
	                          @NonNull Map<String, File> indexFiles, @Nullable AbstractLoadLocalIndexTask task) {
		boolean readDir = readFiles && dir.canRead();
		List<File> files = readDir ? Arrays.asList(listFilesSorted(dir)) : new ArrayList<>(indexFiles.values());
		for (File file : files) {
			if (file.isFile() && file.getName().endsWith(extension)
					&& (!readDir || file.getPath().startsWith(dir.getPath()))) {
				loadLocalData(file, type, items, shouldUpdate, task);
			}
		}
	}

	private void loadLocalData(@NonNull File file, @NonNull LocalItemType type, @NonNull List<LocalItem> items,
	                           boolean shouldUpdate, @Nullable AbstractLoadLocalIndexTask task) {
		LocalItem item = new LocalItem(file, type);
		if (shouldUpdate) {
			LocalItemUtils.updateItem(app, item);
		}
		items.add(item);

		if (task != null) {
			task.loadFile(item);
		}
	}
}