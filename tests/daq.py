#! /usr/bin/python

import socket
import xmlrpclib
import string
from time import *


host = 'tini'
port = 3000 


# prepare connection to xml server
server_url= 'http://fynulap6/~vanderaa/php/dbinterface.php'
serverxml= xmlrpclib.Server(server_url)

# connect to server, get reply
proto = socket.getprotobyname('tcp')
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM, proto)
s.connect((host, port))

# Prepare querys
query=[]
for i in range(8):
	c=("RDAN%d\n")%(i)
	query.append(c)
#print query
while (1):
        print ("---------------------------")
        vvaleur=[]
        for i in query:
		s.send(i)
        	data=s.recv(1024)
#	        print ("%s")%(data),
                value=float(string.split(data)[2])
                vvaleur.append(value)
#                print "Valeur",value 
        sleep(2)
        result=serverxml.insertValues(vvaleur)
	print result
s.close()
