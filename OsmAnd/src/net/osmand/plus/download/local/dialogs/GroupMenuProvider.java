package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.download.local.LocalItemType.DEPTH_DATA;
import static net.osmand.plus.download.local.LocalItemType.MAP_DATA;
import static net.osmand.plus.download.local.LocalItemType.PROFILES;
import static net.osmand.plus.download.local.LocalItemType.TERRAIN_DATA;
import static net.osmand.plus.download.local.LocalItemType.WIKI_AND_TRAVEL_MAPS;
import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.download.ui.SearchDialogFragment;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.settings.enums.MapsSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GroupMenuProvider implements MenuProvider {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final DownloadActivity activity;
	private final LocalItemsFragment fragment;
	private final LocalGroup group;
	private final boolean nightMode;

	public GroupMenuProvider(@NonNull DownloadActivity activity, @NonNull LocalItemsFragment fragment) {
		this.activity = activity;
		this.fragment = fragment;
		this.group = fragment.getGroup();
		this.nightMode = fragment.isNightMode();
		app = activity.getMyApplication();
		uiUtilities = app.getUIUtilities();
	}

	@Override
	public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();

		boolean selectionMode = fragment.isSelectionMode();
		int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);

		if (!selectionMode) {
			MenuItem searchItem = menu.add(0, R.string.shared_string_search, 0, R.string.shared_string_search);
			searchItem.setIcon(getIcon(R.drawable.ic_action_search_dark, colorId));
			searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			searchItem.setOnMenuItemClickListener(item -> {
				activity.showDialog(activity, SearchDialogFragment.createInstance(""));
				return true;
			});
		}
		LocalItemType type = group.getType();
		if (type == MAP_DATA) {
			MapsSortMode sortMode = app.getSettings().LOCAL_MAPS_SORT_MODE.get();
			MenuItem sortItem = menu.add(0, R.string.shared_string_sort, 0, R.string.shared_string_sort);
			sortItem.setIcon(getIcon(sortMode.getIconId(), colorId));
			sortItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			sortItem.setOnMenuItemClickListener(item -> {
				FragmentManager manager = activity.getSupportFragmentManager();
				SortMapsBottomSheet.showInstance(manager, fragment);
				return true;
			});
		}
		MenuItem actionsItem = menu.add(0, R.string.shared_string_more, 0, R.string.shared_string_more);
		actionsItem.setIcon(getIcon(R.drawable.ic_overflow_menu_white, colorId));
		actionsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		actionsItem.setOnMenuItemClickListener(item -> {
			showAdditionalActions(activity.findViewById(item.getItemId()));
			return true;
		});
	}

	private void showAdditionalActions(@NonNull View view) {
		List<PopUpMenuItem> items = new ArrayList<>();

		LocalItemType type = group.getType();
		boolean selectionMode = fragment.isSelectionMode();
		if (selectionMode) {
			if (type != PROFILES) {
				addOperationItem(items, DELETE_OPERATION);
			}
			if (Algorithms.equalsToAny(type, MAP_DATA, DEPTH_DATA, TERRAIN_DATA, WIKI_AND_TRAVEL_MAPS)) {
				addOperationItem(items, BACKUP_OPERATION);
				addOperationItem(items, RESTORE_OPERATION);
			}
		} else {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_select)
					.setIcon(getContentIcon(R.drawable.ic_action_select_all))
					.setOnClickListener(v -> fragment.setSelectionMode(true))
					.create());

			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_import)
					.setIcon(getContentIcon(R.drawable.ic_action_import))
					.setOnClickListener(v -> {
						Intent intent = ImportHelper.getImportFileIntent();
						AndroidUtils.startActivityForResultIfSafe(fragment, intent, IMPORT_FILE_REQUEST);
					})
					.showTopDivider(true)
					.create());
		}

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void addOperationItem(@NonNull List<PopUpMenuItem> items, @NonNull OperationType type) {
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(type.getTitleId())
				.setIcon(getContentIcon(type.getIconId()))
				.setOnClickListener(v -> {
					ItemsSelectionHelper<LocalItem> helper = fragment.getSelectionHelper();
					if (helper.hasSelectedItems()) {
						showConfirmation(type);
					} else {
						showNoItemsToast(app.getString(type.getTitleId()));
					}
				})
				.create());
	}

	private void showConfirmation(@NonNull OperationType type) {
		String action = app.getString(type.getTitleId());
		ItemsSelectionHelper<LocalItem> helper = fragment.getSelectionHelper();
		Set<LocalItem> selectedItems = helper.getSelectedItems();

		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
		builder.setMessage(app.getString(R.string.local_index_action_do, action.toLowerCase(), String.valueOf(helper.getSelectedItemsSize())));
		builder.setPositiveButton(action, (dialog, which) -> {
			fragment.performOperation(type, selectedItems.toArray(new LocalItem[0]));
			fragment.setSelectionMode(false);
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	private void showNoItemsToast(@NonNull String action) {
		String message = app.getString(R.string.local_index_no_items_to_do, action.toLowerCase());
		app.showShortToastMessage(Algorithms.capitalizeFirstLetter(message));
	}

	@Override
	public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
		return false;
	}

	@Nullable
	private Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return uiUtilities.getIcon(id, colorId);
	}

	@Nullable
	private Drawable getContentIcon(@DrawableRes int id) {
		return uiUtilities.getThemedIcon(id);
	}
}