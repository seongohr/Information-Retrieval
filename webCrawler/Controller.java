import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import com.opencsv.CSVReader;
//import com.opencsv.CSVWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.TreeMap;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;


import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;

public class Controller {
   

    private final static String crawlStorageFolder = "./data/crawl";
   
    private final static int numberOfCrawlers = 7;
    private final static String crawlSeed = "http://www.nytimes.com/";
    private final static int maxPageToFetch = 20000;
    private final static int maxDepthOfCrawling = 16;
    private final static String name = "Seongoh Ryoo";
    private final static String id = "2892065214";
    private final static String newssite = "nytimes.com";
    private final static int delay = 200;
    private static Instant start;
    private static Instant finish;
    
    @SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
    	start = Instant.now();
    	
    	CrawlController crawlController = runCrawler();
        
    	CrawlState sumState = new CrawlState();
        List<Object> crawlersLocalData = crawlController.getCrawlersLocalData();
        for (Object localData : crawlersLocalData) {
            CrawlState state = (CrawlState) localData;
            sumState.attemptUrls.addAll(state.attemptUrls);
            sumState.visitedUrls.addAll(state.visitedUrls);
            sumState.discoveredUrls.addAll(state.discoveredUrls);
        }
        saveFetchCsv(sumState);
        saveVisitCsv(sumState);
        saveUrlsCsv(sumState);
        saveStatistics(sumState);

    }
    
    //the URLs it attempts to fetch
    public static void saveFetchCsv(CrawlState sumState) throws Exception {
        String fileName = crawlStorageFolder + "/fetch_nytimes.csv";
        FileWriter writer = new FileWriter(fileName);
        writer.append("URL,Status\n");
        for (UrlInfo info : sumState.attemptUrls) {
            writer.append(info.url + "," + info.statusCode + "\n");
        }
        writer.flush();
        writer.close();
        System.out.println("Numberof attemptUrls: " + (sumState.attemptUrls).size());
    }


    public static void saveVisitCsv(CrawlState sumState) throws Exception {
        String fileName = crawlStorageFolder + "/visit_nyimes.csv";
        FileWriter writer = new FileWriter(fileName);
        writer.append("URL,Size(Bytes),OutLinks,ContentType\n");
        for (UrlInfo info : sumState.visitedUrls) {

        	if (info.type == "unknown") {
        		writer.append(info.url + "," + info.size + "," + info.outgoingUrls.size() + "," + info.extension + "\n");
        	}
        	else {
        		writer.append(info.url + "," + info.size + "," + info.outgoingUrls.size() + "," + info.type + "\n");
        	}
            
        }
        writer.flush();
        writer.close();
    }
    
    public static void saveUrlsCsv(CrawlState sumState) throws Exception {
        String fileName = crawlStorageFolder + "/urls_nytimes.csv";
        FileWriter writer = new FileWriter(fileName);
        writer.append("URL,Type\n");
        for (UrlInfo info : sumState.discoveredUrls) {
            writer.append(info.url + "," + info.type + "\n");
        }
        writer.flush();
        writer.close();
    }
    
    
    public static void saveStatistics(CrawlState sumState) throws Exception {
        String fileName = crawlStorageFolder + "/CrawlReport_nytimes.txt";
        FileWriter writer = new FileWriter(fileName);

        // Personal Info
        writer.append("Name: " + name + "\n");
        writer.append("News site crawled: " + newssite + "\n");
        writer.append("\n");

        // Fetch Statistics
        writer.append("Fetch Statistics\n================\n");
        writer.append("# fetches attempted: " + sumState.attemptUrls.size() + "\n");
        writer.append("# fetched succeeded: " + sumState.visitedUrls.size() + "\n");

        // get failed url and aborted urls
        int failedUrlsCount = 0;

        for (UrlInfo info : sumState.attemptUrls) {
        	if (info.statusCode >= 300) {
            	failedUrlsCount++;
            }
        }

        writer.append("# fetched failed or aborted: " + failedUrlsCount + "\n");
        writer.append("\n");

        // Outgoing URLS
        HashSet<String> hashSet = new HashSet<String>();
        int uniqueUrls = 0;
        int schoolUrls = 0;
        int uscUrls = 0;
        int outUrls = 0;
        writer.append("Outgoing URLs:\n==============\n");
        writer.append("Total URLs extracted: " + sumState.discoveredUrls.size() + "\n");
        for (UrlInfo info : sumState.discoveredUrls) {
            if (!hashSet.contains(info.url)) {
                hashSet.add(info.url);
                uniqueUrls++;
                if (info.type.equals("OK")) {
                    schoolUrls++;
                } 
                else {
                    outUrls++;
                }
            }
        }
        writer.append("# unique URLs extracted: " + uniqueUrls + "\n");
        writer.append("# unique URLs within News Site: " + schoolUrls + "\n");
        writer.append("# unique URLs outside News Site: " + outUrls + "\n");
        writer.append("\n");

        // Status Code
        writer.append("Status Codes:\n=============\n");
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
        for (UrlInfo info : sumState.attemptUrls) {
            if (hashMap.containsKey(info.statusCode)) {
                hashMap.put(info.statusCode, hashMap.get(info.statusCode) + 1);
            } else {
                hashMap.put(info.statusCode, 1);
            }
        }
        TreeMap<Integer, String> statusCodeMapping = new TreeMap<Integer, String>();
        statusCodeMapping.put(200, "OK");
        statusCodeMapping.put(301, "Moved Permanently");
        statusCodeMapping.put(302, "Found");
        statusCodeMapping.put(401, "Unauthorized");
        statusCodeMapping.put(403, "Forbidden");
        statusCodeMapping.put(404, "Not Found");
        statusCodeMapping.put(405, "Method Not Allowed");
        statusCodeMapping.put(500, "Internal Server Error");

        for (Integer key : statusCodeMapping.keySet()) {
        	if (hashMap.get(key) != null) {
        		writer.append("" + key + " " + statusCodeMapping.get(key) + ": " + hashMap.get(key) + "\n");
        	}
        }
        writer.append("\n");

        // File Size
        writer.append("File Size:\n===========\n");
        int oneK = 0;
        int tenK = 0;
        int hundredK = 0;
        int oneM = 0;
        int other = 0;
        for (UrlInfo info : sumState.visitedUrls) {
        	if (info.type == "unknown") {
        		continue;
        	}
            if (info.size < 1024) {
                oneK++;
            } else if (info.size < 10240) {
                tenK++;
            } else if (info.size < 102400) {
                hundredK++;
            } else if (info.size < 1024 * 1024) {
                oneM++;
            } else {
                other++;
            }
        }
        writer.append("< 1KB: " + oneK + "\n");
        writer.append("1KB ~ <10KB: " + tenK + "\n");
        writer.append("10KB ~ <100KB: " + hundredK + "\n");
        writer.append("100KB ~ <1MB: " + oneM + "\n");
        writer.append(">= 1MB: " + other + "\n");
        writer.append("\n");

        // Content Types
        HashMap<String, Integer> hashMap1 = new HashMap<String, Integer>();
        writer.append("Content Types:\n==============\n");
        for (UrlInfo info : sumState.visitedUrls) {
            if (info.type.equals("unknown")) {
                continue;
            }
            if (hashMap1.containsKey(info.type)) {
                hashMap1.put(info.type, hashMap1.get(info.type) + 1);
            } else {
                hashMap1.put(info.type, 1);
            }
        }
        for (String key : hashMap1.keySet()) {
            writer.append("" + key + ": " + hashMap1.get(key) + "\n");
        }
        writer.append("\n");

        writer.flush();
        writer.close();
        
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        System.out.println("Time elapsed: " + timeElapsed +  "milisec");
        timeElapsed = timeElapsed / 1000 ;
        
        long hours = timeElapsed / 3600;
        long remainder = timeElapsed % 3600;
        
        long mins = remainder / 60;
        remainder = remainder % 60; 
        System.out.println("Time elapsed: " + hours + "hours " + mins + "mins " + remainder + "sec");
    }

    public static CrawlController runCrawler() throws Exception {
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        config.setMaxDepthOfCrawling(maxDepthOfCrawling);
        config.setMaxPagesToFetch(maxPageToFetch);
        config.setIncludeBinaryContentInCrawling(Boolean.TRUE);
        config.setPolitenessDelay(delay);
        

        // initialize
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);

        // add seed
        CrawlController crawlController = new CrawlController(config, pageFetcher, robotstxtServer);
        crawlController.addSeed(crawlSeed);

        // start crawling
        MyCrawler.configure(crawlStorageFolder + "/files");
        crawlController.start(MyCrawler.class, numberOfCrawlers);

        return crawlController;
    }
       
}