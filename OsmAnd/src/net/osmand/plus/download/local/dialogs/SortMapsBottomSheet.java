package net.osmand.plus.download.local.dialogs;

import static net.osmand.plus.settings.enums.MapsSortMode.DATE_DESCENDING;
import static net.osmand.plus.settings.enums.MapsSortMode.NAME_DESCENDING;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.configmap.tracks.SortByBottomSheet.SortModeViewHolder;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.MapsSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class SortMapsBottomSheet extends MenuBottomSheetDialogFragment {

	private static final String TAG = SortMapsBottomSheet.class.getSimpleName();

	private static final String SORT_MODE_KEY = "sort_mode_key";

	private MapsSortMode mapsSortMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		OsmandApplication app = requiredMyApplication();
		if (savedInstanceState != null) {
			mapsSortMode = AndroidUtils.getSerializable(savedInstanceState, SORT_MODE_KEY, MapsSortMode.class);
		} else {
			mapsSortMode = app.getSettings().LOCAL_MAPS_SORT_MODE.get();
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, nightMode);
		View view = themedInflater.inflate(R.layout.bottom_sheet_track_group_list, null);

		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.sort_by);
		title.setTextColor(ColorUtilities.getSecondaryTextColor(context, nightMode));

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(context));
		recyclerView.setAdapter(new SortModesAdapter());

		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());
	}

	private void setMapsSortMode(@NonNull MapsSortMode sortMode) {
		Fragment target = getTargetFragment();
		if (target instanceof MapsSortModeListener) {
			((MapsSortModeListener) target).setMapsSortMode(sortMode);
		}
	}

	public class SortModesAdapter extends RecyclerView.Adapter<SortModeViewHolder> {

		private final MapsSortMode[] sortModes = MapsSortMode.values();
		private final int activeColorId = ColorUtilities.getActiveIconColorId(nightMode);
		private final int defaultColorId = ColorUtilities.getDefaultIconColorId(nightMode);


		@NonNull
		@Override
		public SortModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			return new SortModeViewHolder(inflater.inflate(R.layout.list_item_two_icons, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull SortModeViewHolder holder, int position) {
			MapsSortMode sortMode = sortModes[position];

			holder.title.setText(sortMode.getNameId());

			boolean selected = sortMode == mapsSortMode;
			int colorId = selected ? activeColorId : defaultColorId;
			holder.groupTypeIcon.setImageDrawable(getIcon(sortMode.getIconId(), colorId));

			holder.itemView.setOnClickListener(view -> {
				int adapterPosition = holder.getAdapterPosition();
				if (adapterPosition != RecyclerView.NO_POSITION) {
					setMapsSortMode(sortModes[position]);
				}
				dismiss();
			});
			AndroidUiHelper.updateVisibility(holder.selectedIcon, selected);
			AndroidUiHelper.updateVisibility(holder.divider, shouldShowDivider(sortMode));
		}

		private boolean shouldShowDivider(@NonNull MapsSortMode mode) {
			return mode == NAME_DESCENDING || mode == DATE_DESCENDING;
		}

		@Override
		public int getItemCount() {
			return sortModes.length;
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(SORT_MODE_KEY, mapsSortMode);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SortMapsBottomSheet fragment = new SortMapsBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public interface MapsSortModeListener {
		void setMapsSortMode(@NonNull MapsSortMode sortMode);
	}
}

