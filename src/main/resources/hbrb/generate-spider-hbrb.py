#!/usr/bin/env python
import sys, re

if len(sys.argv)<2:
    print "generate-spider (date1)yyyymmdd (date2)yyyymmdd > hbrb(date1)to(date2).json"
    exit(1)

from_date = sys.argv[1]
to_date   = sys.argv[2]

valid_date = re.compile('^(19|20)[0-9][0-9](0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])$')

if (not valid_date.search(from_date)) or (not valid_date.search(to_date)) or (int(to_date) - int(from_date))<0:
    print "invalid parameter, input as \'command\' yyyymmdd (<) yyyymmdd."
    exit(1)

print """{
  \"start\": ["""

days_in_month = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
yyyy = int(from_date[:4])
while yyyy <= int(to_date[:4]):
    for mm in range(12):
        days = days_in_month[mm]
        mm += 1
        for dd in range(days):
            dd += 1
            date = '%04d%02d%02d' % (yyyy, mm, dd)
            if int(date) >= int(from_date) and int(date) < int(to_date):
                print "    \"http://hbrb.cnhubei.com/html/hbrb/" + date + "/menu.html\","
            elif int(date) == int(to_date):
                print "    \"http://hbrb.cnhubei.com/html/hbrb/" + date + "/menu.html\""
    yyyy += 1

print """  ],
  "concurrency": 3,
  "delay": 1500,
  "timeout": 10000,
  "filters": {
    "^http://hbrb.cnhubei.com/HTML/hbrb/\d{8}/menu.html": "a",
    "^http://hbrb.cnhubei.com/HTML/hbrb/\d{8}/.*.html": "map[name=FPMap0] area"
  },
  "store": "hbrb(%s)to(%s)"
}""" % (from_date, to_date)
