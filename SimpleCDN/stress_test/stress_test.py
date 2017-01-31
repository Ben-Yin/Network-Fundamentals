import urllib2
import time
import datetime
import json
from collections import defaultdict

TARGET_PAGE_NUM = 100
SERVER_NAME = "ec2-54-210-1-206.compute-1"
PORT = 40017
TEST_FILEPATH = "./LRU_test.data"
RESULT_FILEPATH = "./"+SERVER_NAME+"_result.data"
LOG_FILEPATH = "./"+SERVER_NAME+"_log.data"

page_num = 0
total_request_time = 0
log = []
result_map = defaultdict(list)

def datetime_to_timestamp(datetime_obj):
    local_timestamp = long(time.mktime(datetime_obj.timetuple()) * 1000.0 + datetime_obj.microsecond / 1000.0)
    return local_timestamp

with open(TEST_FILEPATH, 'r') as f:
	for line in f:
		if (page_num >= TARGET_PAGE_NUM):
			break
		request_time = datetime_to_timestamp(datetime.datetime.now())
		url = "http://"+SERVER_NAME+".amazonaws.com:"+str(PORT)+line[:-1]
		code = urllib2.urlopen(url).getcode()
		print page_num, line[:-1], 'http code: '+str(code)
		respond_time = datetime_to_timestamp(datetime.datetime.now())
		cost_time = respond_time - request_time
		result_map[line].append({"num":page_num, "time":cost_time})
		log.append({"num":page_num, "url":url, "time":cost_time})
		page_num += 1

with open(RESULT_FILEPATH, 'w') as f:
	json.dump(result_map, f)

with open(LOG_FILEPATH, 'w') as f:
	json.dump(log, f)