package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.local.LocalItemType.MAP_DATA;
import static net.osmand.plus.download.local.LocalItemType.PROFILES;
import static net.osmand.plus.download.local.LocalItemType.RENDERING_STYLES;
import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;
import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.CLEAR_TILES_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;
import static net.osmand.plus.settings.fragments.ExportSettingsFragment.SELECTED_TYPES;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.utils.UiUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemMenuProvider implements MenuProvider {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final DownloadActivity activity;
	private final LocalBaseFragment fragment;
	private final Map<String, IndexItem> itemsToUpdate = new HashMap<>();
	private final boolean nightMode;

	private LocalItem localItem;

	@ColorRes
	private int colorId;
	private boolean showInfoItem = true;

	public ItemMenuProvider(@NonNull DownloadActivity activity, @NonNull LocalBaseFragment fragment) {
		this.activity = activity;
		this.fragment = fragment;
		this.nightMode = fragment.isNightMode();
		app = activity.getMyApplication();
		uiUtilities = app.getUIUtilities();
	}

	public void setColorId(@ColorRes int colorId) {
		this.colorId = colorId;
	}

	public void setLocalItem(@NonNull LocalItem localItem) {
		this.localItem = localItem;
	}

	public void setShowInfoItem(boolean showInfoItem) {
		this.showInfoItem = showInfoItem;
	}

	public void showMenu(@NonNull View view) {
		PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
		Menu menu = popupMenu.getMenu();
		if (menu instanceof MenuBuilder) {
			((MenuBuilder) menu).setOptionalIconsVisible(true);
		}
		MenuCompat.setGroupDividerEnabled(menu, true);

		onCreateMenu(menu, new MenuInflater(view.getContext()));
		popupMenu.show();
	}

	@Override
	public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		MenuItem menuItem = null;
		if (showInfoItem) {
			menuItem = menu.add(0, 0, Menu.NONE, R.string.info_button);
			menuItem.setIcon(getIcon(R.drawable.ic_action_info_outlined, colorId));
			menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menuItem.setOnMenuItemClickListener(item -> {
				FragmentManager manager = fragment.getFragmentManager();
				if (manager != null) {
					LocalItemFragment.showInstance(manager, localItem, fragment);
				}
				return true;
			});
		}
		LocalItemType type = localItem.getType();
		if (type == MAP_DATA) {
			menuItem = menu.add(0, R.string.shared_string_update, Menu.NONE, R.string.shared_string_update);
			menuItem.setIcon(getIcon(R.drawable.ic_action_update, colorId));
			menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menuItem.setOnMenuItemClickListener(item -> {
				updateItem();
				return true;
			});
		} else if (type == RENDERING_STYLES) {
			menuItem = menu.add(0, R.string.shared_string_export, Menu.NONE, R.string.shared_string_export);
			menuItem.setIcon(getIcon(R.drawable.ic_action_upload, colorId));
			menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menuItem.setOnMenuItemClickListener(item -> {
				exportItem(ExportSettingsType.CUSTOM_ROUTING);
				return true;
			});
		} else if (type == TILES_DATA) {
			Object object = localItem.getAttachedObject();
			if ((object instanceof TileSourceTemplate) || ((object instanceof SQLiteTileSource)
					&& ((SQLiteTileSource) object).couldBeDownloadedFromInternet())) {
				menuItem = menu.add(0, R.string.shared_string_edit, Menu.NONE, R.string.shared_string_edit);
				menuItem.setIcon(getIcon(R.drawable.ic_action_edit_outlined, colorId));
				menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				menuItem.setOnMenuItemClickListener(item -> {
					OsmandRasterMapsPlugin.defineNewEditLayer(activity, fragment, localItem.getFile().getName());
					return true;
				});
			}
			if ((object instanceof ITileSource) && ((ITileSource) object).couldBeDownloadedFromInternet()) {
				menuItem = menu.add(0, R.string.clear_tile_data, Menu.NONE, R.string.clear_tile_data);
				menuItem.setIcon(getIcon(R.drawable.ic_action_clear_all, colorId));
				menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				menuItem.setOnMenuItemClickListener(item -> {
					clearTiles();
					return true;
				});
			}
			menuItem = menu.add(0, R.string.shared_string_export, Menu.NONE, R.string.shared_string_export);
			menuItem.setIcon(getIcon(R.drawable.ic_action_upload, colorId));
			menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menuItem.setOnMenuItemClickListener(item -> {
				exportItem(ExportSettingsType.MAP_SOURCES);
				return true;
			});
		}
		boolean backuped = localItem.isBackuped();
		if (type == MAP_DATA || backuped) {
			addOperationItem(menu, backuped ? RESTORE_OPERATION : BACKUP_OPERATION);
		}
		if (type != PROFILES) {
			menuItem = menu.add(1, R.string.shared_string_remove, Menu.NONE, R.string.shared_string_remove);
			menuItem.setIcon(getIcon(R.drawable.ic_action_delete_outlined, colorId));
			menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menuItem.setOnMenuItemClickListener(item -> {
				FragmentManager manager = fragment.getFragmentManager();
				if (manager != null) {
					DeleteConfirmationBottomSheet.showInstance(manager, fragment, localItem);
				}
				return true;
			});
		}
	}

	private void addOperationItem(@NonNull Menu menu, @NonNull OperationType type) {
		MenuItem menuItem = menu.add(0, type.getTitleId(), Menu.NONE, type.getTitleId());
		menuItem.setIcon(getIcon(type.getIconId(), colorId));
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menuItem.setOnMenuItemClickListener(item -> {
			performOperation(type);
			return true;
		});
	}

	public void performOperation(@NonNull OperationType type) {
		LocalOperationTask task = new LocalOperationTask(app, type, fragment);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, localItem);
	}

	private void exportItem(@NonNull ExportSettingsType settingsType) {
		List<File> selectedFiles = new ArrayList<>();
		selectedFiles.add(localItem.getFile());

		HashMap<ExportSettingsType, List<?>> selectedTypes = new HashMap<>();
		selectedTypes.put(settingsType, selectedFiles);

		Bundle bundle = new Bundle();
		bundle.putSerializable(SELECTED_TYPES, selectedTypes);
		MapActivity.launchMapActivityMoveToTop(activity, null, null, bundle);

	}

	private void clearTiles() {
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			LocalOperationTask task = new LocalOperationTask(app, CLEAR_TILES_OPERATION, fragment);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, localItem);
		});
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setMessage(app.getString(R.string.clear_confirmation_msg, localItem.getName()));
		builder.show();
	}

	private void updateItem() {
		File file = localItem.getFile();
		IndexItem indexItem = itemsToUpdate.get(file.getName());
		if (indexItem != null) {
			activity.startDownload(indexItem);
		} else {
			String text = app.getString(R.string.map_is_up_to_date, localItem.getName());
			Snackbar snackbar = Snackbar.make(activity.getLayout(), text, Snackbar.LENGTH_LONG);
			UiUtilities.setupSnackbar(snackbar, nightMode, 5);
			snackbar.show();
		}
	}

	public void reloadItemsToUpdate() {
		itemsToUpdate.clear();
		for (IndexItem item : app.getDownloadThread().getIndexes().getItemsToUpdate()) {
			itemsToUpdate.put(item.getTargetFileName(), item);
		}
	}

	@Override
	public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
		return false;
	}

	@Nullable
	private Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return uiUtilities.getIcon(id, colorId);
	}
}