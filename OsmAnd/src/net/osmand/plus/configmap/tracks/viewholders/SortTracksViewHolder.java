package net.osmand.plus.configmap.tracks.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackTab;
import net.osmand.plus.configmap.tracks.TracksFragment;
import net.osmand.plus.configmap.tracks.TracksSortMode;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

public class SortTracksViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final TextView title;
	private final TextView description;
	private final ImageView imageView;
	private final TracksFragment fragment;
	private final boolean nightMode;

	public SortTracksViewHolder(@NonNull View view, @NonNull TracksFragment fragment, boolean nightMode) {
		super(view);
		this.fragment = fragment;
		this.nightMode = nightMode;
		this.app = (OsmandApplication) view.getContext().getApplicationContext();

		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		imageView = view.findViewById(R.id.icon);
		itemView.setOnClickListener(v -> fragment.showSortByDialog());
	}

	public void bindView(@NonNull TrackTab trackTab) {
		TracksSortMode sortMode = trackTab.getSortMode();
		boolean enabled = !Algorithms.isEmpty(trackTab.getGPXInfos());

		int textColorId = enabled ? ColorUtilities.getActiveColorId(nightMode) : ColorUtilities.getSecondaryTextColorId(nightMode);
		int iconColorÎd = enabled ? ColorUtilities.getActiveIconColorId(nightMode) : ColorUtilities.getSecondaryIconColorId(nightMode);

		title.setTextColor(ColorUtilities.getColor(app, textColorId));
		description.setText(sortMode.getNameId());
		imageView.setImageDrawable(app.getUIUtilities().getIcon(sortMode.getIconId(), iconColorÎd));
		itemView.setEnabled(enabled);
	}
}

