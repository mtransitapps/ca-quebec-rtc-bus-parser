package org.mtransit.parser.ca_quebec_rtc_bus;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// http://rtcquebec.ca/Default.aspx?tabid=192
// http://rtcquebec.ca/Portals/0/Admin/DonneesOuvertes/googletransit.zip
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
		System.out.printf("\nGenerating RTC bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating RTC bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
			return Long.valueOf(gRoute.getRouteShortName()); // using route short name as route ID
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		matcher.find();
		long digits = Long.parseLong(matcher.group());
		if (gRoute.getRouteShortName().endsWith("a")) {
			return 10000l + digits;
		} else if (gRoute.getRouteShortName().endsWith("b")) {
			return 20000l + digits;
		} else if (gRoute.getRouteShortName().endsWith("g")) {
			return 70000l + digits;
		} else if (gRoute.getRouteShortName().endsWith("h")) {
			return 80000l + digits;
		}
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	public static final Pattern NULL = Pattern.compile("([\\- ]*null[ \\-]*)", Pattern.CASE_INSENSITIVE);
	public static final String NULL_REPLACEMENT = "";

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

	private static final String COLOR_008AC9 = "008AC9";

	private static final String RSN_21 = "21";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (RSN_21.equals(gRoute.getRouteShortName())) {
			return COLOR_008AC9;
		}
		return super.getRouteColor(gRoute);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabelFR(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		return gStop.getStopId(); // using stop ID as stop code
	}
}
