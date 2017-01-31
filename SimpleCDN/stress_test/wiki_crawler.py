import urllib2

TARGET_PAGE_NUM = 10
WIKI_RAMDOM_URL = 'http://ec2-54-167-4-20.compute-1.amazonaws.com:8080/wiki/Special:Random'
URL_FILEPATH = "./wiki_url.data"
page_num = 0
while page_num < TARGET_PAGE_NUM:
	html = urllib2.urlopen(WIKI_RAMDOM_URL).geturl()
	print html
	with open(URL_FILEPATH, 'a') as f:
		f.write(html + '\n')
	page_num += 1
