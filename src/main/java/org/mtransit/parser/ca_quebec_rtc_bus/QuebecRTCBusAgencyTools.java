package org.mtransit.parser.ca_quebec_rtc_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://www.rtcquebec.ca/donnees-ouvertes
// https://cdn.rtcquebec.ca/Site_Internet/DonneesOuvertes/googletransit.zip
public class QuebecRTCBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-quebec-rtc-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new QuebecRTCBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating RTC bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating RTC bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			long digits = Long.parseLong(matcher.group());
			if (gRoute.getRouteShortName().endsWith("a")) {
				return 10_000L + digits;
			} else if (gRoute.getRouteShortName().endsWith("b")) {
				return 20_000L + digits;
			} else if (gRoute.getRouteShortName().endsWith("g")) {
				return 70_000L + digits;
			} else if (gRoute.getRouteShortName().endsWith("h")) {
				return 80_000L + digits;
			}
		}
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		String routeShortName = gRoute.getRouteShortName();
		return routeShortName.toUpperCase(Locale.FRENCH); // USED BY REAL-TIME API
	}

	private static final Pattern NULL = Pattern.compile("([\\- ]*null[ \\-]*)", Pattern.CASE_INSENSITIVE);
	private static final String NULL_REPLACEMENT = "";

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		if (StringUtils.isEmpty(routeLongName)) {
			routeLongName = gRoute.getRouteDescOrDefault(); // using route description as route long name
		}
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_PARENTHESIS1.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_PARENTHESIS1_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_PARENTHESIS2.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_PARENTHESIS2_REPLACEMENT);
		routeLongName = NULL.matcher(routeLongName).replaceAll(NULL_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "A3C614";

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final int REAL_TIME_API_N = 0;
	private static final int REAL_TIME_API_S = 1;
	private static final int REAL_TIME_API_O = 2;
	private static final int REAL_TIME_API_E = 3;

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) { // DIRECTION ID USED BY REAL-TIME API
		int directionId;
		final String tripHeadsign1 = gTrip.getTripHeadsignOrDefault();
		if (tripHeadsign1.endsWith(" (Nord)")) {
			directionId = REAL_TIME_API_N; // DIRECTION ID USED BY REAL-TIME API
		} else if (tripHeadsign1.endsWith(" (Sud)")) {
			directionId = REAL_TIME_API_S; // DIRECTION ID USED BY REAL-TIME API
		} else if (tripHeadsign1.endsWith(" (Est)")) {
			directionId = REAL_TIME_API_O; // DIRECTION ID USED BY REAL-TIME API
		} else if (tripHeadsign1.endsWith(" (Ouest)")) {
			directionId = REAL_TIME_API_E; // DIRECTION ID USED BY REAL-TIME API
		} else {
			throw new MTLog.Fatal("Unexpected trip head-sign '%s'!", gTrip);
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				directionId
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern ANCIENNE_ = CleanUtils.cleanWordsFR("l'ancienne", "ancienne");
	private static final String ANCIENNE_REPLACEMENT = CleanUtils.cleanWordsReplacement("Anc");
	private static final Pattern CEGER_ = CleanUtils.cleanWordsFR("cégep", "cegep");
	private static final String CEGERP_REPLACEMENT = CleanUtils.cleanWordsReplacement("Cgp");
	private static final Pattern ECOLE_SECONDAIRE_ = CleanUtils.cleanWordsFR("École Secondaire", "École Sec");
	private static final String ECOLE_SECONDAIRE_REPLACEMENT = CleanUtils.cleanWordsReplacement("ES");
	private static final Pattern UNIVERSITE_LAVAL_ = CleanUtils.cleanWordsFR("U Laval", "U. Laval", "Univ.Laval", "Univ. Laval", "Université Laval");
	private static final String UNIVERSITE_LAVAL_REPLACEMENT = CleanUtils.cleanWordsReplacement("U Laval");

	private static final Pattern ENDS_WITH_N_ = Pattern.compile("(^(.*)( \\(nord\\))$)", Pattern.CASE_INSENSITIVE);
	private static final String ENDS_WITH_N_REPLACEMENT = "N-$2";
	private static final Pattern ENDS_WITH_S_ = Pattern.compile("(^(.*)( \\(sud\\))$)", Pattern.CASE_INSENSITIVE);
	private static final String ENDS_WITH_S_REPLACEMENT = "S-$2";
	private static final Pattern ENDS_WITH_E_ = Pattern.compile("(^(.*)( \\(est\\))$)", Pattern.CASE_INSENSITIVE);
	private static final String ENDS_WITH_E_REPLACEMENT = "E-$2";
	private static final Pattern ENDS_WITH_O_ = Pattern.compile("(^(.*)( \\(ouest\\))$)", Pattern.CASE_INSENSITIVE);
	private static final String ENDS_WITH_O_REPLACEMENT = "O-$2";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = ENDS_WITH_N_.matcher(tripHeadsign).replaceAll(ENDS_WITH_N_REPLACEMENT);
		tripHeadsign = ENDS_WITH_S_.matcher(tripHeadsign).replaceAll(ENDS_WITH_S_REPLACEMENT);
		tripHeadsign = ENDS_WITH_E_.matcher(tripHeadsign).replaceAll(ENDS_WITH_E_REPLACEMENT);
		tripHeadsign = ENDS_WITH_O_.matcher(tripHeadsign).replaceAll(ENDS_WITH_O_REPLACEMENT);
		tripHeadsign = ANCIENNE_.matcher(tripHeadsign).replaceAll(ANCIENNE_REPLACEMENT);
		tripHeadsign = CEGER_.matcher(tripHeadsign).replaceAll(CEGERP_REPLACEMENT);
		tripHeadsign = ECOLE_SECONDAIRE_.matcher(tripHeadsign).replaceAll(ECOLE_SECONDAIRE_REPLACEMENT);
		tripHeadsign = UNIVERSITE_LAVAL_.matcher(tripHeadsign).replaceAll(UNIVERSITE_LAVAL_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(Locale.FRENCH, tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.cleanBounds(Locale.FRENCH, gStopName);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		//noinspection deprecation
		return gStop.getStopId(); // using stop ID as stop code
	}
}
