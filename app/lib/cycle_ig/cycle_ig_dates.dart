class CycleIgDates {
  static DateTime dateOnly(DateTime date) =>
      DateTime(date.year, date.month, date.day);

  static bool isWithin(DateTime date, DateTime start, DateTime end) {
    final day = dateOnly(date);
    return !day.isBefore(dateOnly(start)) && !day.isAfter(dateOnly(end));
  }

  static String isoDate(DateTime date) {
    final day = dateOnly(date);
    final month = day.month.toString().padLeft(2, '0');
    final dom = day.day.toString().padLeft(2, '0');
    return '${day.year}-$month-$dom';
  }

  static String compactDate(DateTime date) => isoDate(date).replaceAll('-', '');

  static String displayDate(DateTime date) {
    final day = dateOnly(date);
    final month = day.month.toString().padLeft(2, '0');
    final dom = day.day.toString().padLeft(2, '0');
    return '$month/$dom/${day.year}';
  }

  static DateTime min(DateTime a, DateTime b) => a.isBefore(b) ? a : b;
}
