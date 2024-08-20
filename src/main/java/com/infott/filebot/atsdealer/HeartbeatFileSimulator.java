package com.infott.filebot.atsdealer;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class HeartbeatFileSimulator {

    public static void main(String[] args) {
        String inputFileName = "D:\\bat\\hsi1_20240819.txt";
        String outputFileName = "D:\\bat\\AU1_TEMP_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".txt";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Hong_Kong"));
        Date currentDate = new Date();

        try (BufferedReader br = new BufferedReader(new FileReader(inputFileName))) {

            String line;
            while ((line = br.readLine()) != null) {
                int yearIndex = line.indexOf("2024");
                if (yearIndex == -1 || yearIndex + 23 > line.length()) {
                    writeLineToFile(outputFileName, line);
                    continue;
                }

                String dateTimePart = line.substring(yearIndex, yearIndex + 23);
                
                String todayDate = new SimpleDateFormat("yyyy/MM/dd").format(currentDate);
                String modifiedDateTimePart = dateTimePart.replaceFirst("\\d{4}/\\d{2}/\\d{2}", todayDate);
                String modifiedLine = line.substring(0, yearIndex) + modifiedDateTimePart + line.substring(yearIndex + 23);

                Date modifiedLogDate;
                try {
                    modifiedLogDate = dateFormat.parse(modifiedDateTimePart);
                } catch (Exception e) {
                    writeLineToFile(outputFileName, line);
                    continue;
                }

                long timeDifference = modifiedLogDate.getTime() - currentDate.getTime();

                if (timeDifference < -10000) {
                    continue;
                } else if (timeDifference <= 10000) {
                    writeLineToFile(outputFileName, modifiedLine);
                } else {
                    while (timeDifference > 0) {
                        Thread.sleep(250);
                        currentDate = new Date();
                        timeDifference = modifiedLogDate.getTime() - currentDate.getTime();
                    }

                    writeLineToFile(outputFileName, modifiedLine);
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void writeLineToFile(String fileName, String line) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
            bw.write(line);
            bw.newLine();
            bw.flush();
            System.out.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


