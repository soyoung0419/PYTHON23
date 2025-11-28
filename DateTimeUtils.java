import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

class DateTimeUtils {
    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static LocalDate parseDate(String s) { return LocalDate.parse(s, DATE_FMT); }
    static LocalTime parseTime(String s) { return LocalTime.parse(s, TIME_FMT); }
    static LocalDateTime parseDateTime(String s) { return LocalDateTime.parse(s, DATETIME_FMT); }

    static boolean isSameDay(LocalDate a, LocalDate b) { return Objects.equals(a, b); }

    static DayOfWeek parseKoreanDow(String s) {
        s = s.trim().toUpperCase(Locale.ROOT);
        switch (s) {
            case "MON": case "월": return DayOfWeek.MONDAY;
            case "TUE": case "화": return DayOfWeek.TUESDAY;
            case "WED": case "수": return DayOfWeek.WEDNESDAY;
            case "THU": case "목": return DayOfWeek.THURSDAY;
            case "FRI": case "금": return DayOfWeek.FRIDAY;
            case "SAT": case "토": return DayOfWeek.SATURDAY;
            case "SUN": case "일": return DayOfWeek.SUNDAY;
        }
        throw new IllegalArgumentException("요일 파싱 실패: " + s);
    }
}
