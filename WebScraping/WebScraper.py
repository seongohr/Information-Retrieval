from bs4 import BeautifulSoup
# from time import sleep
import time
import requests
from random import randint
from html.parser import HTMLParser
import json
import csv

QUERY_FILE = "100QueriesSet1.txt"
GOOGLE_RESULT_JSON = "Google_Results1.json"
BING_RESULT_JSON = "bing.json"
RESULT_CSV = "result.csv"
USER_AGENT = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36'}
SEARCHING_URL = 'https://www.bing.com/search?q='

class SearchEngine:

    @staticmethod     
    def search(query, sleep=True):
        if sleep: # Prevents loading too many pages too soon
            time.sleep(randint(10, 30))
        temp_url = '+'.join(query.split()) #for adding : between words for the query
        count = '&count=20' # number of results presented on 1 page
        url = SEARCHING_URL + temp_url + count
        soup = BeautifulSoup(requests.get(url, headers=USER_AGENT).text, "html.parser")
        new_results = SearchEngine.scrape_search_result(soup)
        return new_results

    @staticmethod
    def scrape_search_result(soup):
        raw_results = soup.find_all("li", attrs={"class" : "b_algo"})
        results = []
        #implement a check to get only 10 results and also check that URLs must not be duplicated
        seen = set() # seen set for preventing duplication
        for result in raw_results:
            link = result.find("a").get("href")
            # only ten results
            if (len(seen) < 10):
                seen.add(link)
            else:
                break
        results = list(seen)
        return results

def readFile(inputFile, jsonFile):
    with open(inputFile) as f:
        if jsonFile:
            data_loaded = json.load(f)
        else:
            data_loaded = f.read().split("\n")
    f.close()
    return data_loaded

def writeJsonFile(data, file_name):
    with open(file_name, 'w') as f:
        json.dump( data , f, indent=4)
    f.close()

def writeListToCsv(data_list, file_name):
    with open(file_name, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(data_list)
    f.close()

def listToDict(data_list):
    results_dict = {k : v for v, k in enumerate(data_list)}
    
    return results_dict

def extractUrls(data_list):
    for i in range(len(data_list)):
        # remove '/' at the end of the url
        data_list[i] = data_list[i].strip('/')
        # remove 'https://' at the front of the url
        if data_list[i].startswith('https://'):
            data_list[i] = data_list[i][8:]
        # remove 'http://' at the front of the url
        else:
            data_list[i] = data_list[i][7:]
        # remove 'www.' at the front of the url
        if data_list[i].startswith('www.'):
            data_list[i] =  data_list[i][4:]
        data_list[i] = data_list[i].lower()
    return data_list

def computeSpearman(reference, result):
    index = 0
    total_overlap = 0
    total_percent = 0
    total_spearman = 0
    csv_output = [['Queries', 'Number of Overlapping Results', 'Percent Overlap', 'Spearman Coefficient']]
    biggest_overlap_query = 0
    biggest_overlap = float('-inf')

    for key in reference:
        result_values = result[key]
        result_values = extractUrls(result_values)
        result_values = listToDict(result_values)
        reference_values = reference[key]
        reference_values = extractUrls(reference_values)
        num_overlap = 0
        percent_overlap = 0
        spearman_coefficient = 0
        sum_difference_squares = 0

        for i in range(len(reference_values)):
            url = reference_values[i]
            if url in result_values:
                num_overlap += 1
                refer_rank = i
                result_rank = result_values[url]
                difference_square = (refer_rank - result_rank)**2
                sum_difference_squares += difference_square

        if num_overlap == 0: # no overlap
            spearman_coefficient = 0
        elif num_overlap == 1: # one overlap
            if sum_difference_squares == 0: # ranks are matched
                spearman_coefficient = 1
            else:
                spearman_coefficient = 0 # ranks are not matched
        elif num_overlap == 10 and sum_difference_squares == 0: # perfectly matehced including ranks
            spearman_coefficient = 1
        else:
            spearman_coefficient = 1 - ((6 * sum_difference_squares) / (num_overlap * (num_overlap**2 - 1)))
        percent_overlap = (num_overlap / 10)*100
        
        total_overlap += num_overlap
        total_percent += percent_overlap
        total_spearman += spearman_coefficient
        
        index += 1
        # find the highest number of overlap
        if biggest_overlap < num_overlap:
            biggest_overlap = num_overlap
            biggest_overlap_query = index

        csv_output.append(["Query "+str(index), num_overlap, percent_overlap, spearman_coefficient])
    
    csv_output.append(["Averages", total_overlap / 100, total_percent / 100, total_spearman/ 100])

    print("Biggest overlap: ", csv_output[biggest_overlap_query][0], csv_output[biggest_overlap_query][1], csv_output[biggest_overlap_query][2], csv_output[biggest_overlap_query][3])
    return csv_output

def main():
    ## create search results in Bing and write a json file
    query_list = readFile(QUERY_FILE, False)
    json_result = {}

    for query in query_list:
        result = SearchEngine.search(query)
        json_result[query[:-2]] = result

    writeJsonFile(json_result, BING_RESULT_JSON)

    ## compare the results and compute spearman's ranking 
    google_results = readFile(GOOGLE_RESULT_JSON, True)
    bing_results = readFile(BING_RESULT_JSON, True)
    output = computeSpearman(google_results, bing_results)
    writeListToCsv(output, RESULT_CSV)

if __name__ == "__main__":
    main()