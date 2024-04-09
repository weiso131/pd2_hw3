import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

public class HtmlParser {
    public static void main(String[] args) {
        if (args[0].equals("0"))
            saveCsv("https://pd2-hw3.netdb.csie.ncku.edu.tw/");
        else {
            DataFrame df = openCSV.readCSV_value("data.csv");
            if (args[1].equals("0")) {
                openCSV.writeCSV(openCSV.readCSV("data.csv"), "output.csv");
            } else {
                String stock = args[2];
                int start = Integer.parseInt(args[3]), end = Integer.parseInt(args[4]);
                if (args[1].equals("1"))
                    df.slideMean(5, stock, start, end);
                else if (args[1].equals("2")) {
                    double std = df.rangeStd(stock, start, end);
                    ArrayList<String> output = new ArrayList<>();
                    output.add(String.format("%s,%d,%d\n%.2f", stock, start, end, std));
                    openCSV.writeCSV(output, "output.csv");
                } else if (args[1].equals("3"))
                    df.stdTop3(start, end);
                else if (args[1].equals("4"))
                    df.LinearRegression(stock, start, end);
            }

        }
    }

    private static int saveCsv(String url) {
        int day = -1;
        try {
            Document doc = Jsoup.connect(url).get();
            String title = doc.title();
            day = Integer.parseInt(title.substring(3, title.length()));
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
            datas.add(data);
        }
        return new DataFrame(names, datas);
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

    public Double get(String key, int index) {
        int nameIndex = names.indexOf(key);
        return datas.get(index).get(nameIndex);
    }

    private double MinMax(double x, double y, boolean compare) {
        if (x < y == compare)
            return x;
        else
            return y;
    }

    private double sqrt(double x) {
        double l = MinMax(x, 1, true), r = MinMax(x, 1, false);
        while (r - l > 1e-9) {
            if ((l + r) * (l + r) / 4 > x)
                r = (l + r) / 2;
            else
                l = (l + r) / 2;
        }
        return l;
    }

    private double getMean(String key, int start, int end) {
        double mean = 0;
        for (int i = start - 1; i < end; i++)
            mean += get(key, i) / (end - start + 1);
        return mean;
    }

    public void slideMean(int range, String key, int start, int end) {
        double sum = 0;
        Queue<Double> queue = new LinkedList<Double>();
        ArrayList<String> output = new ArrayList<>();
        String outputData = "";

        for (int i = start - 1; i < end; i++) {
            sum += get(key, i);
            queue.offer(get(key, i));

            if (queue.size() > range)
                sum -= queue.poll();
            if (queue.size() == range) {
                String spilt = ",";
                if (i == end - 1)
                    spilt = "\n";
                outputData += String.format("%.2f", sum / range) + spilt;
            }
        }

        output.add(String.format("%s,%d,%d\n", key, start, end));
        output.add(outputData);
        openCSV.writeCSV(output, "output.csv");
    }

    public double rangeStd(String key, int start, int end) {
        double mean = getMean(key, start, end), std = 0;
        for (int i = start - 1; i < end; i++)
            std += (get(key, i) - mean) * (get(key, i) - mean) / (end - start);
        std = sqrt(std);
        return std;
    }

    public void stdTop3(int start, int end) {
        Double[] top3 = { 0.0, 0.0, 0.0 };
        Integer[] top3Index = { 0, 0, 0 };

        for (int i = 0; i < names.size(); i++) {
            double std = rangeStd(names.get(i), start, end);
            int index = i;
            for (int j = 0; j < 3; j++) {
                if (top3[j] < std) {
                    double save = std;
                    int saveIndex = index;
                    std = top3[j];
                    top3[j] = save;
                    index = top3Index[j];
                    top3Index[j] = saveIndex;
                }
            }
        }
        ArrayList<String> output = new ArrayList<>();
        output.add(String.format("%s,%s,%s,%d,%d\n", names.get(top3Index[0]),
                names.get(top3Index[1]), names.get(top3Index[2]), start, end));
        output.add(String.format("%.2f,%.2f,%.2f", top3[0], top3[1], top3[2]));
        openCSV.writeCSV(output, "output.csv");
    }

    public void LinearRegression(String key, int start, int end) {
        double mean = getMean(key, start, end);
        double timeMean = (start + end) / 2;
        double slope = 0, timeStd = 0, bias = 0;
        for (int t = start; t <= end; t++) {
            slope += (t - timeMean) * (get(key, t - 1) - mean);
            timeStd += (t - timeMean) * (t - timeMean);
        }
        slope /= timeStd;
        bias = mean - slope * timeMean;

        ArrayList<String> output = new ArrayList<>();
        output.add(String.format("%s,%d,%d\n", key, start, end));
        output.add(String.format("%.2f,%.2f\n", slope, bias));

        openCSV.writeCSV(output, "output.csv");
    }

}

// javac -cp ".;./jsoup.jar" HtmlParser.java

// task0
// java -cp ".;./jsoup.jar" HtmlParser 1 0

// task1
// java -cp ".;./jsoup.jar" HtmlParser 1 1 AAL 1 10

// task2
// java -cp ".;./jsoup.jar" HtmlParser 1 2 AAL 1 10

// task3
// java -cp ".;./jsoup.jar" HtmlParser 1 3 AAL 1 10

// task4
// java -cp ".;./jsoup.jar" HtmlParser 1 4 AAL 1 10