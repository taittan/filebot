package com.infott.filebot.util;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimestampConverter {

    public static String convertTimestampFormat(String input) throws ParseException {
        String originalFormatPattern = "dd-MMM-yy hh.mm.ss.SSSSSSSSS a";
        SimpleDateFormat originalFormat = new SimpleDateFormat(originalFormatPattern, Locale.ENGLISH);

        String targetFormatPattern = "dd-MM-yy HH.mm.ss.SSSSSSSSS";
        SimpleDateFormat targetFormat = new SimpleDateFormat(targetFormatPattern);

        int startIndex = input.indexOf('\'') + 1;
        int endIndex = input.indexOf('\'', startIndex);
        String dateTimePart = input.substring(startIndex, endIndex);

        Date date = originalFormat.parse(dateTimePart);

        String formattedDate = targetFormat.format(date);

        String output = input.substring(0, startIndex) + formattedDate + input.substring(endIndex);

        // replace AM/PM and convert as 24H format
        output = output.replace("DD-MON-RR HH.MI.SSXFF AM", "DD-mm-RR HH24.MI.SSXFF");
        output = output.replace("DD-MON-RR HH.MI.SSXFF PM", "DD-mm-RR HH24.MI.SSXFF");

        return output;
    }

    public static void main(String[] args) {
        try {
            String input = "to_timestamp('06-JUL-24 11.12.01.800000000 AM', 'DD-MON-RR HH.MI.SSXFF AM')";
            String output = convertTimestampFormat(input);
            System.out.println("Converted: " + output);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}

