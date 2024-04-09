import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;

public class HtmlParser {
    public static void main(String[] args) {
        int count = -1;
        // saveCsv("https://pd2-hw3.netdb.csie.ncku.edu.tw/");
        while (count != saveCsv("https://pd2-hw3.netdb.csie.ncku.edu.tw/")) {
            if (count == -1)
                count = saveCsv("https://pd2-hw3.netdb.csie.ncku.edu.tw/");
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

    }

    private static int saveCsv(String url) {
        int day = -1;
        try {
            Document doc = Jsoup.connect(url).get();
            String title = doc.title();
            day = Integer.parseInt(title.substring(3, title.length()));
            System.out.println(day);
            Elements rows = doc.select("table").first().select("tr");

            Elements names = rows.get(0).select("th");
            Elements datas = rows.get(1).select("td");

            String nameStr = "";
            String dataStr = "";

            for (int i = 0; i < names.size(); i++) {
                Element name = names.get(i);

                String spilt = ",";
                if (i == names.size() - 1)
                    spilt = "\n";

                nameStr += name.text() + spilt;
                dataStr += datas.get(i).text() + spilt;

            }

            ArrayList<String> data = openCSV.readCSV("data.csv");
            System.out.println(data.size());
            data.set(day, dataStr);
            if (data.get(0).equals("\n"))
                data.set(0, nameStr);

            openCSV.writeCSV(data, "data.csv");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return day;
    }

}

class openCSV {
    public static ArrayList<String> readCSV(String CsvName) {

        ArrayList<String> data = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(CsvName))) {
            String line;
            while ((line = br.readLine()) != null)
                data.add(line + "\n");
        } catch (IOException e) {
            for (int i = 0; i <= 30; i++)
                data.add("\n");
        }

        return data;
    }

    public static DataFrame readCSV_value(String CsvName) {
        ArrayList<String> textData = readCSV(CsvName);
        ArrayList<String> names = new ArrayList<String>(Arrays.asList(textData.get(0).split(",")));
        ArrayList<ArrayList<Double>> datas = new ArrayList<>();

        for (int i = 1; i <= 30; i++) {
            String[] oneDayData = textData.get(i).split(",");
            ArrayList<Double> data = new ArrayList<>();
            for (String s : oneDayData)
                data.add(Double.parseDouble(s));
        }

        DataFrame df = new DataFrame(names, datas);

        return df;

    }

    public static void writeCSV(ArrayList<String> data, String CsvName) {
        try (FileWriter writer = new FileWriter(CsvName)) {
            for (String s : data)
                writer.append(s);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class DataFrame {
    ArrayList<String> names = new ArrayList<>();
    ArrayList<ArrayList<Double>> datas = new ArrayList<ArrayList<Double>>();

    public DataFrame(ArrayList<String> names, ArrayList<ArrayList<Double>> datas) {
        this.names = names;
        this.datas = datas;
    }

}