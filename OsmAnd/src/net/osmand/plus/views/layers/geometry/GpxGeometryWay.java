package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.router.RouteSegmentResult;

import java.util.List;

import androidx.annotation.NonNull;

public class GpxGeometryWay extends MultiColoringGeometryWay<GpxGeometryWayContext, GpxGeometryWayDrawer> {

	private List<WptPt> points;
	private List<RouteSegmentResult> routeSegments;

	private static class GeometryWayWptPtProvider implements GeometryWayProvider {
		private final List<WptPt> points;

		public GeometryWayWptPtProvider(@NonNull List<WptPt> points) {
			this.points = points;
		}

		@Override
		public double getLatitude(int index) {
			return points.get(index).getLatitude();
		}

		@Override
		public double getLongitude(int index) {
			return points.get(index).getLongitude();
		}

		@Override
		public int getSize() {
			return points.size();
		}
	}

	public GpxGeometryWay(GpxGeometryWayContext context) {
		super(context, new GpxGeometryWayDrawer(context));
	}

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		return new GeometryArrowsStyle(getContext(), customDirectionArrowColor, customColor, customWidth);
	}

	public void updateSegment(RotatedTileBox tb, List<WptPt> points, List<RouteSegmentResult> routeSegments) {
		if (tb.getMapDensity() != getMapDensity() || this.points != points || this.routeSegments != routeSegments) {
			this.points = points;
			this.routeSegments = routeSegments;

			if (coloringType.isTrackSolid() || coloringType.isGradient()) {
				if (points != null) {
					updateWay(new GeometryWayWptPtProvider(points), tb);
				} else {
					clearWay();
				}
			} else if (coloringType.isRouteInfoAttribute()) {
				if (points != null && routeSegments != null) {
					updateSolidMultiColorRoute(tb, RouteProvider.locationsFromWpts(points), routeSegments);
				} else {
					clearWay();
				}
			}
		}
	}

	@Override
	public void clearWay() {
		if (points != null || routeSegments != null) {
			points = null;
			routeSegments = null;
			super.clearWay();
		}
	}

	public static class GeometryArrowsStyle extends GeometrySolidWayStyle<GpxGeometryWayContext> {

		private static final float TRACK_WIDTH_THRESHOLD_DP = 8f;
		private static final float ARROW_DISTANCE_MULTIPLIER = 1.5f;
		private static final float SPECIAL_ARROW_DISTANCE_MULTIPLIER = 10f;
		private final float TRACK_WIDTH_THRESHOLD_PIX;

		private final Bitmap arrowBitmap;

		public static final int OUTER_CIRCLE_COLOR = 0x33000000;
		protected int directionArrowColor;
		protected int trackColor;
		protected float trackWidth;

		private float outerCircleRadius;
		private float innerCircleRadius;

		GeometryArrowsStyle(GpxGeometryWayContext context, int arrowColor, int trackColor, float trackWidth) {
			this(context, null, arrowColor, trackColor, trackWidth);
			outerCircleRadius = AndroidUtils.dpToPx(context.getCtx(), 8);
			innerCircleRadius = AndroidUtils.dpToPx(context.getCtx(), 7);
		}

		GeometryArrowsStyle(GpxGeometryWayContext context, Bitmap arrowBitmap, int directionArrowColor,
							int trackColor, float trackWidth) {
			super(context, trackColor, trackWidth, directionArrowColor);
			this.arrowBitmap = arrowBitmap;
			this.directionArrowColor = directionArrowColor;
			this.trackColor = trackColor;
			this.trackWidth = trackWidth;
			TRACK_WIDTH_THRESHOLD_PIX = AndroidUtils.dpToPx(context.getCtx(), TRACK_WIDTH_THRESHOLD_DP);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			return other instanceof GeometryArrowsStyle;
		}

		@Override
		public boolean hasPathLine() {
			return false;
		}

		@Override
		public Bitmap getPointBitmap() {
			if (useSpecialArrow()) {
				return getContext().getSpecialArrowBitmap();
			}
			return arrowBitmap != null ? arrowBitmap : getContext().getArrowBitmap();
		}

		@Override
		public Integer getPointColor() {
			return directionArrowColor;
		}

		public int getTrackColor() {
			return trackColor;
		}

		public float getTrackWidth() {
			return trackWidth;
		}

		public float getOuterCircleRadius() {
			return outerCircleRadius;
		}

		public float getInnerCircleRadius() {
			return innerCircleRadius;
		}

		public boolean useSpecialArrow() {
			return trackWidth <= TRACK_WIDTH_THRESHOLD_PIX;
		}

		@Override
		public double getPointStepPx(double zoomCoef) {
			return useSpecialArrow() ?
					getPointBitmap().getHeight() * SPECIAL_ARROW_DISTANCE_MULTIPLIER :
					getPointBitmap().getHeight() + trackWidth * ARROW_DISTANCE_MULTIPLIER;
		}
	}
}