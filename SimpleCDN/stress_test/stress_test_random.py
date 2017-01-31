import urllib2
import time
import datetime
import json
import random
from collections import defaultdict

TARGET_PAGE_NUM = 6000
SERVER_LIST = [
"ec2-54-210-1-206.compute-1.amazonaws.com",
"ec2-54-67-25-76.us-west-1.compute.amazonaws.com",
"ec2-35-161-203-105.us-west-2.compute.amazonaws.com",
"ec2-52-213-13-179.eu-west-1.compute.amazonaws.com",
"ec2-52-196-161-198.ap-northeast-1.compute.amazonaws.com",
"ec2-54-255-148-115.ap-southeast-1.compute.amazonaws.com",
"ec2-13-54-30-86.ap-southeast-2.compute.amazonaws.com",
"ec2-52-67-177-90.sa-east-1.compute.amazonaws.com",
"ec2-35-156-54-135.eu-central-1.compute.amazonaws.com",
]
PORT = 40017
TEST_FILEPATH = "./LRU_test2.data"
ERROR_LOG_FILEPATH = "./error_log.data"
total_request_time = 0
links = []

def datetime_to_timestamp(datetime_obj):
    local_timestamp = long(time.mktime(datetime_obj.timetuple()) * 1000.0 + datetime_obj.microsecond / 1000.0)
    return local_timestamp

with open(TEST_FILEPATH, 'r') as f:
	links = [line[:-1] for line in f]


random.seed()

for i in range(TARGET_PAGE_NUM):
	random_page = random.choice(links)
	server = random.choice(SERVER_LIST)
	print random_page
	request_time = datetime_to_timestamp(datetime.datetime.now())
	url = "http://"+server+":"+str(PORT)+random_page
	print "request to: " + url
	code = 0
	try:
		code = urllib2.urlopen(url).getcode()
	except urllib2.HTTPError as error:
		with open(ERROR_LOG_FILEPATH, 'a') as f:
			f.write("server: "+server+"\n")
			f.write("url: "+url+"\n")
			f.write("code: "+str(code)+"\n")
	else:
		respond_time = datetime_to_timestamp(datetime.datetime.now())
		cost_time = respond_time - request_time
		print 'http code: '+str(code), 'request time:'+str(cost_time)