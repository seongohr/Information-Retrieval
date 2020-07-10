import requests
import json
import csv

API_URL = "http://archive.org/wayback/available?"
TARGET_URL = "pillpack.com"
TIME_YEAR = "2016"
TIME_DATE = "01"

results = ""

# make a api call with a date of the first of each month in 2016
for i in range(1, 13): # i : month
    time_month = "{:02d}".format(i)
    timestamp = TIME_YEAR + time_month + TIME_DATE

    # url with date
    url = API_URL + "url=" + TARGET_URL + "&timestamp=" + timestamp
    # make a call and retrieve the data
    response = requests.get(url)
    dictionary = response.json()

    # check the availability of the website
    availability = dictionary["archived_snapshots"]["closest"]["available"]

    # get the archived date and the url which contains archived data and add to the variable results
    if availability:
        get_date = dictionary["archived_snapshots"]["closest"]["timestamp"][:8]
        get_url = dictionary["archived_snapshots"]["closest"]["url"]
        results += timestamp + "," + get_date + "," + get_url + "\n"

# write results to a file
with open("task1.csv", "w", newline="") as csvfile:
    fieldnames = ["date", "archived_date", "url"]
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
    writer.writeheader()
    csvfile.write(results)