package com.infott.filebot.util;
import java.util.ArrayList;
import java.util.List;

public class SqlColumnExtractor {
    public static void main(String[] args) {
        String sql = "select ordnum, to_char(orddate, 'yyyy/mm/dd hh24:mi:ss.ff3') as orddate, pic, to_char(effdate, 'yyyy/mm/dd hh24:mi:ss.ff3') as effdate from stk_ord";
        String[] columns = extractColumns(sql);
        for (String column : columns) {
            System.out.println(column);
        }
    }

    public static String[] extractColumns(String sql) {
        String lowerCaseSql = sql.toLowerCase();
        int selectIndex = lowerCaseSql.indexOf("select") + 6;
        int fromIndex = lowerCaseSql.indexOf("from");

        String columnsPart = sql.substring(selectIndex, fromIndex).trim();

        List<String> columns = new ArrayList<>();
        StringBuilder column = new StringBuilder();
        int bracketLevel = 0;

        for (char ch : columnsPart.toCharArray()) {
            if (ch == ',' && bracketLevel == 0) {
                columns.add(column.toString().trim());
                column.setLength(0);
            } else {
                if (ch == '(') bracketLevel++;
                if (ch == ')') bracketLevel--;
                column.append(ch);
            }
        }
        if (column.length() > 0) {
            columns.add(column.toString().trim());
        }

        // remove the part before AS
        List<String> finalColumns = new ArrayList<>();
        for (String col : columns) {
            String[] parts = col.split("\\s+as\\s+", 2);
            if (parts.length == 2) {
                finalColumns.add(parts[1].trim());
            } else {
                finalColumns.add(parts[0].trim());
            }
        }

        return finalColumns.toArray(new String[0]);
    }
}

