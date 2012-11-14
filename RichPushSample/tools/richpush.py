#!/usr/bin/python

import httplib, urllib
import base64
import json
import sys
from optparse import OptionParser

UA_SERVER = "go.urbanairship.com"
RP_URI = "/api/airmail/send/"

def parse_args():
    parser = OptionParser(usage = "Usage: %prog app-key master-secret file")
    (options, args) = parser.parse_args()
    if len(args) < 3:
        parser.error("missing arguments")
    return (options, args)

def parse_payload_data(filename):
    try:
        file = open(filename)
        payload_data = json.loads(file.read())
	file.close()
    except ValueError:
        sys.exit("Error: Bad JSON")
    except IOError:
        sys.exit("Error: %s: No such file or directory" %filename)
    if 'users' in payload_data and 'title' in payload_data and 'message' in payload_data:
        users = payload_data['users']
	if type(users) == list:
	    for user in users:
	        if type(user) != unicode:
	    	    sys.exit("Error: supplied users must be typed as strings")
	else:
	    sys.exit("Error: users must be typed as a list")
	title = payload_data['title']
	message = payload_data['message']
	if type(title) == unicode and type(message) == unicode:
	    return payload_data
	else:
	    sys.exit("Error: title and message values must be typed as strings")
    else:
        sys.exit("Error: Payload data must contain users, title and message key/value pairs")

def build_payload(payload_data):
    try:
        payload = json.dumps(payload_data)
        return payload
    except TypeError:
        sys.exit("Error: Error serializing rich push payload")

def send_rich_push(app_key, master_secret, payload):
    auth = base64.b64encode(b"%s:%s" %(app_key, master_secret)).decode("ascii")
    headers = {'Content-Type': 'application/json', 'Authorization' : 'Basic %s' %  auth}
    conn = httplib.HTTPSConnection(UA_SERVER)
    conn.request("POST", RP_URI, payload, headers=headers)
    response = conn.getresponse()
    print response.status, response.reason
    resp = response.read()
    conn.close()

def main():
    options, args = parse_args()
    app_key, master_secret, filename = args
    payload = build_payload(parse_payload_data(filename))
    send_rich_push(app_key, master_secret, payload)

if __name__ == '__main__':
    main()
