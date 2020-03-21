package org.mtransit.parser.ca_quebec_rtc_bus;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
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

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-quebec-rtc-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new QuebecRTCBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		MTLog.log("Generating RTC bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating RTC bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public long getRouteId(GRoute gRoute) {
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
		MTLog.logFatal("Unexpected route ID for %s!", gRoute);
		return -1L;
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		String routeShortName = gRoute.getRouteShortName();
		return routeShortName.toUpperCase(Locale.FRENCH); // USED BY REAL-TIME API
	}

	private static final Pattern NULL = Pattern.compile("([\\- ]*null[ \\-]*)", Pattern.CASE_INSENSITIVE);
	private static final String NULL_REPLACEMENT = "";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		if (StringUtils.isEmpty(routeLongName)) {
			routeLongName = gRoute.getRouteDesc(); // using route description as route long name
		}
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_PARENTHESE1.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_PARENTHESE1_REPLACEMENT);
		routeLongName = CleanUtils.CLEAN_PARENTHESE2.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_PARENTHESE2_REPLACEMENT);
		routeLongName = NULL.matcher(routeLongName).replaceAll(NULL_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "A3C614";

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		return super.getRouteColor(gRoute);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) { // DIRECTION ID USED BY REAL-TIME API
		int directionId;
		String tripHeadsign;
		if (gTrip.getTripHeadsign().endsWith(" (Nord)")) {
			tripHeadsign = "N-" + gTrip.getTripHeadsign().substring(0, gTrip.getTripHeadsign().length() - 7);
			directionId = 0;
		} else if (gTrip.getTripHeadsign().endsWith(" (Sud)")) {
			tripHeadsign = "S-" + gTrip.getTripHeadsign().substring(0, gTrip.getTripHeadsign().length() - 6);
			directionId = 1;
		} else if (gTrip.getTripHeadsign().endsWith(" (Est)")) {
			tripHeadsign = "E-" + gTrip.getTripHeadsign().substring(0, gTrip.getTripHeadsign().length() - 6);
			directionId = 2;
		} else if (gTrip.getTripHeadsign().endsWith(" (Ouest)")) {
			tripHeadsign = "O-" + gTrip.getTripHeadsign().substring(0, gTrip.getTripHeadsign().length() - 8);
			directionId = 3;
		} else {
			MTLog.logFatal("Unexpected trip head-sign '%s'!", gTrip);
			return;
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(tripHeadsign),
				directionId
		);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		MTLog.logFatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
		return false;
	}

	private static final Pattern ANCIENNE_ = CleanUtils.cleanWords("l'ancienne", "ancienne");
	private static final String ANCIENNE_REPLACEMENT = CleanUtils.cleanWordsReplacement("Anc");
	private static final Pattern CEGER_ = CleanUtils.cleanWords("cégep", "cegep");
	private static final String CEGERP_REPLACEMENT = CleanUtils.cleanWordsReplacement("Cgp");
	private static final Pattern CENTRE_ = CleanUtils.cleanWords("centre", "center");
	private static final String CENTRE_REPLACEMENT = CleanUtils.cleanWordsReplacement("Ctr");
	private static final Pattern ECOLE_SECONDAIRE_ = CleanUtils.cleanWords("École Secondaire", "École Sec");
	private static final String ECOLE_SECONDAIRE_REPLACEMENT = CleanUtils.cleanWordsReplacement("ES");
	private static final Pattern PLACE_ = CleanUtils.cleanWords("place");
	private static final String PLACE_REPLACEMENT = CleanUtils.cleanWordsReplacement("Pl");
	private static final Pattern POINTE_ = CleanUtils.cleanWords("pointe");
	private static final String POINTE_REPLACEMENT = CleanUtils.cleanWordsReplacement("Pte");
	private static final Pattern TERMINUS_ = CleanUtils.cleanWords("terminus");
	private static final String TERMINUS_REPLACEMENT = CleanUtils.cleanWordsReplacement("Term");
	private static final Pattern UNIVERSITE_LAVAL_ = CleanUtils.cleanWords("U Laval", "U. Laval", "Univ.Laval", "Univ. Laval", "Université Laval");
	private static final String UNIVERSITE_LAVAL_REPLACEMENT = CleanUtils.cleanWordsReplacement("U Laval");

	@Override
	public String cleanTripHeadsign(String tripHeadsign) { // KEEP IN SYNC WITH REAL-TIME PROVIDER
		tripHeadsign = ANCIENNE_.matcher(tripHeadsign).replaceAll(ANCIENNE_REPLACEMENT);
		tripHeadsign = CEGER_.matcher(tripHeadsign).replaceAll(CEGERP_REPLACEMENT);
		tripHeadsign = CENTRE_.matcher(tripHeadsign).replaceAll(CENTRE_REPLACEMENT);
		tripHeadsign = ECOLE_SECONDAIRE_.matcher(tripHeadsign).replaceAll(ECOLE_SECONDAIRE_REPLACEMENT);
		tripHeadsign = PLACE_.matcher(tripHeadsign).replaceAll(PLACE_REPLACEMENT);
		tripHeadsign = POINTE_.matcher(tripHeadsign).replaceAll(POINTE_REPLACEMENT);
		tripHeadsign = TERMINUS_.matcher(tripHeadsign).replaceAll(TERMINUS_REPLACEMENT);
		tripHeadsign = UNIVERSITE_LAVAL_.matcher(tripHeadsign).replaceAll(UNIVERSITE_LAVAL_REPLACEMENT);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(GStop gStop) {
		return gStop.getStopId(); // using stop ID as stop code
	}
}
